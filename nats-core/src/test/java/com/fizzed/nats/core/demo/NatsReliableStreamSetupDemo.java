package com.fizzed.nats.core.demo;

import com.fizzed.nats.core.NatsReliableStreamPullSubscriber;
import io.nats.client.*;
import io.nats.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static com.fizzed.nats.core.NatsHelper.dumpMessage;

public class NatsReliableStreamSetupDemo {
    static private final Logger log = LoggerFactory.getLogger(NatsReliableStreamSetupDemo.class);

    static public void main(String[] args) throws Exception {
        try (Connection connection = Nats.connect(Options.builder()
                .server("nats://localhost:14222")
                .connectionName(NatsReliableStreamSetupDemo.class.getCanonicalName())
                .build())) {

            log.info("Connected to nats server: {}", connection.getConnectedUrl());

            final JetStreamManagement jsm = connection.jetStreamManagement();

            // delete all streams
            final List<String> existingStreamNames = jsm.getStreamNames();
            for (String streamName : existingStreamNames) {
                jsm.deleteStream(streamName);
                log.info("Deleted stream: name={}", streamName);
            }

            /*// create work queue stream
            final String requestQueueStreamName = "request-queue-stream";
            final String requestQueueStreamConsumer = "request-queue-consumer";
            final String requestQueueSubjects = "request.queue.priority";
            jsm.addStream(StreamConfiguration.builder()
                .name(requestQueueStreamName)
                .storageType(StorageType.File)
                .subjects(requestQueueSubjects)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .discardPolicy(DiscardPolicy.Old)
                .build());

            log.info("Created stream: name={}, subjects={}", requestQueueStreamName, requestQueueSubjects);

            ConsumerInfo consumerInfo = jsm.createConsumer(requestQueueStreamName, ConsumerConfiguration.builder()
                .filterSubject(requestQueueSubjects)
                .durable(requestQueueStreamConsumer)
                .build());

            log.info("Created durable consumer: stream={}, durable={}", requestQueueStreamName, requestQueueStreamConsumer);

            log.info("Consumer: {}", consumerInfo);*/
        }
    }

}