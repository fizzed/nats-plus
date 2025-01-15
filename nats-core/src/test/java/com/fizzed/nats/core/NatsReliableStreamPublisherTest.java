package com.fizzed.nats.core;

import io.nats.NatsServerRunner;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.fizzed.nats.core.NatsHelper.createWorkQueueStream;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class NatsReliableStreamPublisherTest extends NatsBaseTest {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPublisherTest.class);

    @Test
    void startStop() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection);

                try {
                    publisher.publish(null);
                    fail("Expected publish() to have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                } catch (Exception e) {
                    // unexpected
                    log.error("Unexpected exception", e);
                    fail("Unexpected exception thrown during subscribe: " + e.getMessage());
                }

                // start publisher, which should allow things to work now
                publisher.start();

                // publish() should succeed now
                final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 1")
                    .build());

                assertThat(ack1, is(not(nullValue())));
            }
        }
    }

    @Test
    void startButConnectionFailed() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection);

                nats.shutdown(true);

                publisher.start();
            }
        }
    }

    @Test
    void publishButConnectionFailed() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                nats.shutdown(true);

                try {
                    final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());
                    fail("Expected publish() to have failed");
                } catch (NatsRecoverableException e) {
                    // expected
                }

                // start the server again, publish should work
                try (NatsServerRunner nats2 = this.buildNatsServerRunner()) {
                    // make test more reliable on windows
                    Thread.sleep(1000L);

                    final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());

                    assertThat(ack1, is(not(nullValue())));
                }
            }
        }
    }

    @Test
    void publishWithThreadInterrupt() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                Thread.currentThread().interrupt();

                try {
                    final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());
                    fail("Expected publish() to have failed");
                } catch (InterruptedException e) {
                    // expected
                }

            }
        }
    }

    @Test
    void publishWithConnectionClosed() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                connection.close();

                try {
                    final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());
                    fail("Expected publish() to have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                }
            }
        }
    }

}