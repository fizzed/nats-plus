package com.fizzed.nats.ninja;

import com.fizzed.nats.core.NatsConnectionProvider;
import com.google.inject.AbstractModule;
import io.nats.client.Connection;

public class NinjaNatsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NatsConnectionProvider.class).toProvider(NinjaNatsConnectionProvider.class);
        bind(NinjaNatsLifecycle.class);
    }
    
}