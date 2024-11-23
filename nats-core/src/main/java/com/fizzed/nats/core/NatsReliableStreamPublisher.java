package com.fizzed.nats.core;

import io.nats.client.*;
import io.nats.client.api.PublishAck;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class NatsReliableStreamPublisher {

    private final Connection connection;
    private JetStream js;

    public NatsReliableStreamPublisher(Connection connection) {
        this.connection = connection;
    }

    public NatsReliableStreamPublisher start() throws IOException {
        this.js = this.connection.jetStream();
        return this;
    }

    public PublishAck publish(Message message) throws InterruptedException, JetStreamApiException, IOException {
        if (this.js == null) {
            throw new IllegalStateException("Not started");
        }

        return this.js.publish(message);
    }

}