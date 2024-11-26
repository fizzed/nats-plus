package com.fizzed.nats.core;

/**
 * Indicates that an error occurred and the safest course of action is to start publishing/subscribing ALL over again
 * after waiting a brief period of time. For example, some sort of queue is destroyed, subscription becomes invalid,
 * etc.
 */
public class NatsUnrecoverableException extends Exception {

    public NatsUnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }

}