package com.fizzed.nats.core;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.api.*;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NatsHelper {

    static public List<NatsReliableMessage> toReliableMessageList(List<Message> messages) {
        // if just a single message, we'll optimize for that
        if (messages == null) {
            return null;
        }
        if (messages.size() == 1) {
            return Collections.singletonList(new NatsReliableMessage(messages.get(0)));
        }
        List<NatsReliableMessage> newMessages = new ArrayList<>(messages.size());
        for (Message m : messages) {
            newMessages.add(new NatsReliableMessage(m));
        }
        return newMessages;
    }

    static public String dumpMessage(Message message) {
        return dumpMessage(message, 255);
    }

    static public String dumpMessage(NatsReliableMessage message) {
        if (message == null) {
            return null;
        }
        return dumpMessage(message.unwrap(), 255);
    }

    static public String dumpMessage(Message message, int maxDataLength) {
        if (message == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("subject: ").append(message.getSubject()).append("\n");

        final NatsJetStreamMetaData md = message.metaData();
        if (md != null) {
            final long seqNo = md.streamSequence();
            sb.append("seqNo: ").append(seqNo).append("\n");
        }

        final Headers headers = message.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            sb.append("headers: count=").append(headers.size()).append("\n");
            headers.forEach((key, values) -> {
                values.forEach(value -> {
                    sb.append("  ").append(key).append(": ").append(value).append("\n");
                });
            });
        }

        final byte[] data = message.getData();
        sb.append("data: bytes=").append(data != null ? data.length : 0).append("\n");
        if (data != null && data.length > 0) {
            // cap logging to X bytes
            if (data.length > maxDataLength) {
                sb.append(" ").append(new String(data, 0, maxDataLength, StandardCharsets.UTF_8)).append("\n");
                sb.append(" <truncated ").append(data.length - maxDataLength).append(" bytes>\n");
            } else {
                sb.append(" ").append(new String(data, StandardCharsets.UTF_8)).append("\n");
            }
        }

        return sb.toString();
    }

    static public void deleteAllStreams(Connection connection) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = connection.jetStreamManagement();
        final List<String> streamNames = jsm.getStreamNames();
        for (String streamName : streamNames) {
            jsm.deleteStream(streamName);
        }
    }

    static public StreamInfo createWorkQueueStream(Connection connection, String streamName, String subjects) throws IOException, JetStreamApiException {
        JetStreamManagement jsm = connection.jetStreamManagement();
        return jsm.addStream(StreamConfiguration.builder()
            .name(streamName)
            .storageType(StorageType.File)
            .subjects(subjects)
            .retentionPolicy(RetentionPolicy.WorkQueue)
            .discardPolicy(DiscardPolicy.Old)
            .build());
    }

}