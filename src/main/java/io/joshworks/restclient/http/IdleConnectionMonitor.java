package io.joshworks.restclient.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Created by Josh Gontijo on 5/5/17.
 */
public class IdleConnectionMonitor extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(IdleConnectionMonitor.class);


    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Supplier<Collection<RestClient>> clients;


    public IdleConnectionMonitor(Supplier<Collection<RestClient>> clients) {
        this.clients = clients;
        super.setDaemon(true);
        this.start();
    }


    public void shutdown() {
        logger.info("Stopping idle client connection monitor");
        running.set(false);
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        if (running.get()) {
            logger.warn("Monitor is already running");
            return;
        }
        running.set(true);
        try {
            logger.info("Starting idle connection monitor");
            while (!Thread.currentThread().isInterrupted() && running.get()) {
                synchronized (this) {
                    wait(5000);

                    // Close expired connections
                    // Optionally, close connections
                    clients.get().iterator().forEachRemaining(RestClient::closeIdleConnections);

                }
            }
        } catch (InterruptedException ex) {
            logger.warn("Idle client connection stopped abruptly");
            return;
        }
        logger.info("Idle client connection stopped gracefully");
    }

}