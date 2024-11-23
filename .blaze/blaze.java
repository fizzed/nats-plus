import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import com.fizzed.jne.HardwareArchitecture;
import com.fizzed.jne.NativeTarget;
import com.fizzed.jne.OperatingSystem;
import com.fizzed.jne.PlatformInfo;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.hc.client5.http.fluent.Request;

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
//        new NatsServerPlatform("linux", "amd64", "linux", "x64"),
//        new NatsServerPlatform("darwin", "amd64", "macos", "x64"),
        new NatsServerPlatform("windows", "amd64", "windows", "x64")
    );



    public void downloadNatsServers() throws Exception {
        final Path projectDir = Contexts.withBaseDir("..").toAbsolutePath().normalize();

        // detect current os & arch, then translate to values that nats-server project uses
        final NativeTarget nativeTarget = NativeTarget.detect();
        final String natsOs;
        switch (nativeTarget.getOperatingSystem()) {
            case MACOS:
                natsOs = "darwin";
                break;
            default:
                natsOs = nativeTarget.getOperatingSystem().name().toLowerCase();
                break;
        }

        final String natsArch;
        switch (nativeTarget.getHardwareArchitecture()) {
            case X64:
                natsArch = "amd64";
                break;
            case X32:
                natsArch = "386";
                break;
            default:
                natsArch = nativeTarget.getHardwareArchitecture().name().toLowerCase();
                break;
        }


        // make a scratch directory
        final Path scratchDir = projectDir.resolve("temp-download-dir");
        rm(scratchDir).recursive().force().run();
        Files.createDirectories(scratchDir);
        log.info("Created scratch directory: " + scratchDir);
        try {
            // download the nat-server release we will be testing with
            final String url = "https://github.com/nats-io/nats-server/releases/download/v"
                + this.natsVersion + "/nats-server-v" + this.natsVersion + "-" + natsOs + "-" + natsArch + ".zip";

            final Path downloadFile = scratchDir.resolve("nats-server.zip");
            this.downloadFile(url, downloadFile);

            this.unzip(downloadFile, scratchDir, true);

            Path natsExe = scratchDir.resolve("nats-server");
            if (!Files.exists(natsExe)) {
                natsExe = scratchDir.resolve("nats-server.exe");
            }

            // target directory is as a resource
            final Path resourceDir = projectDir.resolve(".resources");
            Files.createDirectories(resourceDir);
            log.info("Created resource directory: " + resourceDir);

            final Path resourceExe = resourceDir.resolve(natsExe.getFileName());

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
            // always cleanup the scratch directory
            rm(scratchDir).recursive().force().run();
            log.info("Deleted scratch directory: " + scratchDir);
        }
    }

    public void nuke() {

    }

    // helpers

    private void downloadFile(String url, Path file) throws Exception {
        log.info("Downloading {} -> {}", url, file);
        Request.get(url)
            .execute()
            .saveContent(file.toFile());
    }

    private void unzip(Path zipFile, Path destDir, boolean stripLeadingDir) throws IOException {
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
    }

}