package com.fizzed.nats.core;

import io.nats.client.Message;
import io.nats.client.impl.Headers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class NatsReliableMessage {

    private final Message message;

    public NatsReliableMessage(Message message) {
        this.message = message;
    }

    public Message unwrap() {
        return this.message;
    }

    /**
     * Gets a UTF-8 encoded string of the data
     * @return
     */
    public String getString() {
        return new String(this.message.getData(), StandardCharsets.UTF_8);
    }

    public Headers getHeaders() {
        return this.message.getHeaders();
    }

    public byte[] getData() {
        return this.message.getData();
    }

    public String getSubject() {
        return this.message.getSubject();
    }

    public void ack() throws NatsUnrecoverableException, InterruptedException {
        boolean success = false;
        try {
            message.ack();
            success = true;
        } catch (IllegalStateException e) {
            // nats.java uses this to represent a lot of various problems, from subscriptions being inactive, etc.
            // in general, we will classify any of these as "unrecoverable" to help the client know to restart everything
            throw new NatsUnrecoverableException(e.getMessage(), e);
        } catch (IllegalMonitorStateException e) {
            // nats.java v2.20.0 - v2.20.4 tries to release a lock it never had if its interrupted during a fetch(), it can be
            // safely ignored in this version
        }

        if (!success) {
            if (Thread.interrupted()) {  // test and clear it
                // otherwise, throw an interrupt exception
                throw new InterruptedException();
            }
        }
    }

    public void ackSync(Duration timeout) throws TimeoutException, NatsUnrecoverableException, InterruptedException {
        boolean success = false;
        try {
            message.ackSync(timeout);
            success = true;
        } catch (IllegalStateException e) {
            // nats.java uses this to represent a lot of various problems, from subscriptions being inactive, etc.
            // in general, we will classify any of these as "unrecoverable" to help the client know to restart everything
            throw new NatsUnrecoverableException(e.getMessage(), e);
        } catch (IllegalMonitorStateException e) {
            // nats.java v2.20.0 - v2.20.4 tries to release a lock it never had if its interrupted during a fetch(), it can be
            // safely ignored in this version
        }

        if (!success) {
            if (Thread.interrupted()) {  // test and clear it
                // otherwise, throw an interrupt exception
                throw new InterruptedException();
            }
        }
    }

}