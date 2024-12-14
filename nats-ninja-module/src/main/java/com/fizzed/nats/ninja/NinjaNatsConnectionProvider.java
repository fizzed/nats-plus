package com.fizzed.nats.ninja;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.fizzed.nats.core.NatsConnectionProvider;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.ServerInfo;
import ninja.utils.NinjaProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Singleton
public class NinjaNatsConnectionProvider implements Provider<NatsConnectionProvider> {
    static private final Logger log = LoggerFactory.getLogger(NinjaNatsConnectionProvider.class);

    private final Options options;
    private NatsConnectionProvider connectionProvider;
    
    @Inject
    public NinjaNatsConnectionProvider(NinjaProperties ninjaProperties) {
        log.info("Nats connection initializing...");

        Options.Builder optionsBuilder = Options.builder();
        
        final String url = ninjaProperties.get("nats.url");
        if (!isBlank(url)) {
            // split by comma
            String[] urls = StringUtils.split(url, ",");
            for (String v : urls) {
                String _url = v.trim();
                log.info("server: {}", _url);
                optionsBuilder.server(_url);
            }
        }

        final String username = ninjaProperties.get("nats.username");
        if (!isBlank(username)) {
            final String password = ninjaProperties.get("nats.password");
            log.info("username/password: {}/<redacted>", username);
            optionsBuilder.userInfo(username, password);
        }
        
        final String connectionName = ninjaProperties.get("nats.connection_name");
        if (!isBlank(connectionName)) {
            log.info("connectionName: {}", connectionName);
            optionsBuilder.connectionName(connectionName);
        }

        this.options = optionsBuilder.build();
    }
    
    @Override
    public NatsConnectionProvider get() {
        if (this.connectionProvider == null) {
            this.connectionProvider = new NatsConnectionProvider(() -> {
                try {
                    log.info("Connecting to nats server...");

                    Connection connection = Nats.connect(this.options);

                    // get some information about nats
                    final ServerInfo serverInfo = connection.getServerInfo();

                    log.info("Nats server version: {}", serverInfo.getVersion());
                    log.info("Nats server name: {}", serverInfo.getServerName());
                    log.info("Nats server cluster: {}", serverInfo.getCluster());
                    log.info("Nats server protocol version: {}", serverInfo.getProtocolVersion());
                    log.info("Nats server jetstream enabled: {}", serverInfo.isJetStreamAvailable());
                    log.info("Connected to nats server!");

                    return connection;
                } catch (Exception e) {
                    log.error("Failed to connect to nats server!", e);
                    throw new RuntimeException(e);
                }
            });
        }

        return this.connectionProvider;
    }
    
}