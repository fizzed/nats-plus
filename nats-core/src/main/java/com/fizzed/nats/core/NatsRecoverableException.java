package com.fizzed.nats.core;

/**
 * Indicates an exception occurred while publishing/subscribing and that we believe it's recoverable, so it'd a best
 * practice to retry what you just did after waiting for a period of time. An example is a connection issue to the
 * NATS server.
 */
public class NatsRecoverableException extends Exception {

    public NatsRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }

}