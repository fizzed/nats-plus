package com.fizzed.nats.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import io.nats.NatsServerRunner;
import io.nats.client.*;
import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

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
        JetStreamManagement jsm = connection.jetStreamManagement();
        final List<String> streamNames = jsm.getStreamNames();
        for (String streamName : streamNames) {
            jsm.deleteStream(streamName);
        }
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

    public void createWorkQueueStream(Connection connection, String streamName, String subjects) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = connection.jetStreamManagement();
        jsm.addStream(StreamConfiguration.builder()
            .name(streamName)
            .storageType(StorageType.File)
            .subjects(subjects)
            .retentionPolicy(RetentionPolicy.WorkQueue)
            .discardPolicy(DiscardPolicy.Old)
            .build());
    }

}