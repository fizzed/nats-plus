package com.fizzed.nats.core;

import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;

public class NatsReliableStreamPullConsumerDemo {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamPullConsumerDemo.class);

    static public void main(String[] args) throws Exception {
        final Options o = new Options.Builder()
            .server("nats://localhost:14222")
            .maxReconnects(-1)
            .verbose()
            .connectionName(NatsReliableStreamPullConsumerDemo.class.getCanonicalName())
            .build();

        try (Connection nc = Nats.connect(o)) {
            log.info("Connected to nats server!");

            final NatsReliableStreamPullConsumer consumer = new NatsReliableStreamPullConsumer(nc)
                .setSubject("REQUESTS.priority")
                .setDurable("REQUESTS-priority-processor")
                .start();

            while (true) {
                log.debug("Waiting for next message...");

                final Message message = consumer.nextMessage(Duration.ofSeconds(120));

                if (message == null) {
                    log.warn("Uh oh, no message received");
                    continue;   // keep searching for messages
                }

                log.debug("Received message:\n{}", dumpMessage(message));

                message.ack();
            }

            /*final Thread subscriberThread = new Thread(() -> {
                while (true) {
                    try {
                        log.info("Waiting for next message...");


                        final List<Message> messages = jss.fetch(1, Duration.ofSeconds(1000));
                        final Message message = messages != null && !messages.isEmpty() ? messages.get(0) : null;

                        if (message != null) {
                            NatsJetStreamMetaData md = message.metaData();
                            final long seqNo = md.streamSequence();
                            final ZonedDateTime ts = md.timestamp();

                            log.info("Consumed message: seqNo={}, ts={}, headers={}, subject={}, data={}",
                                seqNo, ts, message.getHeaders(), message.getSubject(), new String(message.getData()));

                            // if you don't ack the message, problems arise and it'll eventually be redelivered and retried
                            message.ack();
                        } else {
                            log.info("No message received");
                        }

                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        log.error("Error consuming message", e);
                        try {
                            Thread.sleep(5000L);
                        } catch (InterruptedException ex) {
                            log.error("Error sleeping", ex);
                        }
                    }
                }
            });*/

            /*nc.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionEvent(Connection conn, Events type) {
                    System.out.println("Connection event type=" + type);
                    if (type == Events.DISCONNECTED || type == Events.CLOSED) {
                        subscriberThread.interrupt();
                        *//*try {
                            nc.close();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }*//*
                    }
                }
            });

            subscriberThread.start();
            subscriberThread.join();*/

        }
    }

}