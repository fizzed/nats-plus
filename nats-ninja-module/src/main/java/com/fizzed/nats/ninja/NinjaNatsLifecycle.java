package com.fizzed.nats.ninja;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fizzed.nats.core.NatsConnectionProvider;
import io.nats.client.Connection;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class NinjaNatsLifecycle {
 
    private final NatsConnectionProvider connectionProvider;
    
    @Inject
    public NinjaNatsLifecycle(NinjaProperties ninjaProperties, NatsConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }
    
    @Start
    public void start() throws IOException {
        // connection a connection the first time will attempt a connection, which should verify we can connect
        final Connection connection = this.connectionProvider.get();
    }

    @Dispose
    public void stop() throws IOException, InterruptedException {
        // connection a connection the first time will attempt a connection, which should verify we can connect
        final Connection connection = this.connectionProvider.get();
        connection.close();
    }
    
}