package com.fizzed.nats.core.demo;

import com.fizzed.nats.core.NatsReliableMessage;
import com.fizzed.nats.core.NatsReliableStreamPullSubscriber;
import com.fizzed.nats.core.NatsUnrecoverableException;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;

public class NatsReliableStreamPullSubscriberDemo {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullSubscriberDemo.class);

    static public void main(String[] args) throws Exception {
        try (Connection connection = Nats.connect(Options.builder()
                .server("nats://localhost:14222")
                .connectionName(NatsReliableStreamPullSubscriberDemo.class.getCanonicalName())
                .build())) {

            log.info("Connected to nats server: {}", connection.getConnectedUrl());

            final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                .setSubject("request.queue.priority")
                .setDurable("request-queue-consumer")
                .start();

            while (true) {
                try {
                    log.debug("Waiting for next message...");

                    final Message message = subscriber.nextMessage(Duration.ofSeconds(120));

                    if (message == null) {
                        log.warn("No message received");
                        continue;   // keep searching for messages
                    }

                    log.debug("Received message:\n{}", dumpMessage(message));

                    NatsReliableMessage.ack(message);
                } catch (NatsUnrecoverableException e) {
                    log.error("Unrecoverable exception (will backoff for 5 seconds)", e);
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    log.error("Interrupted exception (will exit)", e);
                    break;
                }
            }

            log.info("Done.");
        }
    }

}