package io.joshworks.restclient.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Josh Gontijo on 5/5/17.
 */
public class ClientContainer {

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
    private static final Map<String, RestClient> clients = new ConcurrentHashMap<>();

    //Global monitor
    private static final IdleConnectionMonitor monitor = new IdleConnectionMonitor(clients::values);

    private ClientContainer() {

    }

    static void addClient(RestClient client) {
        logger.info("New rest client created, id: " + client.id);
        clients.put(client.id, client);

    }

    static void removeClient(RestClient client) {
        logger.info("Rest client removed, id: " + client.id);
        clients.remove(client.id);
    }

    public static void shutdown() {
        for (Map.Entry<String, RestClient> clientEntry : clients.entrySet()) {
            logger.info("Shutting down rest client, id: " + clientEntry.getKey());
            try {
                clientEntry.getValue().shutdown();
            } catch (IOException e) {
                logger.error("Failed to shutdown client '" + clientEntry.getKey() + "'", e);
            }
        }

        monitor.shutdown();
    }
}
