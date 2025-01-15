import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import com.fizzed.jne.HardwareArchitecture;
import com.fizzed.jne.NativeLanguageModel;
import com.fizzed.jne.NativeTarget;
import com.fizzed.jne.OperatingSystem;
import org.slf4j.Logger;
import java.nio.file.Path;

import static com.fizzed.blaze.Archives.unarchive;
import static com.fizzed.blaze.Https.*;
import static com.fizzed.blaze.Systems.*;

public class blaze {
    private final Logger log = Contexts.logger();
    private final Config config = Contexts.config();
    private final Path projectDir = Contexts.withBaseDir("..").toAbsolutePath().normalize();
    private final Path resourcesDir = projectDir.resolve(".resources");
    private final String natsVersion = config.value("nats.version").orElse("2.10.22");

    public void setup() throws Exception {
        this.downloadNatsServer();
    }

    public void nuke() {
        rm(resourcesDir).recursive().force().verbose().run();
    }

    public void downloadNatsServer() throws Exception {
        // detect current os & arch, then translate to values that nats-server project uses
        final NativeTarget nativeTarget = NativeTarget.detect();
        final NativeLanguageModel nlm = new NativeLanguageModel()
            .add("version", this.natsVersion)
            .add(OperatingSystem.MACOS, "darwin")
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

            httpGet(url).target(downloadFile).run();

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

}