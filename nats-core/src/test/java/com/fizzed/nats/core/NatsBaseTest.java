package com.fizzed.nats.core;

import java.io.IOException;
import java.nio.file.Path;

import io.nats.NatsServerRunner;

class NatsBaseTest {

    public NatsServerRunner buildNatsServerRunner() throws IOException {
        final Path natsExe = NatsTestHelper.getOrFindNatsExe();

        final NatsServerRunner.Builder builder = NatsServerRunner.builder()
            .port(24222)
            //.debugLevel(DebugLevel.DEBUG_TRACE)
            .jetstream(true)
            .executablePath(natsExe)
//            .configFilePath("/mypath/custom.conf")
//            .configInserts(customInserts)
            ;

        return builder.build();
    }

}