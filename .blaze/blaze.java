import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.fizzed.blaze.Systems.exec;
import static com.fizzed.blaze.Systems.rm;
import static java.util.Arrays.asList;

public class blaze {
    private final Logger log = Contexts.logger();
    private final Config config = Contexts.config();

    private final String natsVersion = config.value("nats.version").orElse("2.10.22");

    static public class NatsServerPlatform {
        final String os;
        final String arch;
        final String jneOs;
        final String jneArch;

        public NatsServerPlatform(String os, String arch, String jneOs, String jneArch) {
            this.os = os;
            this.arch = arch;
            this.jneOs = jneOs;
            this.jneArch = jneArch;
        }
    }

    private final List<NatsServerPlatform> natsServerPlatforms = asList(
        new NatsServerPlatform("linux", "amd64", "linux", "x64"),
        new NatsServerPlatform("darwin", "amd64", "macos", "x64"),
        new NatsServerPlatform("windows", "amd64", "windows", "x64")
    );

    public void downloadNatsServers() throws Exception {
        final Path projectDir = Contexts.withBaseDir("..").toAbsolutePath().normalize();

        // make a scratch directory
        final Path scratchDir = projectDir.resolve("temp-download-dir");

        for (NatsServerPlatform nsp : natsServerPlatforms) {
            rm(scratchDir).recursive().force().run();
            Files.createDirectories(scratchDir);
            log.info("Created scratch directory: " + scratchDir);

            // download the nat-server release we will be testing with
            final String url = "https://github.com/nats-io/nats-server/releases/download/v"
                + this.natsVersion + "/nats-server-v" + this.natsVersion + "-" + nsp.os + "-" + nsp.arch + ".tar.gz";

            exec("wget", "-O", "nats-server.tar.gz", url)
                .workingDir(scratchDir)
                .run();

            exec("tar", "zxvf", "nats-server.tar.gz", "--strip-components=1")
                .workingDir(scratchDir)
                .run();

            Path natsExe = scratchDir.resolve("nats-server");
            if (!Files.exists(natsExe)) {
                natsExe = scratchDir.resolve("nats-server.exe");
            }

            // target directory is as a resource
            final Path resourceDir = projectDir.resolve("nats-core/src/test/resources/jne/" + nsp.jneOs + "/" + nsp.jneArch);
            Files.createDirectories(resourceDir);
            log.info("Created resource directory: " + resourceDir);

            final Path resourceExe = resourceDir.resolve(natsExe.getFileName());
            log.info("Copying {} -> {}", natsExe, resourceExe);
            Files.copy(natsExe, resourceExe, StandardCopyOption.REPLACE_EXISTING);
        }

        rm(scratchDir).recursive().force().run();
    }

    public void nuke() {

    }

}