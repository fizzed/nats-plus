package com.fizzed.nats.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import io.nats.NatsServerRunner;
import io.nats.client.*;

class NatsBaseTest {

    public NatsServerRunner buildNatsServerRunner() throws IOException {
        final Path natsExe = NatsTestHelper.getOrFindNatsExe();

        final NatsServerRunner.Builder builder = NatsServerRunner.builder()
            //.debug(true)
            .fullErrorReportOnStartup(true)
            .port(24222)
            //.debugLevel(DebugLevel.DEBUG_TRACE)
            .jetstream(true)
            .executablePath(natsExe)
            .processCheckTries(10)
            .processCheckWait(500L)
//            .configFilePath("/mypath/custom.conf")
//            .configInserts(customInserts)
            ;

        return builder.build();
    }

    public Connection connectNats(NatsServerRunner nats, boolean clean) throws IOException, InterruptedException, JetStreamApiException {
        final Options options = new Options.Builder()
            .server(nats.getURI())
            .connectionName("unit-test")
            .build();

        final Connection connection = Nats.connect(options);

        if (clean) {
            this.cleanNats(connection);
        }

        return connection;
    }

    public void cleanNats(Connection connection) throws IOException, JetStreamApiException {
        NatsHelper.deleteAllStreams(connection);
    }

    public String randomStreamName() {
        return "stream-" + UUID.randomUUID().toString().replace("-", "");
    }

    public String randomSubjectName() {
        return "subject." + UUID.randomUUID().toString().replace("-", "");
    }

    public String randomDurableName() {
        return "durable-" + UUID.randomUUID().toString().replace("-", "");
    }

}