package conf;

import com.fizzed.nats.core.NatsConnectionProvider;
import com.fizzed.nats.core.NatsHelper;
import io.nats.client.Connection;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DemoNatsMigration {

    private final NatsConnectionProvider connectionProvider;

    @Inject
    public DemoNatsMigration(NinjaProperties ninjaProperties, NatsConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }
    
    @Start
    public void start() throws Exception {
        // connection a connection the first time will attempt a connection, which should verify we can connect
        final Connection connection = this.connectionProvider.get();

        // setup schemas that we will need
        //NatsHelper.deleteAllStreams(connection);
        NatsHelper.createWorkQueueStream(connection, "demo-requests-stream", "demo.requests.queue");
    }
    
}