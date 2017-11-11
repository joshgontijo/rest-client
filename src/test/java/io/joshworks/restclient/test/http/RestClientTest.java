/*
The MIT License

Copyright (c) 2013 Mashape (http://mashape.com)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.joshworks.restclient.test.http;

import io.joshworks.restclient.http.ClientBuilder;
import io.joshworks.restclient.http.ClientContainer;
import io.joshworks.restclient.http.Headers;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.JsonNode;
import io.joshworks.restclient.http.RestClient;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.exceptions.RestClientException;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequest;
import io.joshworks.restclient.test.helper.TestData;
import io.joshworks.restclient.test.helper.TestServer;
import io.joshworks.restclient.test.helper.TestUtils;
import net.jodah.failsafe.CircuitBreaker;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class RestClientTest {

    private boolean status;

    private RestClient client;

    private static final String BASE_URL = "http://localhost:9000";

    @Before
    public void setUp() {
        client = RestClient.builder().build();
        status = false;
    }

    @After
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeClass
    public static void startServer() {
        TestServer.start();
    }

    @AfterClass
    public static void shutdownContainer() {
        TestServer.stop();
        ClientContainer.shutdown();
    }

    private String findAvailableIpAddress() throws Exception {
        for (int i = 100; i <= 255; i++) {
            String ip = "192.168.1." + i;
            if (!InetAddress.getByName(ip).isReachable(1000)) {
                return ip;
            }
        }

        throw new RuntimeException("Couldn't find an available IP address in the range of 192.168.0.100-255");
    }


    @Test
    public void simpleGet() {
        HttpResponse<String> response = client.get(BASE_URL + "/hello").asString();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    public void simplePost() {
        String message = "hello";
        HttpResponse<String> response = client.post(BASE_URL + "/echoPlain").body(message).asString();
        assertEquals(200, response.getStatus());
        assertEquals(message, response.getBody());
    }

    @Test
    public void simplePut() {
        String message = "hello";
        HttpResponse<String> response = client.put(BASE_URL + "/echoPlain").body(message).asString();
        assertEquals(200, response.getStatus());
        assertEquals(message, response.getBody());
    }

    @Test
    public void simpleDelete() {
        String message = "hello";
        HttpResponse<String> response = client.delete(BASE_URL + "/echoPlain").body(message).asString();
        assertEquals(200, response.getStatus());
        assertEquals(message, response.getBody());
    }

    @Test
    public void simpleOptions() {
        String message = "hello";
        HttpResponse<String> response = client.options(BASE_URL + "/echoPlain").body(message).asString();
        assertEquals(200, response.getStatus());
        assertEquals(message, response.getBody());
    }

    @Test
    public void simpleHead() {
        HttpResponse<String> response = client.head(BASE_URL + "/echoPlain").asString();
        assertEquals(200, response.getStatus());
        assertNull(response.getBody());
    }

    @Test
    public void nullBody() {
        HttpResponse<String> response = client.head(BASE_URL + "/nullBody").asString();
        assertEquals(200, response.getStatus());
        assertNull(response.getBody());
    }

    @Test
    public void queryParamUTF8() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").queryString("param3", "こんにちは").asJson();
        assertEquals(response.getBody().getObject().getJSONObject("queryParams").getJSONArray("param3").get(0).toString(), "こんにちは");
    }

    @Test
    public void postUTF8() {
        String value = "こんにちは";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("param3", value)
                .asJson();

        assertEquals(value, response.getBody().getObject().getJSONObject("body").getString("param3"));
    }

    @Test
    public void postBinaryUTF8() throws Exception {
        String value = "こんにちは";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("param3", value)
                .field("file", new File(getClass().getResource("/test").toURI()))
                .asJson();

        assertEquals("This is a test file", response.getBody().getObject().getJSONObject("body").getJSONObject("file").getString("content"));
        assertEquals(value, response.getBody().getObject().getJSONObject("body").getString("param3"));
    }

    @Test
    public void postRawBody() {
        String sourceString = "'\"@こんにちは-test-123";
        byte[] sentBytes = sourceString.getBytes();

        HttpResponse<String> response = client.post(BASE_URL + "/echoBinary")
                .header("Content-type", "text/plain")
                .body(sentBytes)
                .asString();
        assertEquals(sourceString, response.getBody());
    }

    @Test
    public void redirect301() {
        HttpResponse<String> response = client.get(BASE_URL + "/redirect301").asString();
        assertEquals(200, response.getStatus());
        assertEquals("Hello", response.getBody());
    }

    @Test
    public void redirect302() {
        HttpResponse<String> response = client.get(BASE_URL + "/redirect302").asString();
        assertEquals(200, response.getStatus());
        assertEquals("Hello", response.getBody());
    }

    @Test
    public void redirect303() {
        HttpResponse<String> response = client.get(BASE_URL + "/redirect303").asString();
        assertEquals(200, response.getStatus());
        assertEquals("Hello", response.getBody());
    }

    @Test
    public void disableRedirect() {
        try (RestClient customClient = RestClient.builder()
                .followRedirect(false)
                .baseUrl(BASE_URL)
                .build()) {

            HttpResponse<JsonNode> postResponse = customClient.get("/redirect301")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .asJson();

            assertEquals(301, postResponse.getStatus());
        }
    }

    @Test
    public void customUserAgent() {
        String agent = "hello-world";
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").header("user-agent", agent).asJson();
        assertEquals(agent, response.getBody().getObject().getJSONObject("headers").getJSONArray("user-agent").get(0));

        GetRequest getRequest = client.get("http");
        for (Object current : Arrays.asList(0, 1, 2)) {
            getRequest.queryString("name", current);
        }
    }

    @Test
    public void getMultiple() {
        for (int i = 1; i <= 50; i++) {
            HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo?try=" + i).asJson();
            assertEquals(response.getBody().getObject().getJSONObject("queryParams").getJSONArray("try").get(0), ((Integer) i).toString());
        }
    }

    @Test
    public void getFields() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").queryString("name", "mark").queryString("nick", "thefosk").asJson();
        assertEquals(response.getBody().getObject().getJSONObject("queryParams").getJSONArray("name").get(0), "mark");
        assertEquals(response.getBody().getObject().getJSONObject("queryParams").getJSONArray("nick").get(0), "thefosk");
    }

    @Test
    public void getFieldsEmailFormat() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").queryString("email", "hello@hello.com").asJson();
        assertEquals("hello@hello.com", response.getBody().getObject().getJSONObject("queryParams").getJSONArray("email").get(0));
    }

    @Test
    public void queryStringEncoding() {
        String testKey = "email2=someKey&email";
        String testValue = "hello@hello.com";
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").queryString(testKey, testValue).asJson();
        assertEquals(testValue, response.getBody().getObject().getJSONObject("queryParams").getJSONArray(testKey).get(0));
    }

    @Test
    public void basicAuth() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo").basicAuth("user", "test").asJson();
        assertEquals("Basic dXNlcjp0ZXN0", response.getBody().getObject().getJSONObject("headers").getJSONArray("Authorization").get(0));
    }

    @Test
    public void asyncPost() throws Exception {
        Future<HttpResponse<JsonNode>> future = client.post(BASE_URL + "/echoMultipart")
                .header("accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("param1", "value1")
                .field("param2", "bye")
                .asJsonAsync();


        assertNotNull(future);
        HttpResponse<JsonNode> jsonResponse = future.get();

        assertTrue(jsonResponse.getHeaders().size() > 0);
        assertTrue(jsonResponse.getBody().toString().length() > 0);
        assertFalse(jsonResponse.getRawBody() == null);
        assertEquals(200, jsonResponse.getStatus());

        JsonNode json = jsonResponse.getBody();
        assertFalse(json.isArray());
        assertNotNull(json.getObject());
        assertNotNull(json.getArray());
        assertEquals(1, json.getArray().length());
        assertNotNull(json.getArray().get(0));
    }

    @Test
    public void asyncCallback() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        //FIXME
        client.post(BASE_URL + "/echoMultipart")
                .header("accept", "application/json")
                .field("param1", "value1")
                .field("param2", "bye")
                .asJsonAsync(new Callback<JsonNode>() {

                    @Override
                    public void failed(RestClientException e) {
                        fail();
                    }

                    @Override
                    public void completed(HttpResponse<JsonNode> jsonResponse) {
                        assertTrue(jsonResponse.getHeaders().size() > 0);
                        assertTrue(jsonResponse.getBody().toString().length() > 0);
                        assertFalse(jsonResponse.getRawBody() == null);
                        assertEquals(200, jsonResponse.getStatus());

                        JsonNode json = jsonResponse.getBody();
                        assertFalse(json.isArray());
                        assertNotNull(json.getObject());
                        assertNotNull(json.getArray());
                        assertEquals(1, json.getArray().length());
                        assertNotNull(json.getArray().get(0));

                        assertEquals("value1", jsonResponse.getBody().getObject().getJSONObject("body").getString("param1"));
                        assertEquals("bye", jsonResponse.getBody().getObject().getJSONObject("body").getString("param2"));

                        status = true;
                        lock.countDown();
                    }

                    @Override
                    public void cancelled() {
                        fail();
                    }
                });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void multipart() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", "Mark")
                .field("file", new File(getClass().getResource("/test").toURI()))
                .asJson();

        validateBasicResponseFields(response);

        JsonNode json = response.getBody();
        assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONObject("file").getString("content"));
        assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));
    }

    @Test
    @Ignore
    public void multipart_async_largeFile() throws Exception {
        long size = 1073741824; //1GB
        Future<HttpResponse<String>> response = client.post(BASE_URL + "/upload")
                .field("name", "Mark")
                .field("file", TestUtils.mockStream(size), "someFile.txt")
                .asStringAsync();

        HttpResponse<String> stringHttpResponse = response.get();
        assertEquals(200, stringHttpResponse.getStatus());
        assertEquals(size, Long.parseLong(stringHttpResponse.getBody()));
    }

    @Test
    public void multipartContentType() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", "Mark")
                .field("file", new File(getClass().getResource("/image.jpg").toURI()), "image/jpeg")
                .asJson();

        validateBasicResponseFields(response);

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONObject("file").getString("type").contains("image/jpeg"));
        assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));
    }


    @Test
    public void multipartInputStreamContentType() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", "Mark")
                .field("file", new FileInputStream(new File(getClass().getResource("/image.jpg").toURI())), ContentType.APPLICATION_OCTET_STREAM, "image.jpg")
                .asJson();

        validateBasicResponseFields(response);

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONObject("file").getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
        assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));
    }

    private void validateBasicResponseFields(HttpResponse<JsonNode> response) {
        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaders().size() > 0);
        assertTrue(response.getBody().toString().length() > 0);
        assertFalse(response.getRawBody() == null);

        JsonNode json = response.getBody();
        assertFalse(json.isArray());
        assertNotNull(json.getObject());
        assertNotNull(json.getArray());
        assertEquals(1, json.getArray().length());
        assertNotNull(json.getArray().get(0));
        assertNotNull(json.getObject().getJSONObject("body"));
        assertNotNull(json.getObject().getJSONObject("body").getJSONObject("file"));
    }

    @Test
    public void multipartInputStreamContentTypeAsync() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        client.post(BASE_URL + "/echoMultipart").field("name", "Mark").field("file", new FileInputStream(new File(getClass().getResource("/test").toURI())), ContentType.APPLICATION_OCTET_STREAM, "test").asJsonAsync(new Callback<JsonNode>() {

            public void failed(RestClientException e) {
                fail();
            }

            public void completed(HttpResponse<JsonNode> response) {
                validateBasicResponseFields(response);

                JsonNode json = response.getBody();
                assertTrue(json.getObject().getJSONObject("body").getJSONObject("file").getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
                assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));

                status = true;
                lock.countDown();
            }

            public void cancelled() {
                fail();
            }

        });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void testMultipartByteContentType() throws Exception {
        final InputStream stream = new FileInputStream(new File(getClass().getResource("/image.jpg").toURI()));
        final byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        stream.close();
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart").field("name", "Mark").field("file", bytes, "image.jpg").asJson();

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONObject("file").getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
        assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));
    }

    @Test
    public void multipartByteContentTypeAsync() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        final InputStream stream = new FileInputStream(new File(getClass().getResource("/test").toURI()));
        final byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        stream.close();
        client.post(BASE_URL + "/echoMultipart").field("name", "Mark").field("file", bytes, "test").asJsonAsync(new Callback<JsonNode>() {

            public void failed(RestClientException e) {
                fail();
            }

            public void completed(HttpResponse<JsonNode> response) {
                validateBasicResponseFields(response);

                JsonNode json = response.getBody();
                assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONObject("file").getString("content"));
                assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));

                status = true;
                lock.countDown();
            }

            public void cancelled() {
                fail();
            }

        });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void asyncCompletableFuture() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        client.get(BASE_URL + "/hello")
                .asStringAsync()
                .thenAccept(resp -> {
                    assertEquals("Hello", resp.getBody());
                    latch.countDown();
                });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail();
        }
    }

    @Test
    public void multipartAsync() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        client.post(BASE_URL + "/echoMultipart").field("name", "Mark").field("file", new File(getClass().getResource("/test").toURI())).asJsonAsync(new Callback<JsonNode>() {

            public void failed(RestClientException e) {
                fail();
            }

            public void completed(HttpResponse<JsonNode> response) {
                JsonNode json = response.getBody();
                assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONObject("file").getString("content"));
                assertEquals("Mark", json.getObject().getJSONObject("body").getString("name"));

                status = true;
                lock.countDown();
            }

            public void cancelled() {
                fail();
            }

        });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void gzip() {
        HttpResponse<JsonNode> jsonResponse = client.get("http://httpbin.org/gzip").asJson();
        assertTrue(jsonResponse.getHeaders().size() > 0);
        assertTrue(jsonResponse.getBody().toString().length() > 0);
        assertFalse(jsonResponse.getRawBody() == null);
        assertEquals(200, jsonResponse.getStatus());

        JsonNode json = jsonResponse.getBody();
        assertFalse(json.isArray());
        assertTrue(json.getObject().getBoolean("gzipped"));
    }

    @Test
    public void gzipAsync() throws Exception {
        HttpResponse<JsonNode> jsonResponse = client.get("http://httpbin.org/gzip").asJsonAsync().get();
        assertTrue(jsonResponse.getHeaders().size() > 0);
        assertTrue(jsonResponse.getBody().toString().length() > 0);
        assertFalse(jsonResponse.getRawBody() == null);
        assertEquals(200, jsonResponse.getStatus());

        JsonNode json = jsonResponse.getBody();
        assertFalse(json.isArray());
        assertTrue(json.getObject().getBoolean("gzipped"));
    }

    @Test
    public void testDefaultHeaders() {
        RestClient customClient = null;
        try {
            customClient = RestClient.builder()
                    .defaultHeader("X-Custom-Header", "hello")
                    .defaultHeader("user-agent", "foobar")
                    .build();

            HttpResponse<JsonNode> jsonResponse = customClient.get("http://httpbin.org/headers").asJson();
            assertTrue(jsonResponse.getHeaders().size() > 0);
            assertTrue(jsonResponse.getBody().toString().length() > 0);
            assertFalse(jsonResponse.getRawBody() == null);
            assertEquals(200, jsonResponse.getStatus());

            JsonNode json = jsonResponse.getBody();
            assertFalse(json.isArray());
            assertTrue(jsonResponse.getBody().getObject().getJSONObject("headers").has("X-Custom-Header"));
            assertEquals("hello", json.getObject().getJSONObject("headers").getString("X-Custom-Header"));
            assertTrue(jsonResponse.getBody().getObject().getJSONObject("headers").has("User-Agent"));
            assertEquals("foobar", json.getObject().getJSONObject("headers").getString("User-Agent"));

            jsonResponse = customClient.get("http://httpbin.org/headers").asJson();
            assertTrue(jsonResponse.getBody().getObject().getJSONObject("headers").has("X-Custom-Header"));
            assertEquals("hello", jsonResponse.getBody().getObject().getJSONObject("headers").getString("X-Custom-Header"));
        } finally {
            if (customClient != null) {
                customClient.close();
            }
        }

    }

    @Test
    public void testSetTimeouts() throws Exception {
        RestClient customClient = null;
        try {
            int timeout = 3000;
            customClient = RestClient.builder().timeouts(timeout, 10000).build();
            String address = "http://" + findAvailableIpAddress() + "/";
            long start = System.currentTimeMillis();
            try {
                client.get("http://" + address + "/").asString();
            } catch (Exception e) {
                long diff = System.currentTimeMillis() - start;
                if (diff >= timeout) { // Add 100ms for code execution
                    fail("Expected timeout of less than " + timeout + ", got " + diff);
                }
            }

        } finally {
            if (customClient != null) {
                customClient.close();
            }
        }
    }

    @Test
    public void testPathParameters() {
        HttpResponse<JsonNode> jsonResponse = client.get("http://httpbin.org/{method}").routeParam("method", "get").queryString("name", "Mark").asJson();

        assertEquals(200, jsonResponse.getStatus());
        assertEquals(jsonResponse.getBody().getObject().getJSONObject("args").getString("name"), "Mark");
    }

    @Test
    public void testQueryAndBodyParameters() {
        HttpResponse<JsonNode> jsonResponse = client.post("http://httpbin.org/{method}").routeParam("method", "post").queryString("name", "Mark").field("wot", "wat").asJson();

        assertEquals(200, jsonResponse.getStatus());
        assertEquals(jsonResponse.getBody().getObject().getJSONObject("args").getString("name"), "Mark");
        assertEquals(jsonResponse.getBody().getObject().getJSONObject("form").getString("wot"), "wat");
    }

    @Test
    public void testPathParameters2() {
        HttpResponse<JsonNode> jsonResponse = client.patch("http://httpbin.org/{method}").routeParam("method", "patch").field("name", "Mark").asJson();

        assertEquals(200, jsonResponse.getStatus());
        assertEquals("OK", jsonResponse.getStatusText());
        assertEquals(jsonResponse.getBody().getObject().getJSONObject("form").getString("name"), "Mark");
    }

    @Test(expected = RuntimeException.class)
    public void testMissingPathParameter() {
        client.get("http://httpbin.org/{method}").routeParam("method222", "get").queryString("name", "Mark").asJson();
        fail("Exception was expected");
    }

//    @Test
//    public void parallelTest()  Exception {
//        RestClient firstClient = null;
//        RestClient secondClient = null;
//        try {
//            firstClient = RestClient.builder().concurrency(10).build();
//
//            long start = System.currentTimeMillis();
//            makeParallelRequests(firstClient);
//            long smallerConcurrencyTime = (System.currentTimeMillis() - start);
//
//            secondClient = RestClient.builder().concurrency(20).build();
//            start = System.currentTimeMillis();
//            makeParallelRequests(secondClient);
//            long higherConcurrencyTime = (System.currentTimeMillis() - start);
//
//            assertTrue(higherConcurrencyTime < smallerConcurrencyTime);
//        } finally {
//            firstClient.close();
//            secondClient.close();
//        }
//
//    }

    private void makeParallelRequests(final RestClient restClient) throws Exception {
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(10);
        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 200; i++) {
            newFixedThreadPool.execute(() -> {
                restClient.get("http://httpbin.org/get").queryString("index", counter.incrementAndGet()).asJson();
            });
        }

        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(10, TimeUnit.MINUTES);
    }

    @Test
    public void testAsyncCustomContentType() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        client.post("http://httpbin.org/post").header("accept", "application/json").header("Content-Type", "application/json").body("{\"hello\":\"world\"}").asJsonAsync(new Callback<JsonNode>() {

            public void failed(RestClientException e) {
                fail();
            }

            public void completed(HttpResponse<JsonNode> jsonResponse) {
                JsonNode json = jsonResponse.getBody();
                assertEquals("{\"hello\":\"world\"}", json.getObject().getString("data"));
                assertEquals("application/json", json.getObject().getJSONObject("headers").getString("Content-Type"));

                status = true;
                lock.countDown();
            }

            public void cancelled() {
                fail();
            }
        });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void testAsyncCustomContentTypeAndFormParams() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        client.post("http://httpbin.org/post").header("accept", "application/json").header("Content-Type", "application/x-www-form-urlencoded").field("name", "Mark").field("hello", "world").asJsonAsync(new Callback<JsonNode>() {

            public void failed(RestClientException e) {
                fail();
            }

            public void completed(HttpResponse<JsonNode> jsonResponse) {
                JsonNode json = jsonResponse.getBody();
                assertEquals("Mark", json.getObject().getJSONObject("form").getString("name"));
                assertEquals("world", json.getObject().getJSONObject("form").getString("hello"));

                assertEquals("application/x-www-form-urlencoded", json.getObject().getJSONObject("headers").getString("Content-Type"));

                status = true;
                lock.countDown();
            }

            public void cancelled() {
                fail();
            }
        });

        lock.await(10, TimeUnit.SECONDS);
        assertTrue(status);
    }

    @Test
    public void testGetQuerystringArray() {
        HttpResponse<JsonNode> response = client.get("http://httpbin.org/get").queryString("name", "Mark").queryString("name", "Tom").asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("args").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void testPostMultipleFiles() throws Exception {
        HttpResponse<JsonNode> response = client.post("http://httpbin.org/post").field("param3", "wot").field("file1", new File(getClass().getResource("/test").toURI())).field("file2", new File(getClass().getResource("/test").toURI())).asJson();

        JSONObject names = response.getBody().getObject().getJSONObject("files");
        assertEquals(2, names.length());

        assertEquals("This is a test file", names.getString("file1"));
        assertEquals("This is a test file", names.getString("file2"));

        assertEquals("wot", response.getBody().getObject().getJSONObject("form").getString("param3"));
    }

    @Test
    public void testGetArray() {
        HttpResponse<JsonNode> response = client.get("http://httpbin.org/get").queryString("name", Arrays.asList("Mark", "Tom")).asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("args").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void testPostArray() {
        HttpResponse<JsonNode> response = client.post("http://httpbin.org/post").field("name", "Mark").field("name", "Tom").asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("form").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void testPostCollection() {
        HttpResponse<JsonNode> response = client.post("http://httpbin.org/post").field("name", Arrays.asList("Mark", "Tom")).asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("form").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void testCaseInsensitiveHeaders() {
        GetRequest request = client.get("http://httpbin.org/headers").header("Name", "Marco");
        assertEquals(1, request.getHeaders().size());
        assertEquals("Marco", request.getHeaders().get("name").get(0));
        assertEquals("Marco", request.getHeaders().get("NAme").get(0));
        assertEquals("Marco", request.getHeaders().get("Name").get(0));
        JSONObject headers = request.asJson().getBody().getObject().getJSONObject("headers");
        assertEquals("Marco", headers.getString("Name"));

    }

//    @Test
//    public void multipleHeaders() {
//        GetRequest request = client.get("http://localhost:9000").header("Name", "Marco").header("Name", "John");
//        assertEquals(1, request.getHeaders().size());
//        assertEquals("Marco", request.getHeaders().get("name").get(0));
//        assertEquals("John", request.getHeaders().get("name").get(1));
//        assertEquals("Marco", request.getHeaders().get("NAme").get(0));
//        assertEquals("John", request.getHeaders().get("NAme").get(1));
//        assertEquals("Marco", request.getHeaders().get("Name").get(0));
//        assertEquals("John", request.getHeaders().get("Name").get(1));
//
//        JSONObject headers = request.asJson().getBody().getObject().getJSONObject("headers");
//        assertEquals("Marco,John", headers.get("Name"));
//    }


    @Test
    public void multipleClients() throws Exception {
        RestClient client1 = RestClient.builder().build();
        int status = client1.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);

        RestClient client2 = RestClient.builder().build();
        status = client2.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);

        RestClient client3 = RestClient.builder().build();
        status = client3.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);

        client1.close();
        client2.close();
        client3.close();
    }

    @Test
    public void multipleClients_sameBuilder() throws Exception {
        ClientBuilder builder = RestClient.builder();
        RestClient client1 = builder.build();
        int status = client1.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);
        client1.close();

        RestClient client2 = builder.build();
        status = client2.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);
        client2.close();

        RestClient client3 = builder.build();
        status = client3.get("http://httpbin.org/get").asString().getStatus();
        assertEquals(200, status);
        client3.close();
    }

    //FIXME
//    @Test
//    public void retry()  , IOException {
//        RestClient retryClient = RestClient.builder()
//                .retryPolicy(new RetryPolicy().withMaxRetries(2))
//                .build();
//        int status = retryClient.get("http://dummy-url.abc").asString().getStatus();
//        assertEquals(200, status);
//    }

    @Test
    public void fallbackResponse() throws Exception {
        String fallback = "FALLBACK-DATA";
        RestClient retryClient = RestClient.builder().build();

        HttpResponse<String> fallbackResponse = retryClient.get("http://localhost:1234/invalid-endpoint")
                .withFallback(fallback)
                .asString();

        assertEquals(fallback, fallbackResponse.getBody());
    }

//
//    @Test
//    public void setTimeoutsAndCustomClient() {
//        try {
//            client.setTimeouts(1000, 2000);
//        } catch (Exception e) {
//            fail();
//        }
//
//        try {
//            client.setAsyncHttpClient(HttpAsyncClientBuilder.create().build());
//        } catch (Exception e) {
//            fail();
//        }
//
//        try {
//            client.setAsyncHttpClient(HttpAsyncClientBuilder.create().build());
//            client.setTimeouts(1000, 2000);
//            fail();
//        } catch (Exception e) {
//            // Ok
//        }
//
//        try {
//            client.setHttpClient(HttpClientBuilder.create().build());
//            client.setTimeouts(1000, 2000);
//            fail();
//        } catch (Exception e) {
//            // Ok
//        }
//    }

    @Test
    public void defaultObjectMapper() throws Exception {
        TestData testData = new TestData("yolo");
        HttpResponse<TestData> getResponse = client.post(BASE_URL + "/echoJson")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body(testData).asObject(TestData.class);

        assertEquals(200, getResponse.getStatus());
        assertNotNull(getResponse.getBody());
        assertEquals(testData, getResponse.getBody());
    }


    @Test
    public void testPostProvidesSortedParams() throws Exception {
        // Verify that fields are encoded into the body in sorted order.
        HttpRequest httpRequest = client.post("test").field("z", "Z").field("y", "Y").field("x", "X").getHttpRequest();

        InputStream content = httpRequest.getBody().getEntity().getContent();
        String body = IOUtils.toString(content, "UTF-8");
        assertEquals("x=X&y=Y&z=Z", body);
    }

    @Test
    public void testHeaderNamesCaseSensitive() {
        // Verify that header names are the same as server (case sensitive)
        final Headers headers = new Headers();
        headers.put("Content-Type", Arrays.asList("application/json"));

        assertEquals("Only header \"Content-Type\" should exist", null, headers.getFirst("cOnTeNt-TyPe"));
        assertEquals("Only header \"Content-Type\" should exist", null, headers.getFirst("content-type"));
        assertEquals("Only header \"Content-Type\" should exist", "application/json", headers.getFirst("Content-Type"));
    }

    @Test
    public void circuitBreaker() {
        RestClient customClient = null;
        try {
            CircuitBreaker circuitBreaker = new CircuitBreaker()
                    .withFailureThreshold(1);

            customClient = RestClient.builder()
                    .baseUrl("http://localhost:1234")
                    .circuitBreaker(circuitBreaker)
                    .build();


            assertTrue(circuitBreaker.isClosed());
            try {
                customClient.get("/invalid-endpoint").asJson();
                customClient.get("/invalid-endpoint").asJson();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
            assertTrue(circuitBreaker.isOpen());


        } finally {
            customClient.close();
        }
    }

    @Test
    public void urlTransformer() {
        RestClient client = RestClient.builder()
                .baseUrl("http://invalid-url.abc")
                .urlTransformer((url) -> "http://httpbin.org")
                .build();

        HttpResponse<String> response = client.get("/get").asString();
        assertEquals(200, response.getStatus());
    }

    //TODO add to all other methods
    @Test
    public void baseUrl() {
        try (RestClient customClient = RestClient.builder()
                .baseUrl("http://httpbin.org")
                .build()) {
            HttpResponse<JsonNode> postResponse = customClient.get("/get")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .asJson();

            assertEquals(200, postResponse.getStatus());
        }
    }

}
