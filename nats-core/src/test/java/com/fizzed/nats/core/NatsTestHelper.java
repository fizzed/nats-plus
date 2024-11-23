package com.fizzed.nats.core;

import com.fizzed.crux.util.Resources;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class NatsTestHelper {

    static protected Path natsExe;

    static {
        // Optionally remove existing handlers attached to the root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        // Install SLF4JBridgeHandler as the only handler for the root logger
        SLF4JBridgeHandler.install();
    }

    static public Path getOrFindNatsExe() {
        if (natsExe == null) {
            try {
                Path markerFile = Resources.file("/locator.txt");
                Path resourcesDir = markerFile.resolve("../../../../.resources").normalize().toAbsolutePath();
                Path maybeExe = resourcesDir.resolve("nats-server");
                if (!Files.exists(maybeExe)) {
                    maybeExe = resourcesDir.resolve("nats-server.exe");
                }
                if (!Files.exists(maybeExe)) {
                    throw new RuntimeException("Unable to locate .resources/nats-server (did you run 'blaze setup' ??)");
                }

                natsExe = maybeExe;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return natsExe;
    }

}