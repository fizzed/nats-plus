import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import com.fizzed.jne.HardwareArchitecture;
import com.fizzed.jne.NativeLanguageModel;
import com.fizzed.jne.NativeTarget;
import com.fizzed.jne.OperatingSystem;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.hc.client5.http.fluent.Request;

import static com.fizzed.blaze.Archives.unarchive;
import static com.fizzed.blaze.Systems.exec;
import static com.fizzed.blaze.Systems.rm;

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
        rm(tempDownloadDir).recursive().force().run();
        Files.createDirectories(tempDownloadDir);
        log.info("Created temp download directory: " + tempDownloadDir);
        try {
            // download the nat-server release we will be testing with
            final String url = nlm.format("https://github.com/nats-io/nats-server/releases/download/v{version}/nats-server-v{version}-{os}-{arch}.zip", nativeTarget);
            final Path downloadFile = tempDownloadDir.resolve("nats-server.zip");
            this.downloadFile(url, downloadFile);

            unarchive(downloadFile)
                .target(tempDownloadDir)
                .stripLeadingPath()
                .verbose()
                .run();

            final Path natsExe = tempDownloadDir.resolve(nativeTarget.resolveExecutableFileName("nats-server"));

            // target directory is as a resource
            Files.createDirectories(resourcesDir);
            log.info("Created resource directory: " + resourcesDir);

            final Path resourceExe = resourcesDir.resolve(natsExe.getFileName());

            log.info("Copying {} -> {}", natsExe, resourceExe);
            natsExe.toFile().setExecutable(true);
            Files.copy(natsExe, resourceExe, StandardCopyOption.REPLACE_EXISTING);

            // verify the nats-server works
            log.info("");
            log.info("Verifying nats-server works... will run 'nat-server -v' to test");
            log.info("");
            exec(resourceExe, "-v").run();
            log.info("");
            log.info("Success, all done.");
        } finally {
            // always cleanup
            rm(tempDownloadDir).recursive().force().run();
            log.info("Deleted temp download directory: " + tempDownloadDir);
        }
    }

    // helpers

    private void downloadFile(String url, Path file) throws Exception {
        log.info("Downloading {} -> {}", url, file);
        Request.get(url)
            .execute()
            .saveContent(file.toFile());
    }

    /*private void unzip(Path zipFile, Path destDir, boolean stripLeadingDir) throws IOException {
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        try (InputStream fin = Files.newInputStream(zipFile)) {
            try (ZipInputStream zipIn = new ZipInputStream(fin)) {
                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {
                    String entryName = entry.getName();
                    if (stripLeadingDir && entryName.contains("/")) {
                        entryName = entryName.substring(entryName.indexOf('/')+1);
                    }

                    final Path outputPath = destDir.resolve(entryName);

                    if (!entry.isDirectory()) {
                        extractFile(zipIn, outputPath);
                        log.info("Extracted {}", outputPath);
                    } else {
                        Files.createDirectories(outputPath);
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, Path outputPath) throws IOException {
        try (OutputStream fos = Files.newOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zipIn.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }*/

}