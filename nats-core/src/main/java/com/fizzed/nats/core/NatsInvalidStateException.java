package com.fizzed.nats.core;

public class NatsInvalidStateException extends RuntimeException {

    public NatsInvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }

}