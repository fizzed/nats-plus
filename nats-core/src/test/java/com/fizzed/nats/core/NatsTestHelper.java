package com.fizzed.nats.core;

import com.fizzed.jne.JNE;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
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
                File f = JNE.findExecutable("nats-server");
                if (f != null) {
                    natsExe = f.toPath();
                } else {
                    throw new RuntimeException("Could not find nats exe");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return natsExe;
    }

}