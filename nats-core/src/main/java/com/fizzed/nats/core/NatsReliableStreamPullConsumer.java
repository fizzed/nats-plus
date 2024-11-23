package com.fizzed.nats.core;

import io.nats.client.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class NatsReliableStreamPullConsumer {

    private final Connection connection;
    private String subject;
    private String durable;
    private JetStreamSubscription subscription;

    public NatsReliableStreamPullConsumer(Connection connection) {
        this.connection = connection;
    }

    public String getSubject() {
        return subject;
    }

    public NatsReliableStreamPullConsumer setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getDurable() {
        return durable;
    }

    public NatsReliableStreamPullConsumer setDurable(String durable) {
        this.durable = durable;
        return this;
    }

    public NatsReliableStreamPullConsumer start() throws IOException, JetStreamApiException {
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
        if (this.subscription == null) {
            throw new IllegalStateException("Not subscribed");
        }

        final List<Message> messages = this.subscription.fetch(1, pollTime);

        final Message message = messages != null && !messages.isEmpty() ? messages.get(0) : null;

        return message;

        /*if (message != null) {
            NatsJetStreamMetaData md = message.metaData();
            final long seqNo = md.streamSequence();
            final ZonedDateTime ts = md.timestamp();

            log.info("Consumed message: seqNo={}, ts={}, headers={}, subject={}, data={}",
                seqNo, ts, message.getHeaders(), message.getSubject(), new String(message.getData()));

            // if you don't ack the message, problems arise and it'll eventually be redelivered and retried
            message.ack();
        } else {
            log.info("No message received");
        }*/
    }

}