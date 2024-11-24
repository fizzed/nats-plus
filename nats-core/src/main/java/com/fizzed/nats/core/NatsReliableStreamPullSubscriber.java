package com.fizzed.nats.core;

import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class NatsReliableStreamPullSubscriber {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullSubscriber.class);

    // implemented here so its not in the public API
    private class InternalConnectionListener implements ConnectionListener {
        @Override
        public void connectionEvent(Connection conn, Events type) {
            if (type != null) {
                switch (type) {
                    case CLOSED:
                    case DISCONNECTED:
                    case LAME_DUCK:
                        log.warn("Connection to nats {}", type.name());
                        unhealthyRef.set("Nats connection problem: " + type.name());
                        // anyone waiting for the fetch() will have problems until we're connected again, also the fetch()
                        // will never return until its timeout expires, plus in our testing it also won't work!
                        final Thread fetchingThread = threadRef.getAndSet(null);
                        if (fetchingThread != null) {
                            // tell the fetching thread to give up!
                            fetchingThread.interrupt();
                        }
                        break;
                    case CONNECTED:
                    case RECONNECTED:
                        // are we back to being healthy again?
                        log.debug("Connection to nats {}", type.name());
                        unhealthyRef.set(null);
                        break;
                }
            }
        }
    }

    private final Connection connection;
    private final InternalConnectionListener connectionListener;
    private final AtomicReference<Thread> threadRef = new AtomicReference<>();
    private final AtomicReference<String> unhealthyRef = new AtomicReference<>();
    private String subject;
    private String durable;
    private JetStreamSubscription subscription;

    public NatsReliableStreamPullSubscriber(Connection connection) {
        this.connection = connection;
        this.connectionListener = new InternalConnectionListener();
    }

    public String getSubject() {
        return subject;
    }

    public NatsReliableStreamPullSubscriber setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getDurable() {
        return durable;
    }

    public NatsReliableStreamPullSubscriber setDurable(String durable) {
        this.durable = durable;
        return this;
    }

    public NatsReliableStreamPullSubscriber start() throws IOException, JetStreamApiException {
        if (this.subscription != null) {
            throw new IllegalStateException("Already subscribed");
        }

        this.connection.addConnectionListener(this.connectionListener);

        final JetStream js = this.connection.jetStream();

        this.subscription = js.subscribe(this.subject, PullSubscribeOptions.builder()
            .durable(this.durable)
            .build());

        return this;
    }

    public NatsReliableStreamPullSubscriber stop() throws IOException, JetStreamApiException {
        this.connection.removeConnectionListener(this.connectionListener);

        // try our best to officially unsubscribe
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }

        return this;
    }

    public boolean isHealthy() {
        return this.unhealthyRef.get() == null;
    }

    protected void checkHealth() throws NatsUnrecoverableException {
        String unhealthyMessage = this.unhealthyRef.get();
        if (unhealthyMessage != null) {
            throw new NatsUnrecoverableException(unhealthyMessage, null);
        }
    }

    public Message nextMessage(Duration pollTime) throws NatsUnrecoverableException, InterruptedException {
        final List<Message> messages = this.nextMessages(1, pollTime);

        return messages != null && !messages.isEmpty() ? messages.get(0) : null;
    }

    public List<Message> nextMessages(int batchSize, Duration pollTime) throws NatsUnrecoverableException, InterruptedException {
        // in nats.java < v2.20.0, they used synchronized() blocks which are not interruptible, so we will do that
        // check here before we try to do a fetch()
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (this.subscription == null) {
            throw new NatsUnrecoverableException("Not yet subscribed", null);
        }

        // verify we are healthy
        this.checkHealth();

        // NOTE: this library loves to throw java.lang.IllegalStateException: This subscription became inactive.
        // e.g. the server was shutdown, so the subscription became invalid
        List<Message> messages;

        // we need to store the thread that will be waiting (only 1 at a time)
        if (!this.threadRef.compareAndSet(null, Thread.currentThread())) {
            throw new NatsUnrecoverableException("Only 1 thread at a time is allowed to fetch per instance of " + this.getClass().getCanonicalName(), null);
        }
        try {
            messages = this.subscription.fetch(1, pollTime);
        } catch (IllegalStateException e) {
            // nats.java uses this to represent a lot of various problems, from subscriptions being inactive, etc.
            // in general, we will classify any of these as "unrecoverable" to help the client know to restart everything
            throw new NatsUnrecoverableException(e.getMessage(), e);
        } catch (IllegalMonitorStateException e) {
            // nats.java v2.20.0 - v2.20.4 tries to release a lock it never had if its interrupted during a fetch(), it can be
            // safely ignored in this version
            messages = null;
        } finally {
            // clear the thread reference
            this.threadRef.set(null);
        }

        if (messages == null || messages.isEmpty()) {
            // NOTE: even if we were interrupted, if fetch() returned results / partial results, its important those
            // still have a chance to be processed (for graceful shutdown scenarios), so we'll ignore if an interrupt occurred
            // were we interrupted? even if we were, what if we still got a message? we should probably return it
            if (Thread.interrupted()) {  // test and clear it
                // its possible the interrupt was actually from THIS class, in which case we'll throw that exception instead
                this.checkHealth();

                // otherwise, throw an interrupt exception
                throw new InterruptedException();
            }
        }

        return messages;
    }

}