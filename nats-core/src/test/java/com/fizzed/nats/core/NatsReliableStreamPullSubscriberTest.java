package com.fizzed.nats.core;

import com.fizzed.crux.util.WaitFor;
import io.nats.NatsServerRunner;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class NatsReliableStreamPullSubscriberTest extends NatsBaseTest{
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullSubscriberTest.class);

    @Test
    void publishAndSubscribe() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                this.createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // how we'll wait for replies
                final BlockingDeque<Message> receivedMessages = new LinkedBlockingDeque<>();
                final CountDownLatch subscriberStoppedLatch = new CountDownLatch(1);

                final Thread subscriberThread = new Thread(() -> {
                    try {
                        while (true) {
                            final Message message = subscriber.nextMessage(Duration.ofSeconds(30));

                            receivedMessages.push(message);
                            message.ack();
                        }
                    } catch (InterruptedException e) {
                        log.debug("Expected interruption");
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        fail("Exception thrown during subscribe!");
                    }

                    subscriberStoppedLatch.countDown();
                });
                subscriberThread.start();


                final PublishAck ack1 = publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 1")
                    .build());

                assertThat(ack1.getStream(), is(streamName));
                assertThat(ack1.getSeqno(), is(1L));    // first message in a stream should be 1

                Message message1 = receivedMessages.poll(5, TimeUnit.SECONDS);

                assertThat(message1.getSubject(), is(subjectName));
                assertThat(new String(message1.getData()), is("Hello 1"));


                final PublishAck ack2 = publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 2")
                    .build());

                Message message2 = receivedMessages.poll(5, TimeUnit.SECONDS);

                assertThat(message2, is(not(nullValue())));
                assertThat(message2.getSubject(), is(subjectName));
                assertThat(new String(message2.getData()), is("Hello 2"));

                // interrupt subscriber, wait for it to exit as gracefully as we can (we test for this logic in other
                // unit tests so we are just trying to avoid weird log messages when server shuts down as part of cleanup
                subscriberThread.interrupt();

                if (!subscriberStoppedLatch.await(3, TimeUnit.SECONDS)) {
                    fail("Subscriber thread did not successfully stop");
                }
            }
        }
    }

    @Test
    void nextMessageThreadInterruptedBeforeEnteringMethod() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                this.createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                try {
                    // NOTE: underlying nats.java library appears to honor the interrupt and immediately returns from
                    // its underlying fetch(), which then allows us to check and throw an interrupted exception
                    Thread.currentThread().interrupt();

                    Message message = subscriber.nextMessage(Duration.ofSeconds(5));

                    fail("We expected an InterruptedException");
                } catch (InterruptedException e) {
                    // expected
                    //log.debug("Expected interruption", e);
                } finally {
                    // always cleanup interrupt status of this executing thread (to not ruin other unit tests)
                    Thread.interrupted();
                }

                // we also want to make sure publisher/subscriber work after the interrupt
                publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 1")
                    .build());

                final Message message = subscriber.nextMessage(Duration.ofSeconds(5));

                assertThat(message.getSubject(), is(subjectName));
                assertThat(new String(message.getData()), is("Hello 1"));
            }
        }
    }

    @Test
    void nextMessageThreadInterruptedDuringFetch() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                this.createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // start thread to consume
                CountDownLatch nextMessageBeforeLatch = new CountDownLatch(1);
                CountDownLatch nextMessageInterruptedLatch = new CountDownLatch(1);

                final Thread subscriberThread = new Thread(() -> {
                    try {
                        // NOTE: this will help our unit test get as close as possible to waiting for us to enter nextMessage()
                        nextMessageBeforeLatch.countDown();

                        final Message message = subscriber.nextMessage(Duration.ofSeconds(10));

                        log.debug("nextMessage() exited");

                        //nextMessageAfterLatch.countDown();
                    } catch (InterruptedException e) {
                        // expected
                        nextMessageInterruptedLatch.countDown();
                        //log.debug("Expected interruption", e);
                    } catch (Exception e) {
                        fail("Consume exception thrown " + e.getMessage());
                    }
                });
                subscriberThread.start();

                // await for message consuming to be happening
                nextMessageBeforeLatch.await();

                // to give our subscriberThread as much time to make sure its in the nats.java fetch() we will pause
                // for a brief period to help guarantee as much as we can that it'll be waiting
                Thread.sleep(1000L);

                // interrupt the subscriber thread, the fetch() better darn exit!
                subscriberThread.interrupt();

                if (!nextMessageInterruptedLatch.await(5, TimeUnit.SECONDS)) {
                    fail("nextMessage() did not honor the interrupt");
                }
            }
        }
    }

    @Test
    void nextMessageServerShutdownDuringFetch() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                this.createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // start thread to consume
                final CountDownLatch nextMessageBeforeLatch1 = new CountDownLatch(1);
                final CountDownLatch nextMessageUnrecoverableLatch1 = new CountDownLatch(1);

                final Thread subscriberThread1 = new Thread(() -> {
                    try {
                        // NOTE: this will help our unit test get as close as possible to waiting for us to enter nextMessage()
                        nextMessageBeforeLatch1.countDown();

                        final Message message = subscriber.nextMessage(Duration.ofSeconds(20));

                        if (message != null) {
                            log.debug("Received message: {}", dumpMessage(message));
                        }

                        fail("nextMessage() expected to throw exception, not exit");
                    } catch (NatsUnrecoverableException e) {
                        // expected
                        log.debug("nextMesssage threw an expection exception", e);
                        nextMessageUnrecoverableLatch1.countDown();
                    } catch (Exception e) {
                        log.error("Unexpected exception", e);
                        fail("Unexpected exception " + e.getMessage());
                    }
                });
                subscriberThread1.start();

                // await for message consuming to be happening
                nextMessageBeforeLatch1.await();

                // to give our subscriberThread as much time to make sure its in the nats.java fetch() we will pause
                // for a brief period to help guarantee as much as we can that it'll be waiting
                Thread.sleep(1000L);

                // shutdown the server while the subscriber is fetching, normally nats.java by itself wouldn't quickly
                // exit the fetch() and it would be stuck there for the full timeout, but ours should exit quickly
                nats.shutdown(true);

                // the subscriber should now receive an unrecoverable exception
                if (!nextMessageUnrecoverableLatch1.await(5, TimeUnit.SECONDS)) {
                    fail("nextMessage() did not throw an unrecoverable exception");
                }

                // since the connection is still down, we should quickly get an unrecoverable exception
                try {
                    Message message1 = subscriber.nextMessage(Duration.ofSeconds(10));
                    fail("nextMessage() should have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                    log.debug("nextMesssage threw an expection exception", e);
                }

                // now, we'll start the server back up, which should allow the
                try (NatsServerRunner nats2 = this.buildNatsServerRunner()) {
                    // we need to allow the connection to recover, which should fix the subscriber too
                    WaitFor.of(subscriber::isHealthy)
                        .requireMillis(5000L, 100L);

                    // this should now work, but return nothing yet
                    Message message1 = subscriber.nextMessage(Duration.ofSeconds(1));

                    assertThat(message1, is(nullValue()));

                    // let's publish a message and then get it to be extra safe the subscription works
                    publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());

                    Message message2 = subscriber.nextMessage(Duration.ofSeconds(5));

                    assertThat(message2, is(not(nullValue())));
                    assertThat(message2.getSubject(), is(subjectName));
                    assertThat(new String(message2.getData()), is("Hello 1"));

                    message2.ackSync(Duration.ofSeconds(5));
                }
            }
        }
    }

}