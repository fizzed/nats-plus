package com.fizzed.nats.core;

import com.fizzed.crux.util.Resources;
import com.fizzed.jne.NativeTarget;
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
                final Path markerFile = Resources.file("/locator.txt");
                final Path resourcesDir = markerFile.resolve("../../../../.resources").normalize().toAbsolutePath();
                final NativeTarget nativeTarget = NativeTarget.detect();
                final String exeName = nativeTarget.resolveExecutableFileName("nats-server");
                final Path maybeExe = resourcesDir.resolve(exeName);

                if (!Files.exists(maybeExe)) {
                    throw new RuntimeException("Unable to locate .resources/" + exeName + " (did you run 'blaze setup' ??)");
                }

                natsExe = maybeExe;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return natsExe;
    }

}