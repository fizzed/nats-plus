package com.fizzed.nats.core;

import com.fizzed.crux.util.WaitFor;
import io.nats.NatsServerRunner;
import io.nats.client.Connection;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.fizzed.nats.core.NatsHelper.createWorkQueueStream;
import static com.fizzed.nats.core.NatsHelper.dumpMessage;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.fail;

class NatsReliableStreamPullSubscriberTest extends NatsBaseTest {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullSubscriberTest.class);

    @Test
    void nextMessagePublishAndSubscribe() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // how we'll wait for replies
                final BlockingDeque<NatsReliableMessage> receivedMessages = new LinkedBlockingDeque<>();
                final CountDownLatch subscriberStoppedLatch = new CountDownLatch(1);
                final AtomicBoolean shuttingDown = new AtomicBoolean(false);

                final Thread subscriberThread = new Thread(() -> {
                    try {
                        while (true) {
                            final NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(30));

                            receivedMessages.push(message);

                            message.ack();
                        }
                    } catch (InterruptedException | NatsUnrecoverableException e) {
                        // this is okay if we're being flagged to shutdown
                        if (shuttingDown.get()) {
                            log.debug("Expected interruption / unrecoverable exception", e);
                        } else {
                            log.error("UNEXPECTED EXCEPTION", e);
                            fail("Unexpected exception thrown during subscribe!");
                        }
                    } catch (Exception e) {
                        log.error("UNEXPECTED EXCEPTION", e);
                        fail("Unexpected exception thrown during subscribe!");
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

                NatsReliableMessage message1 = receivedMessages.poll(5, TimeUnit.SECONDS);

                assertThat(message1.getSubject(), is(subjectName));
                assertThat(message1.getString(), is("Hello 1"));


                final PublishAck ack2 = publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 2")
                    .build());

                NatsReliableMessage message2 = receivedMessages.poll(5, TimeUnit.SECONDS);

                assertThat(message2, is(not(nullValue())));
                assertThat(message2.getSubject(), is(subjectName));
                assertThat(message2.getString(), is("Hello 2"));

                // interrupt subscriber, wait for it to exit as gracefully as we can (we test for this logic in other
                // unit tests so we are just trying to avoid weird log messages when server shuts down as part of cleanup
                shuttingDown.set(true);
                subscriberThread.interrupt();

                if (!subscriberStoppedLatch.await(3, TimeUnit.SECONDS)) {
                    fail("Subscriber thread did not successfully stop");
                }
            }
        }
    }

    @Test
    void startStop() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    // do NOT call start yet
                    ;

                try {
                    subscriber.nextMessage(Duration.ofSeconds(5));
                    fail("Expected nextMessage() to have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                } catch (Exception e) {
                    // unexpected
                    fail("Unexpected exception thrown during subscribe: " + e.getMessage());
                }

                // what happens if you call stop before start?
                subscriber.stop();

                // start subscriber, which should allow things to work now
                subscriber.start();

                // what happens if you call it when already started?
                try {
                    subscriber.start();
                    fail("Expected start() to have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                }

                // nextMessage() should succeed now
                NatsReliableMessage message = subscriber.nextMessage(Duration.ofMillis(250L));

                assertThat(message, is(nullValue()));

                // stop should work
                subscriber.stop();

                // should be able to call it twice
                subscriber.stop();
            }
        }
    }

    @Test
    void startAfterConnectionFailed() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    // do NOT call start yet
                    ;

                nats.shutdown(true);

                try {
                    subscriber.start();
                    fail("Expected start() to have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                }

                // start the nats server back up
                try (NatsServerRunner nats2 = this.buildNatsServerRunner()) {
                    // the subscriber should start now
                    subscriber.start();
                }
            }
        }
    }

    @Test
    void stopAfterConnectionFailed() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                nats.shutdown(true);

                Thread.sleep(1000L);

                log.debug("Stopping subscriber now...");
                subscriber.stop();
                subscriber.stop();
                log.debug("Stopped subscriber");
            }
        }
    }

    @Test
    void nextMessagesPublishAndSubscribe() throws Exception {
        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                final String subjectName = this.randomSubjectName();

                createWorkQueueStream(connection, this.randomStreamName(), subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(this.randomDurableName())
                    .start();

                // subscribe will wait for no messages and return empty
                final List<NatsReliableMessage> messages1 = subscriber.nextMessages(2, Duration.ofMillis(250L));

                assertThat(messages1, is(nullValue()));

                // publish 1 message, but we'll ask for 2, see if it waits the full timeout?
                publisher.publish(NatsMessage.builder()
                    .subject(subjectName)
                    .data("Hello 1")
                    .build());

                // we'll request a batch of 2 messages even though just 1 was published, does it wait the full amount?
                final long nowStart1 = System.currentTimeMillis();
                final List<NatsReliableMessage> messages2 = subscriber.nextMessages(20, Duration.ofSeconds(10));
                final long nowStart2 = System.currentTimeMillis();

                assertThat(messages2, hasSize(1));
                assertThat(nowStart2 - nowStart1, lessThan(3000L));     // nextMessages should have been quick
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
                createWorkQueueStream(connection, streamName, subjectName);

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

                    NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(5));

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

                final NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(5));

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
                createWorkQueueStream(connection, streamName, subjectName);

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

                        final NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(10));

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
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPublisher publisher = new NatsReliableStreamPublisher(connection)
                    .start();

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // start thread to consume
                final CountDownLatch nextMessageBeforeLatch1 = new CountDownLatch(1);
                final CountDownLatch nextMessageRecoverableLatch = new CountDownLatch(1);

                final Thread subscriberThread1 = new Thread(() -> {
                    try {
                        // NOTE: this will help our unit test get as close as possible to waiting for us to enter nextMessage()
                        nextMessageBeforeLatch1.countDown();

                        final NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(20));

                        if (message != null) {
                            log.debug("Received message: {}", dumpMessage(message.unwrap()));
                        }

                        fail("nextMessage() expected to throw exception, not exit");
                    } catch (NatsRecoverableException e) {
                        // expected
                        log.debug("nextMesssage threw an expected exception", e);
                        nextMessageRecoverableLatch.countDown();
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
                if (!nextMessageRecoverableLatch.await(5, TimeUnit.SECONDS)) {
                    fail("nextMessage() did not throw an unrecoverable exception");
                }

                // since the connection is still down, we should quickly get a recoverable exception
                try {
                    NatsReliableMessage message1 = subscriber.nextMessage(Duration.ofSeconds(10));
                    fail("nextMessage() should have failed");
                } catch (NatsRecoverableException e) {
                    // expected
                    log.debug("nextMesssage threw an expection exception", e);
                } catch (Exception e) {
                    // anything else would be unexpected
                    log.error("Unexpected exception", e);
                    fail("Unexpected exception " + e.getMessage());
                }

                // now, we'll start the server back up, which should allow the
                try (NatsServerRunner nats2 = this.buildNatsServerRunner()) {
                    // we need to allow the connection to recover, which should fix the subscriber too
                    WaitFor.of(subscriber::isHealthy)
                        .requireMillis(5000L, 100L);

                    // this should now work, but return nothing yet
                    NatsReliableMessage message1 = subscriber.nextMessage(Duration.ofSeconds(1));

                    assertThat(message1, is(nullValue()));

                    // let's publish a message and then get it to be extra safe the subscription works
                    publisher.publish(NatsMessage.builder()
                        .subject(subjectName)
                        .data("Hello 1")
                        .build());

                    NatsReliableMessage message2 = subscriber.nextMessage(Duration.ofSeconds(5));

                    assertThat(message2, is(not(nullValue())));
                    assertThat(message2.getSubject(), is(subjectName));
                    assertThat(new String(message2.getData()), is("Hello 1"));

                    message2.ackSync(Duration.ofSeconds(5));
                }
            }
        }
    }

    @Test
    void nextMessageQueueDeletedDuringFetch() throws Exception {
        final String streamName = this.randomStreamName();
        final String subjectName = this.randomSubjectName();
        final String durableName = this.randomDurableName();

        try (NatsServerRunner nats = this.buildNatsServerRunner()) {
            try (Connection connection = this.connectNats(nats, true)) {
                // create stream for work queue
                createWorkQueueStream(connection, streamName, subjectName);

                final NatsReliableStreamPullSubscriber subscriber = new NatsReliableStreamPullSubscriber(connection)
                    .setSubject(subjectName)
                    .setDurable(durableName)
                    .start();

                // start thread to consume
                final CountDownLatch nextMessageBeforeLatch = new CountDownLatch(1);
                final CountDownLatch nextMessageUnrecoverableLatch = new CountDownLatch(1);

                final Thread subscriberThread1 = new Thread(() -> {
                    try {
                        // NOTE: this will help our unit test get as close as possible to waiting for us to enter nextMessage()
                        nextMessageBeforeLatch.countDown();

                        final NatsReliableMessage message = subscriber.nextMessage(Duration.ofSeconds(20));

                        fail("nextMessage() expected to throw exception, not exit");
                    } catch (NatsUnrecoverableException e) {
                        // expected
                        log.debug("nextMesssage threw an expected exception", e);
                        nextMessageUnrecoverableLatch.countDown();
                    } catch (Exception e) {
                        log.error("Unexpected exception", e);
                        fail("Unexpected exception " + e.getMessage());
                    }
                });
                subscriberThread1.start();

                // await for message consuming to be happening
                nextMessageBeforeLatch.await();

                // to give our subscriberThread as much time to make sure its in the nats.java fetch() we will pause
                // for a brief period to help guarantee as much as we can that it'll be waiting
                Thread.sleep(1000L);

                // let's delete the queue now
                this.cleanNats(connection);

                // the subscriber should now receive an unrecoverable exception
                if (!nextMessageUnrecoverableLatch.await(5, TimeUnit.SECONDS)) {
                    fail("nextMessage() did not throw an unrecoverable exception");
                }

                // subscriber stop should be okay
                subscriber.stop();

                // start should not succeed if queues are still deleted
                try {
                    subscriber.start();
                    fail("start() should have failed");
                } catch (NatsUnrecoverableException e) {
                    // expected
                }

                // create queues again
                createWorkQueueStream(connection, streamName, subjectName);

                // start should now work
                subscriber.start();
            }
        }
    }

}