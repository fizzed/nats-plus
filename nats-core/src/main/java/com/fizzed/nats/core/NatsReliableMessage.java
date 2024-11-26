package com.fizzed.nats.core;

import io.nats.client.Message;

public class NatsReliableMessage {

    static public void ack(Message message) throws NatsUnrecoverableException, InterruptedException {
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

}