package com.fizzed.nats.core.demo;

import com.fizzed.nats.core.NatsReliableMessage;
import com.fizzed.nats.core.NatsReliableStreamPublisher;
import com.fizzed.nats.core.NatsReliableStreamPullSubscriber;
import com.fizzed.nats.core.NatsUnrecoverableException;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.NatsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;

public class NatsReliableStreamPublisherDemo {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPublisherDemo.class);

    static public void main(String[] args) throws Exception {
        try (Connection connection = Nats.connect(Options.builder()
                .server("nats://localhost:14222")
                .connectionName(NatsReliableStreamPublisherDemo.class.getCanonicalName())
                .build())) {

            log.info("Connected to nats server: {}", connection.getConnectedUrl());

            final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                .start();

            publisher.publish(NatsMessage.builder()
                .subject("request.queue.priority")
                .data("Hello World " + Instant.now())
                .build());

            log.info("Done.");
        }
    }

}