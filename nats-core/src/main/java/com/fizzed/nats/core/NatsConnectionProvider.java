package com.fizzed.nats.core;

import io.nats.client.Connection;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class NatsConnectionProvider implements Supplier<Connection> {

    private final Supplier<Connection> supplier;
    private final AtomicReference<Connection> connectionRef;

    public NatsConnectionProvider(Supplier<Connection> supplier) {
        this.supplier = supplier;
        this.connectionRef = new AtomicReference<>();
    }

    public Connection get() {
        Connection connection = this.connectionRef.get();
        if (connection == null) {
            synchronized (this) {
                // we need to make sure its still not yet set (double lock)
                connection = this.connectionRef.get();
                if (connection == null) {
                    connection = supplier.get();
                    this.connectionRef.set(connection);
                }
            }
        }
        return connection;
    }

}