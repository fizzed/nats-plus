package com.fizzed.nats.core;

import io.nats.NatsServerRunner;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.fizzed.nats.core.NatsTestHelper.getOrFindNatsExe;
import static org.junit.jupiter.api.Assertions.*;

class NatsReliableStreamPullConsumerTest extends NatsBaseTest{
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullConsumerTest.class);

    @Test
    void nextMessageInterrupted() throws Exception {
        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            final Options co = new Options.Builder()
                .server(nats.getURI())
                .build();

            try (Connection connection = Nats.connect(co)) {

                Thread.sleep(2000L);

                nats.shutdown(true);

                Thread.sleep(2000L);
            }
        }

        /*final Options connectOptions = new Options.Builder()
            .server("nats://localhost:14222")
            .maxReconnects(-1)
            .build();

        try (Connection connection = Nats.connect(connectOptions)) {

        }*/
    }

}