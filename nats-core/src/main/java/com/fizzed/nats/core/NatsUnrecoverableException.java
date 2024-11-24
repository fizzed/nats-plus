package com.fizzed.nats.core;

public class NatsUnrecoverableException extends Exception {

    public NatsUnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }

}