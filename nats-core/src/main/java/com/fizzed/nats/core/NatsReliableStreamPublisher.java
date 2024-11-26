package com.fizzed.nats.core;

import io.nats.client.*;
import io.nats.client.api.PublishAck;

import java.io.IOException;

public class NatsReliableStreamPublisher {

    private final Connection connection;
    private JetStream js;

    public NatsReliableStreamPublisher(Connection connection) {
        this.connection = connection;
    }

    public NatsReliableStreamPublisher start() throws NatsUnrecoverableException {
        if (this.js != null) {
            throw new NatsUnrecoverableException("Publisher already active", null);
        }

        try {
            this.js = this.connection.jetStream();
        } catch (IOException e) {
            throw new NatsUnrecoverableException(e.getMessage(), e);
        }

        return this;
    }

    public PublishAck publish(Message message) throws NatsUnrecoverableException, NatsRecoverableException, InterruptedException {
        // in nats.java < v2.20.0, they used synchronized() blocks which are not interruptible, so we will do that
        // check here before we try to do a public()
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (this.js == null) {
            throw new NatsUnrecoverableException("Publisher is not active (did you forget to call .start() ?)", null);
        }

        try {
            return this.js.publish(message);
        } catch (JetStreamApiException | IOException e) {
            throw new NatsRecoverableException(e.getMessage(), e);
        } catch (IllegalStateException e) {
            // nats.java uses this to represent a lot of various problems, from subscriptions being inactive, etc.
            // in general, we will classify any of these as "unrecoverable" to help the client know to restart everything
            throw new NatsUnrecoverableException(e.getMessage(), e);
        } catch (IllegalMonitorStateException e) {
            // nats.java v2.20.0 - v2.20.4 tries to release a lock it never had if its interrupted during a fetch(), it can be
            // safely ignored in this version
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            throw new NatsUnrecoverableException(e.getMessage(), e);
        }
    }

}