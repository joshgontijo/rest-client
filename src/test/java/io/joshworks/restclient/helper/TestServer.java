package io.joshworks.restclient.helper;

import io.joshworks.snappy.Exchange;
import io.joshworks.snappy.SnappyServer;
import io.joshworks.snappy.http.MediaType;
import io.joshworks.snappy.http.multipart.MultipartExchange;
import io.joshworks.snappy.http.multipart.Part;
import io.joshworks.snappy.parser.JsonParser;
import io.joshworks.snappy.parser.Parsers;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.joshworks.snappy.SnappyServer.delete;
import static io.joshworks.snappy.SnappyServer.enableTracer;
import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.head;
import static io.joshworks.snappy.SnappyServer.multipart;
import static io.joshworks.snappy.SnappyServer.options;
import static io.joshworks.snappy.SnappyServer.post;
import static io.joshworks.snappy.SnappyServer.put;
import static io.joshworks.snappy.parser.MediaTypes.consumes;
import static io.joshworks.snappy.parser.MediaTypes.produces;

/**
 * Created by Josh Gontijo on 5/10/17.
 */
public class TestServer {

    public static final String contentType = "application/custom-type";

    public static void start() {

        //see RestclientTest#customObjectMapper
        Parsers.register(MediaType.valueOf(contentType), new JsonParser());

        get("/hello", exchange -> exchange.send("Hello"), produces("txt"));
        head("/nullBody", Exchange::end, produces("txt"));
        get("/echo/{name}", exchange -> exchange.send(exchange.pathParameter("name"), "txt"));
        get("/get", exchange -> exchange.status(200).end());
        get("/500", exchange -> exchange.status(500));


        //redirect
        get("/redirect300", exchange -> exchange.status(300).header("Location", "http://localhost:9000/hello"));
        get("/redirect301", exchange -> exchange.status(301).header("Location", "http://localhost:9000/hello"));
        get("/redirect302", exchange -> exchange.status(302).header("Location", "http://localhost:9000/hello"));
        get("/redirect303", exchange -> exchange.status(303).header("Location", "http://localhost:9000/hello"));

        //echo
        get("/echo", exchange -> exchange.send(getRequestData(exchange)));
        post("/echo", exchange -> exchange.send(getRequestData(exchange)));
        post("/echoCustomType", exchange -> exchange.send(exchange.body().asObject(TestData.class)), consumes(contentType), produces(contentType));
        multipart("/echoMultipart", exchange -> {
            Map<String, Object> echoResponse = getRequestData(exchange);
            echoResponse.put("body", extractFormData(exchange));
            exchange.send(echoResponse, "json");
        });
        multipart("/echoMultipart/{pathParam}", exchange -> {
            Map<String, Object> echoResponse = getRequestData(exchange);
            echoResponse.put("body", extractFormData(exchange));
            exchange.send(echoResponse, "json");
        });

        post("/echo", exchange -> {
            Map<String, Object> echoResponse = getRequestData(exchange);
            echoResponse.put("body", exchange.body().asMap());
            exchange.send(echoResponse, "json");
        });

        post("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        put("/echoJson", exchange -> exchange.send(exchange.body().asObject(TestData.class)));
        post("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        post("/echoHeaders", exchange -> exchange.send(extractHeaders(exchange)), produces("json"));
        get("/echoHeaders", exchange -> exchange.send(extractHeaders(exchange)), produces("json"));
        put("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        delete("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        options("/echoPlain", exchange -> exchange.send(exchange.body().asString()), produces("txt"));
        head("/echoPlain", exchange -> exchange.status(200), produces("txt"));
        post("/echoBinary", exchange -> exchange.send(TestUtils.toString(exchange.body().asBinary()), "txt"), consumes("txt"));


        get("/testData", exchange -> exchange.send(new TestData("yolo")));
        get("/gzip", exchange -> exchange.send(new TestData("yolo")));

        get("/hang", exchange -> TimeUnit.MINUTES.sleep(1));

        get("/set-cookie", exchange -> {
            for (Map.Entry<String, Deque<String>> entry : exchange.queryParameters().entrySet()) {
                exchange.cookie(new CookieImpl(entry.getKey(), entry.getValue().getFirst()));
            }
            exchange.end();
        });

        enableTracer();
        SnappyServer.start();
    }

    private static Map<String, Object> getRequestData(Exchange exchange) {
        Map<String, Object> echoResponse = new HashMap<>();
        echoResponse.put("headers", extractHeaders(exchange));
        echoResponse.put("queryParams", exchange.queryParameters());
        echoResponse.put("path", exchange.pathParameters());
        return echoResponse;
    }

    private static Map<String, Object> extractHeaders(Exchange exchange) {
        Map<String, Object> headers = new HashMap<>();
        for (HttpString name : exchange.headers().getHeaderNames()) {
            headers.put(name.toString(), exchange.header(name.toString()));
        }
        return headers;
    }

    private static Map<String, List<Object>> extractFormData(MultipartExchange exchange) {
        Map<String, List<Object>> formData = new HashMap<>();
        for (String name : exchange.partNames()) {
            if (!formData.containsKey(name)) {
                formData.put(name, new ArrayList<>());
            }

            for (Part part : exchange.parts(name)) {

                if (part.isFile()) {
                    Map<String, Object> fileProps = new HashMap<>();
                    fileProps.put("name", part.file().name());
                    fileProps.put("content", readFileContent(part));
                    fileProps.put("type", part.type().toString());

                    formData.get(name).add(fileProps);
                } else {
                    formData.get(name).add(part.value());
                }

            }
        }
        return formData;
    }

    //For text files only
    private static String readFileContent(Part part) {
        try {
            return Files.readAllLines(part.file().path()).stream().collect(Collectors.joining());
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    public static void stop() {
        SnappyServer.stop();
    }

}
