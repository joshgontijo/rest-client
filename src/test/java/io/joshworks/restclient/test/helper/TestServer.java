package io.joshworks.restclient.test.helper;

import io.joshworks.snappy.SnappyServer;

import java.util.HashMap;
import java.util.Map;

import static io.joshworks.snappy.SnappyServer.delete;
import static io.joshworks.snappy.SnappyServer.enableTracer;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.options;
import static io.joshworks.snappy.SnappyServer.post;
import static io.joshworks.snappy.SnappyServer.put;
import static io.joshworks.snappy.parser.MediaTypes.produces;

/**
 * Created by Josh Gontijo on 5/10/17.
 */
public class TestServer {

    private Map<String, String> dataSink = new HashMap<>();

    public static void start() {
        get("/hello", exchange -> exchange.send("Hello"), produces("txt"));
        get("/hello/{name}", exchange -> exchange.send(new TestData(exchange.pathParameter("name"))));
        get("/get", exchange -> exchange.status(200).end());
        get("/500", exchange -> exchange.status(500));
        get("/echo", exchange -> {
            Map<String, Object> echoResponse = new HashMap<>();
            echoResponse.put("headers", exchange.headers());
            echoResponse.put("queryParams", exchange.queryParameters());
            echoResponse.put("path", exchange.path());

            exchange.send(echoResponse);
        });

        //echo
        post("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        put("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        post("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        put("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        delete("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        options("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));


        get("/testData", exchange -> exchange.send(new TestData("yolo")));

        enableTracer();
        SnappyServer.start();
    }

    public static void stop() {
        SnappyServer.stop();
    }

}
