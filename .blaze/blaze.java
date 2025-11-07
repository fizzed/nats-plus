import com.fizzed.blaze.maven.MavenClasspath;
import com.fizzed.blaze.maven.MavenProject;
import com.fizzed.blaze.maven.MavenProjects;
import com.fizzed.blaze.project.PublicBlaze;
import com.fizzed.buildx.Buildx;
import com.fizzed.buildx.Target;
import com.fizzed.jne.HardwareArchitecture;
import com.fizzed.jne.NativeLanguageModel;
import com.fizzed.jne.NativeTarget;
import com.fizzed.jne.OperatingSystem;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.fizzed.blaze.Archives.unarchive;
import static com.fizzed.blaze.Https.*;
import static com.fizzed.blaze.Systems.*;
import static com.fizzed.blaze.maven.MavenProjects.*;

public class blaze extends PublicBlaze {
    private final Path resourcesDir = projectDir.resolve(".resources");
    private final String natsVersion = config.value("nats.version").orElse("2.10.22");

    @Override
    protected void projectSetup() throws Exception {
        super.projectSetup();
        this.downloadNatsServer();
    }

    @Override
    public void projectNuke() throws Exception {
        super.projectNuke();
        rm(resourcesDir).recursive().force().verbose().run();
    }

    protected void downloadNatsServer() throws Exception {
        // detect current os & arch, then translate to values that nats-server project uses
        final NativeTarget nativeTarget = NativeTarget.detect();
        final NativeLanguageModel nlm = new NativeLanguageModel()
            .add("version", this.natsVersion)
            .add(OperatingSystem.MACOS, "darwin")
            .add(HardwareArchitecture.ARMHF, "arm7")
            .add(HardwareArchitecture.X64, "amd64")
            .add(HardwareArchitecture.X32, "386");

        // make a scratch directory
        final Path tempDownloadDir = projectDir.resolve("temp-download-dir");
        rm(tempDownloadDir).recursive().force().verbose().run();
        mkdir(tempDownloadDir).parents().verbose().run();

        try {
            // download the nat-server release we will be testing with
            final String url = nlm.format("https://github.com/nats-io/nats-server/releases/download/v{version}/nats-server-v{version}-{os}-{arch}.zip", nativeTarget);
            final Path downloadFile = tempDownloadDir.resolve("nats-server.zip");

            httpGet(url)
                .verbose()
                .progress()
                .target(downloadFile, true)
                .run();

            unarchive(downloadFile)
                .target(tempDownloadDir)
                .stripLeadingPath()
                .verbose()
                .run();

            final Path natsExe = tempDownloadDir.resolve(nativeTarget.resolveExecutableFileName("nats-server"));

            mkdir(resourcesDir).parents().verbose().run();

            final Path resourceExe = resourcesDir.resolve(natsExe.getFileName());

            natsExe.toFile().setExecutable(true);
            cp(natsExe).target(resourceExe).force().verbose().run();

            // verify the nats-server works
            log.info("Verifying nats-server works... will run 'nat-server -v' to test");
            System.out.println();
            exec(resourceExe, "-v").run();
            System.out.println();
            log.info("Success, all done.");
        } finally {
            // always cleanup
            rm(tempDownloadDir).recursive().force().run();
            log.info("Deleted temp download directory: " + tempDownloadDir);
        }
    }

    final MavenProject mavenProject = MavenProjects.mavenProject().run();

    public void demo_ninja() throws Exception {
        // (re)compiles code and we get the classpath in one step
        final MavenClasspath classpath = mavenClasspath(this.mavenProject, "runtime", "compile", "nats-ninja-demo")
            .verbose()
            .run();

        exec("java", "-cp", classpath, "ninja.standalone.NinjaJetty")
            .verbose()
            .run();
    }

    public void release() throws Exception {
        this.mvnCommandsWithJdk(this.minimumSupportedJavaVersion(),
            "clean", "-DskipTests", "-Darguments=-DskipTests", "release:prepare", "release:perform");
    }

    @Override
    protected int[] supportedJavaVersions() {
        return new int[] {21, 17, 11};
    }

    @Override
    protected List<Target> crossHostTestTargets() {
        return super.crossHostTestTargets().stream()
            .filter(v -> !(v.getName().startsWith("freebsd") && v.getName().contains("arm64")))
            .filter(v -> !(v.getName().startsWith("openbsd")))
            .collect(Collectors.toList());
    }

    @Override
    protected void mvnCrossHostTests(List<Target> crossTestTargets) throws Exception {
        final boolean serial = this.config.flag("serial").orElse(false);
        new Buildx(crossTestTargets)
            .parallel(!serial)
            .execute((target, project) -> {
                // we need the nats-server first
                project.exec("java", "-jar", "blaze.jar", "setup")
                    .run();
                project.exec("mvn", "clean", "test")
                    .run();
            });
    }

}
