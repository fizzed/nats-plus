package controllers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fizzed.nats.core.NatsConnectionProvider;
import com.fizzed.nats.core.NatsReliableStreamPublisher;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import ninja.Result;
import ninja.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.RequestConsumers;

@Singleton
public class ApplicationController {
    static private final Logger log = LoggerFactory.getLogger(ApplicationController.class);
    
    private final RequestConsumers requestConsumers;
    private final AtomicInteger messageCounter;
    private final NatsReliableStreamPublisher publisher;
    private boolean started;
    
    @Inject
    public ApplicationController(
            RequestConsumers requestConsumers,
            NatsConnectionProvider natsConnectionProvider) throws Exception {
        
        this.requestConsumers = requestConsumers;
        this.messageCounter = new AtomicInteger();
        this.publisher = new NatsReliableStreamPublisher(natsConnectionProvider);
    }
    
    public Result home() throws Exception {
        final StringBuilder html = new StringBuilder();
        
        html.append("<a href='/publish'>Publish Message</a><br/>");
        
        html.append("<br/>");
        
        /*html.append("<b>Channel Pool</b><br/>");
        html.append("Max Total: ").append(this.channelPool.getMaxTotalCount()).append("<br/>");
        html.append("Min Idle: ").append(this.channelPool.getMinIdleCount()).append("<br/>");
        html.append("Max Idle: ").append(this.channelPool.getMaxIdleCount()).append("<br/>");
        html.append("Idle: ").append(this.channelPool.getIdleCount()).append("<br/>");
        html.append("Active: ").append(this.channelPool.getActiveCount()).append("<br/>");*/
        
        html.append("<br/>");
        
        html.append("<b>Request Consumers</b><br/>");
        
        if (this.requestConsumers.isStarted()) {
            html.append("<a href='/stop'>Stop</a><br/>");
        } else if (this.requestConsumers.isStopped()) {
            html.append("<a href='/start'>Start</a><br/>");
        } else {
            html.append("Currently ").append(this.requestConsumers.getState()).append("<br/>");
        }
        
        html.append("<br/>");
        
        this.requestConsumers.getRunnables().forEach(wr -> {
            html.append(wr.getName()).append("; ").append(wr.getState()).append("; ").append(wr.getMessage());
            html.append("<br/>");
        });
        
        return Results.html()
            .renderRaw(html.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public Result stop() throws Exception {
        this.requestConsumers.shutdown();
        
        return Results.redirect("/");
    }
    
    public Result start() throws Exception {
        this.requestConsumers.start();
        
        return Results.redirect("/");
    }
    
    public Result publish() throws Exception {
        if (!started) {
            this.publisher.start();
            started = true;
        }

        final PublishAck ack = this.publisher.publish(NatsMessage.builder()
            .subject("demo.requests.queue")
            .data("Hello " + this.messageCounter.incrementAndGet() + " @ " + System.currentTimeMillis())
            .build());

        log.debug("Published message: " + ack);
        
        return Results.redirect("/");
    }
    
}