package com.fizzed.nats.core;

import io.nats.client.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class NatsReliableStreamPullSubscriber {

    private final Connection connection;
    private String subject;
    private String durable;
    private JetStreamSubscription subscription;

    public NatsReliableStreamPullSubscriber(Connection connection) {
        this.connection = connection;
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

        final JetStream js = this.connection.jetStream();

        this.subscription = js.subscribe(this.subject, PullSubscribeOptions.builder()
            .durable(this.durable)
            .build());

        return this;
    }

    public Message nextMessage(Duration pollTime) throws InterruptedException {
        final List<Message> messages = this.nextMessages(1, pollTime);

        return messages != null && !messages.isEmpty() ? messages.get(0) : null;
    }

    public List<Message> nextMessages(int batchSize, Duration pollTime) throws InterruptedException {
        if (this.subscription == null) {
            throw new IllegalStateException("Not subscribed");
        }

        // NOTE: this library loves to throw java.lang.IllegalStateException: This subscription became inactive.
        // e.g. the server was shutdown, so the subscription became invalid
        final List<Message> messages = this.subscription.fetch(1, pollTime);

        if (messages == null || messages.isEmpty()) {
            // NOTE: even if we were interrupted, if fetch() returned results / partial results, its important those
            // still have a chance to be processed (for graceful shutdown scenarios), so we'll ignore if an interrupt occurred
            // were we interrupted? even if we were, what if we still got a message? we should probably return it
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }

        return messages;
    }

}