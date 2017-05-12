package io.joshworks.restclient.test.helper;

import io.joshworks.snappy.SnappyServer;

import java.util.HashMap;
import java.util.Map;

import static io.joshworks.snappy.SnappyServer.*;
import static io.joshworks.snappy.parser.MediaTypes.produces;

/**
 * Created by Josh Gontijo on 5/10/17.
 */
public class TestServer {

    private Map<String, String> dataSink = new HashMap<>();

    public void start() {
        get("/get", exchange -> {
        });

        get("/500", exchange -> exchange.status(500));

        get("/getJson/{id}", exchange -> {
            String id = exchange.pathParameter("id");
            String data = dataSink.get(id);
            if (data == null) {
                exchange.status(404);
                return;
            }
            exchange.send(data);
        }, produces("json"));

        get("/getJson", exchange -> exchange.send(dataSink.values()));

        post("/postJson/{id}", exchange -> {
            String id = exchange.pathParameter("id");
            String body = exchange.body().asString();
            dataSink.put(id, body);
            exchange.send(body); //echo
        });

        post("/putJson/{id}", exchange -> {
            String id = exchange.pathParameter("id");
            String body = exchange.body().asString();
            dataSink.put(id, body);
            exchange.send(body); //echo
        });

        delete("/deleteJson/{id}", exchange -> {
            String id = exchange.pathParameter("id");
            if (!dataSink.containsKey(id)) {
                exchange.status(404);
                return;
            }
            dataSink.remove(id);
        });

        enableTracer();
        SnappyServer.start();
    }

    public void stop() {
        SnappyServer.stop();
    }

}
