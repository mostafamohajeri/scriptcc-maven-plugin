import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import translation.Translator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.empty;

@Mojo(name = "scriptcc", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true, threadSafe = true)

public class ScriptCCMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    private MavenProject project;
    @Parameter(defaultValue = "${basedir}/src/main/asl")
    private File sourceDirectory;
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/asl")
    private File outputDirectory;

    public static void main(String[] args) {
        ScriptCCMojo s = new ScriptCCMojo();
        var loc = s.compile(new File("/home/msotafa/IdeaProjects/ASC-test-java/src/main/asl"),new File("/home/msotafa/IdeaProjects/maven-scriptcc/target/generated-sources"));
        System.out.println(loc);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Translating ASL code");
        var loc = compile(sourceDirectory,outputDirectory);

        project.addCompileSourceRoot(outputDirectory.getPath());
        project.addCompileSourceRoot(loc);
    }

    private Stream<File> getListOfFiles(File dir) {
        if(dir.exists() && dir.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(dir.listFiles())).filter(File::isFile).filter(f -> f.getName().contains(".asl"));
        }
        getLog().debug("no dir found");
        return Stream.<File>empty();
    }

    private String compile(File dir, File out) {
        getLog().debug("Translating sources from dir: "+ dir);
        var translator = new Translator();
        var files = getListOfFiles(dir);
        files.forEach(f-> {

            var name = f.getName().split("\\.")[0];
            var pkg = dir.getName();
            var output = new File(out.getAbsolutePath() + FileSystems.getDefault().getSeparator() + FileSystems.getDefault().getSeparator() + name + ".scala");
            var code = translator.translate(f.getAbsolutePath(),name);
            getLog().debug("translating " + name);
            if(Objects.nonNull(output.getParentFile())) {
                output.getParentFile().mkdirs();
                getLog().debug("creating "+ output.getParentFile());
            }
            try {
                var result = output.createNewFile();
                getLog().debug("created "+ output + " " + result);
                getLog().debug("size of code for "+ output + " " + code.length() );
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(
                        "package " + pkg + "\n" +
                                code + "\n" +

                        "object " + name +"_companion { \n" +
                                "   def create() = new "+ name +"().agentBuilder \n" +
                                "   def create(in_coms : AgentCommunicationLayer) = new "+ name +"(coms = in_coms).agentBuilder \n" +
                                "   def create(in_beliefBaseFactory: IBeliefBaseFactory) = new "+ name +"(beliefBaseFactory = in_beliefBaseFactory).agentBuilder \n" +
                                "} \n"
                );
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        return new File(out.getAbsolutePath() + FileSystems.getDefault().getSeparator() + dir.getName()).getPath();
    }

}