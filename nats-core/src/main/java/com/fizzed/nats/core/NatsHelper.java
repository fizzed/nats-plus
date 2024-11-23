package com.fizzed.nats.core;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;

import java.nio.charset.StandardCharsets;

public class NatsHelper {

    static public String dumpMessage(Message message) {
        return dumpMessage(message, 255);
    }

    static public String dumpMessage(Message message, int maxDataLength) {
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

}