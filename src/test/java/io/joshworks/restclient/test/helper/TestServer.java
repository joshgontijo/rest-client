package io.joshworks.restclient.test.helper;

import io.joshworks.snappy.Exchange;
import io.joshworks.snappy.SnappyServer;
import io.joshworks.snappy.multipart.MultipartExchange;
import io.undertow.util.HttpString;

import java.util.HashMap;
import java.util.Map;

import static io.joshworks.snappy.SnappyServer.delete;
import static io.joshworks.snappy.SnappyServer.enableTracer;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.head;
import static io.joshworks.snappy.SnappyServer.multipart;
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
        get("/nullBody", Exchange::end, produces("txt"));
        get("/hello/{name}", exchange -> exchange.send(new TestData(exchange.pathParameter("name"))));
        get("/get", exchange -> exchange.status(200).end());
        get("/500", exchange -> exchange.status(500));


        //redirect
        get("/redirect300", exchange -> exchange.status(300).header("Location", "http://localhost:9000/hello"));
        get("/redirect301", exchange -> exchange.status(301).header("Location", "https://localhost:9000/hello"));
        get("/redirect302", exchange -> exchange.status(302).header("Location", "http://localhost:9000/hello"));
        get("/redirect303", exchange -> exchange.status(303).header("Location", "http://localhost:9000/hello"));

        //echo
        get("/echo", exchange -> exchange.send(getRequestData(exchange)));
        multipart("/echoFormData", exchange -> {
            Map<String, Object> echoResponse = getRequestData(exchange);
            echoResponse.put("body", extractFormData(exchange));
            exchange.send(echoResponse);
        });
        post("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        put("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        post("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        put("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        delete("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        options("/echoPlain", exchange ->exchange.send(exchange.body().asString()), produces("txt"));
        head("/echoPlain", exchange -> exchange.status(200), produces("txt"));
        multipart("/echoMultipart", exchange -> {
            Map<String, Object> echoResponse = new HashMap<>();
            exchange.partNames().forEach(p -> echoResponse.put(p, exchange.part(p)));
            exchange.send(echoResponse);
        });


        get("/testData", exchange -> exchange.send(new TestData("yolo")));

        enableTracer();
        SnappyServer.start();
    }

    private static Map<String, Object> getRequestData(Exchange exchange) {
        Map<String, Object> echoResponse = new HashMap<>();
        echoResponse.put("headers", extractHeaders(exchange));
        echoResponse.put("queryParams", exchange.queryParameters());
        echoResponse.put("path", exchange.path());
        return echoResponse;
    }

    private static Map<String, Object> extractHeaders(Exchange exchange) {
        Map<String, Object> headers = new HashMap<>();
        for (HttpString name : exchange.headers().getHeaderNames()) {
            headers.put(name.toString(), exchange.header(name.toString()));
        }
        return headers;
    }

    private static Map<String, Object> extractFormData(MultipartExchange exchange) {
        Map<String, Object> formData = new HashMap<>();
        for (String name : exchange.partNames()) {
            formData.put(name, exchange.part(name));
        }
        return formData;
    }

    public static void stop() {
        SnappyServer.stop();
    }

}
