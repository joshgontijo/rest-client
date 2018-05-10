package io.joshworks.restclient.http;

import io.joshworks.restclient.http.utils.ClientStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Josh Gontijo on 5/5/17.
 */
public class ClientContainer {

    private static final Logger logger = LoggerFactory.getLogger(ClientContainer.class);
    private static final Map<String, RestClient> clients = new ConcurrentHashMap<>();

    //Global monitor
    private static final IdleConnectionMonitor monitor = new IdleConnectionMonitor(clients::values);

    private ClientContainer() {
        Runtime.getRuntime().addShutdownHook(new Thread(ClientContainer::shutdown));
    }

    static void addClient(RestClient client) {
        logger.info("New rest client created, id: {}", client.id);
        clients.put(client.id, client);
    }

    static void removeClient(RestClient client) {
        logger.info("Rest client removed, id: {}", client.id);
        clients.remove(client.id);
    }

    public static int size() {
        return clients.size();
    }

    public static Map<String, ClientStats> stats() {
        return clients.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, d -> d.getValue().stats()));
    }

    public static synchronized void shutdown() {
        for (Map.Entry<String, RestClient> clientEntry : clients.entrySet()) {
            logger.info("Shutting down rest client, id: {}", clientEntry.getKey());
            try {
                clientEntry.getValue().close();
            } catch (Exception e) {
                logger.error("Failed to close client '" + clientEntry.getKey() + "'", e);
            }
        }

        monitor.shutdown();
    }
}
