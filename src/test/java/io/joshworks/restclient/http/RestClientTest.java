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

package io.joshworks.restclient.http;

import io.joshworks.restclient.helper.TestData;
import io.joshworks.restclient.helper.TestServer;
import io.joshworks.restclient.helper.TestUtils;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.mapper.JsonMapper;
import io.joshworks.restclient.http.mapper.ObjectMappers;
import io.joshworks.restclient.http.utils.ClientStats;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

        assertEquals(value, response.getBody().getObject().getJSONObject("body").getJSONArray("param3").getString(0));
    }

    @Test
    public void postBinaryUTF8() throws Exception {
        String value = "こんにちは";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("param3", value, "text/plain; charset=UTF-8")
                .part("file", new File(getClass().getResource("/test").toURI()))
                .asJson();

        assertEquals("This is a test file", response.getBody().getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("content"));
        assertEquals(value, response.getBody().getObject().getJSONObject("body").getJSONArray("param3").getString(0));
    }

    @Test
    public void partWithType() throws Exception {
        String type = "text/plain; charset=UTF-8";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("file", new File(getClass().getResource("/test").toURI()), type)
                .asJson();

        assertEquals(type, response.getBody().getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type"));
        assertEquals("This is a test file", response.getBody().getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("content"));
    }

    @Test
    public void postRawBody() {
        String sourceString = "'\"@こんにちは-test-123";
        byte[] sentBytes = sourceString.getBytes(StandardCharsets.UTF_8);

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
            assertEquals(response.getBody().getObject().getJSONObject("queryParams").getJSONArray("try").get(0), String.valueOf(i));
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
        client.post(BASE_URL + "/echoMultipart")
                .header("accept", "application/json")
                .field("param1", "value1")
                .field("param2", "bye")
                .asJsonAsync(new Callback<JsonNode>() {

                    @Override
                    public void failed(Exception e) {
                        e.printStackTrace();
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

                        assertEquals("value1", jsonResponse.getBody().getObject().getJSONObject("body").getJSONArray("param1").getString(0));
                        assertEquals("bye", jsonResponse.getBody().getObject().getJSONObject("body").getJSONArray("param2").getString(0));

                        status = true;
                        lock.countDown();
                    }

                    @Override
                    public void cancelled() {
                        fail();
                    }
                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
        assertTrue(status);
    }

    @Test
    public void multipart() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", new File(getClass().getResource("/test").toURI()))
                .asJson();

        validateBasicFormFields(response);

        JsonNode json = response.getBody();
        assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("content"));
        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
    }

    @Test
    public void multipart_contentType_withPreviousValue() {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .part("name", "Mark")
                .asJson();

        assertEquals(415, response.getStatus());
    }

    @Test
    public void formData_contentType() {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", "Mark")
                .asJson();

        JSONArray contentTypeValues = response.getBody().getObject().getJSONObject("headers").getJSONArray(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, contentTypeValues.length());
        assertEquals(ContentType.APPLICATION_FORM_URLENCODED.withCharset(Charsets.UTF_8).toString(), contentTypeValues.getString(0));
    }

    @Test
    public void multipartContentType() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", new File(getClass().getResource("/test").toURI()), "text/plain")
                .asJson();

        validateBasicFormFields(response);

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type").contains("text/plain"));
        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
    }

    @Test
    public void multipartContentType_withDefaultHeader() throws URISyntaxException {
        try (RestClient formClient = RestClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON).build()) {
            HttpResponse<JsonNode> response = formClient.post(BASE_URL + "/echoMultipart")
                    .part("name", "Mark")
                    .part("file", new File(getClass().getResource("/test").toURI()), "text/plain")
                    .asJson();

            validateBasicFormFields(response);

            JsonNode json = response.getBody();
            JSONObject headers = json.getObject().getJSONObject("headers");
            assertTrue(headers.getJSONArray(HttpHeaders.CONTENT_TYPE).get(0).toString().contains(MediaType.MULTIPART_FORM_DATA));
            assertTrue(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type").contains("text/plain"));
            assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
        }
    }

    @Test
    public void urlFormEncoded_withDefaultHeader() {
        try (RestClient formClient = RestClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON).build()) {
            HttpResponse<JsonNode> response = formClient.post(BASE_URL + "/echoMultipart")
                    .field("name", "Mark")
                    .asJson();

            JsonNode json = response.getBody();
            JSONObject headers = json.getObject().getJSONObject("headers");
            assertTrue(headers.getJSONArray(HttpHeaders.CONTENT_TYPE).get(0).toString().contains(MediaType.APPLICATION_FORM_URLENCODED));
            assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
        }
    }


    @Test
    public void multipartInputStreamContentType() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", new FileInputStream(new File(getClass().getResource("/test").toURI())), "test.txt")
                .asJson();

        validateBasicFormFields(response);

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
    }

    private void validateBasicFormFields(HttpResponse<JsonNode> response) {
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
        assertNotNull(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0));
    }

    @Test
    public void multipartInputStreamAsync_withoutContentType() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);
        client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", new FileInputStream(new File(getClass().getResource("/test").toURI())), "test")
                .asJsonAsync(new Callback<JsonNode>() {

                    public void failed(Exception e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }

                    public void completed(HttpResponse<JsonNode> response) {
                        validateBasicFormFields(response);

                        JsonNode json = response.getBody();
                        assertTrue(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
                        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").get(0));

                        status = true;
                        lock.countDown();
                    }

                    public void cancelled() {
                        fail();
                    }

                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
        assertTrue(status);
    }

    @Test
    public void testMultipartByteContentType() throws Exception {
        final byte[] bytes = Files.readAllBytes(Paths.get(this.getClass().getResource("/test").toURI()));
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", bytes, "test.txt")
                .asJson();

        JsonNode json = response.getBody();
        assertTrue(json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("type").contains(ContentType.APPLICATION_OCTET_STREAM.getMimeType()));
        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
    }

    @Test
    public void multipartByteContentTypeAsync() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        final InputStream stream = new FileInputStream(new File(getClass().getResource("/test").toURI()));
        final byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        stream.close();
        client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", bytes, "test")
                .asJsonAsync(new Callback<JsonNode>() {

                    public void failed(Exception e) {
                        e.printStackTrace();
                        fail();
                    }

                    public void completed(HttpResponse<JsonNode> response) {
                        validateBasicFormFields(response);

                        JsonNode json = response.getBody();
                        assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("content"));
                        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));

                        status = true;
                        lock.countDown();
                    }

                    public void cancelled() {
                        fail();
                    }

                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
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

        client.post(BASE_URL + "/echoMultipart")
                .part("name", "Mark")
                .part("file", new File(getClass().getResource("/test").toURI()))
                .asJsonAsync(new Callback<JsonNode>() {

                    public void failed(Exception e) {
                        e.printStackTrace();
                        fail();
                    }

                    public void completed(HttpResponse<JsonNode> response) {
                        JsonNode json = response.getBody();
                        assertEquals("This is a test file", json.getObject().getJSONObject("body").getJSONArray("file").getJSONObject(0).getString("content"));
                        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));

                        status = true;
                        lock.countDown();
                    }

                    public void cancelled() {
                        fail();
                    }

                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
        assertTrue(status);
    }

    @Test
    public void contentTypeSimple() {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoHeaders")
                .contentType("json")
                .body("{\"field\": \"value\"}")
                .asJson();

        assertTrue(response.getHeaders().size() > 0);
        assertFalse(response.getRawBody() == null);
        assertEquals(200, response.getStatus());

        JSONArray contentTypes = response.getBody().getObject().getJSONArray("Content-Type");
        assertEquals(1, contentTypes.length());
        assertEquals("application/json", contentTypes.get(0));
    }

    @Test
    public void gzip() {
        String value = "return this as GZIP";
        HttpResponse<String> response = client.post(BASE_URL + "/echoPlain")
                .body(value)
                .asString();

        assertTrue(response.getHeaders().size() > 0);
        assertFalse(response.getRawBody() == null);
        assertEquals(200, response.getStatus());

        assertEquals(value, response.getBody());
    }

    @Test
    public void gzipAsync() throws Exception {
        String value = "return this as GZIP";
        HttpResponse<String> response = client.post(BASE_URL + "/echoPlain")
                .body(value)
                .asStringAsync()
                .get();

        assertTrue(response.getHeaders().size() > 0);
        assertFalse(response.getRawBody() == null);
        assertEquals(200, response.getStatus());

        assertEquals(value, response.getBody());
    }

    @Test
    public void defaultHeaders() {
        String userAgentVal = "foobar";
        String customHeaderVal = "hello";
        try (RestClient customClient = RestClient.builder()
                .defaultHeader("X-Custom-Header", customHeaderVal)
                .defaultHeader("User-Agent", userAgentVal)
                .build()) {


            HttpResponse<JsonNode> jsonResponse = customClient.post(BASE_URL + "/echoHeaders").asJson();
            assertTrue(jsonResponse.getHeaders().size() > 0);
            assertTrue(jsonResponse.getBody().toString().length() > 0);
            assertFalse(jsonResponse.getRawBody() == null);
            assertEquals(200, jsonResponse.getStatus());

            JsonNode json = jsonResponse.getBody();
            assertFalse(json.isArray());
            assertTrue(jsonResponse.getBody().getObject().has("X-Custom-Header"));
            assertEquals(customHeaderVal, json.getObject().getJSONArray("X-Custom-Header").get(0));
            assertTrue(jsonResponse.getBody().getObject().has("User-Agent"));
            assertEquals(userAgentVal, json.getObject().getJSONArray("User-Agent").get(0));
        }
    }

    @Test
    public void connectionTimeout() throws Exception {
        RestClient customClient = null;
        try {
            int connectionTimeout = 3000;
            customClient = RestClient.builder().timeout(connectionTimeout, -1).build();
            String address = "http://" + findAvailableIpAddress() + "/";
            long start = System.currentTimeMillis();
            try {
                client.get("http://" + address + "/").asString();
            } catch (Exception e) {
                long diff = System.currentTimeMillis() - start;
                if (diff >= connectionTimeout) { // Add 100ms for code execution
                    fail("Expected connectionTimeout of less than " + connectionTimeout + ", got " + diff);
                }
            }

        } finally {
            if (customClient != null) {
                customClient.close();
            }
        }
    }

    @Test
    public void socketTimeout() {
        RestClient customClient = null;
        try {
            int socketTimeout = 3000;
            customClient = RestClient.builder().timeout(-1, socketTimeout).build();
            long start = System.currentTimeMillis();
            try {
                customClient.get(BASE_URL + "/hang").asString();
            } catch (Exception e) {
                long diff = System.currentTimeMillis() - start;
                if (diff >= socketTimeout * 2) { // Add some fat to the code completion
                    fail("Expected socketTimeout of less than " + socketTimeout + ", got " + diff);
                }
            }

        } finally {
            TestServer.stop();
            TestServer.start();

            if (customClient != null) {
                customClient.close();
            }
        }
    }

    @Test
    public void pathParameters() {
        String value = "Josh";
        HttpResponse<String> response = client.get(BASE_URL + "/echo/{name}")
                .routeParam("name", value)
                .asString();

        assertEquals(200, response.getStatus());
        assertEquals(value, response.getBody());
    }

    @Test
    public void pathParametersEncodedValue() {
        String value = "A%24A%26A%25";
        HttpResponse<String> response = client.get(BASE_URL + "/echo/{name}")
                .routeParam("name", value)
                .asString();

        assertEquals(200, response.getStatus());
        assertEquals(value, response.getBody());
    }

    @Test
    public void pathParametersEncodeValue() {
        String value = "A%A&A$";
        HttpResponse<String> response = client.get(BASE_URL + "/echo/{name}")
                .routeParam("name", value)
                .asString();

        assertEquals(200, response.getStatus());
        assertEquals(value, response.getBody());
    }

    @Test
    public void pathParametersEncodeValueWithQuery() {
        String pathValue = "A%A&A$";
        String queryValue = "someValue";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart/{pathParam}")
                .routeParam("pathParam", pathValue)
                .queryString("q", queryValue)
                .asJson();

        assertEquals(200, response.getStatus());
        assertEquals(pathValue, response.getBody().getObject().getJSONObject("path").getString("pathParam"));
        assertEquals(queryValue, response.getBody().getObject().getJSONObject("queryParams").getJSONArray("q").get(0));
    }

    @Test
    public void testQueryAndBodyParameters() {
        String pathParamVal = "Josh";
        String queryVal = "QueryVal";
        String formParamVal = "FormVal";
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart/{pathParam}")
                .queryString("q", queryVal)
                .routeParam("pathParam", pathParamVal)
                .field("formParam", formParamVal)
                .asJson();

        assertEquals(200, response.getStatus());
        assertEquals(queryVal, response.getBody().getObject().getJSONObject("queryParams").getJSONArray("q").get(0));
        assertEquals(pathParamVal, response.getBody().getObject().getJSONObject("path").getString("pathParam"));
        assertEquals(formParamVal, response.getBody().getObject().getJSONObject("body").getJSONArray("formParam").getString(0));
    }


    @Test(expected = RuntimeException.class)
    public void testMissingPathParameter() {
        client.get(BASE_URL + "/echo/{name}")
                .routeParam("someOtherKey", "get")
                .queryString("q", "Mark")
                .asJson();
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

        client.post(BASE_URL + "/echo")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body("{\"hello\":\"world\"}")
                .asJsonAsync(new Callback<JsonNode>() {

                    public void failed(Exception e) {
                        fail();
                    }

                    public void completed(HttpResponse<JsonNode> response) {
                        JsonNode json = response.getBody();
                        assertEquals("world", json.getObject().getJSONObject("body").getString("hello"));
                        assertEquals("application/json", json.getObject().getJSONObject("headers").getJSONArray("Content-Type").get(0));

                        status = true;
                        lock.countDown();
                    }

                    public void cancelled() {
                        fail();
                    }
                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
        assertTrue(status);
    }

    @Test
    public void testAsyncCustomContentTypeAndFormParams() throws Exception {
        final CountDownLatch lock = new CountDownLatch(1);

        client.post(BASE_URL + "/echoMultipart")
                .header("accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("name", "Mark")
                .field("hello", "world")
                .asJsonAsync(new Callback<JsonNode>() {

                    public void failed(Exception e) {
                        fail();
                    }

                    public void completed(HttpResponse<JsonNode> response) {
                        JsonNode json = response.getBody();
                        assertEquals("Mark", json.getObject().getJSONObject("body").getJSONArray("name").getString(0));
                        assertEquals("world", json.getObject().getJSONObject("body").getJSONArray("hello").getString(0));

                        assertEquals("application/x-www-form-urlencoded", json.getObject().getJSONObject("headers").getJSONArray("Content-Type").get(0));

                        status = true;
                        lock.countDown();
                    }

                    public void cancelled() {
                        fail();
                    }
                });

        if (!lock.await(10, TimeUnit.SECONDS)) {
            fail("Wait time exceeded");
        }
        assertTrue(status);
    }

    @Test
    public void getQuerystringArray() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo")
                .queryString("name", "Mark")
                .queryString("name", "Tom")
                .asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("queryParams").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void postMultipleFiles() throws Exception {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .part("param3", "wot")
                .part("file1", new File(getClass().getResource("/test").toURI()))
                .part("file2", new File(getClass().getResource("/test").toURI()))
                .asJson();

        JSONObject body = response.getBody().getObject().getJSONObject("body");
        assertEquals(3, body.length());


        assertEquals("This is a test file", body.getJSONArray("file1").getJSONObject(0).getString("content"));
        assertEquals("This is a test file", body.getJSONArray("file2").getJSONObject(0).getString("content"));

        assertEquals("wot", body.getJSONArray("param3").getString(0));
    }

    @Test
    public void getArray() {
        HttpResponse<JsonNode> response = client.get(BASE_URL + "/echo")
                .queryString("name", Arrays.asList("Mark", "Tom"))
                .asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("queryParams").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void postArray() {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", "Mark")
                .field("name", "Tom")
                .asJson();

        assertEquals(200, response.getStatus());
        JSONArray names = response.getBody().getObject().getJSONObject("body").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }


    @Test
    public void formArrayValue() {
        HttpResponse<JsonNode> response = client.post(BASE_URL + "/echoMultipart")
                .field("name", Arrays.asList("Mark", "Tom"))
                .asJson();

        JSONArray names = response.getBody().getObject().getJSONObject("body").getJSONArray("name");
        assertEquals(2, names.length());

        assertEquals("Mark", names.getString(0));
        assertEquals("Tom", names.getString(1));
    }

    @Test
    public void caseInsensitiveHeaders() {
        GetRequest request = client.get(BASE_URL + "/echoHeaders")
                .header("Name", "Marco");

        assertEquals(1, request.getHeaders().size());
        assertEquals("Marco", request.getHeaders().get("name").get(0));
        assertEquals("Marco", request.getHeaders().get("NAme").get(0));
        assertEquals("Marco", request.getHeaders().get("Name").get(0));

        HttpResponse<JsonNode> response = request.asJson();
        JSONObject headers = response.getBody().getObject();
        assertEquals("Marco", headers.getJSONArray("Name").getString(0));

    }

    @Test
    public void multipleHeaders() {
        GetRequest request = client.get("http://localhost:9000/echoHeaders")
                .header("Name", "Marco")
                .header("Name", "John");

        assertEquals(1, request.getHeaders().size());
        assertEquals("Marco", request.getHeaders().get("name").get(0));
        assertEquals("John", request.getHeaders().get("name").get(1));
        assertEquals("Marco", request.getHeaders().get("NAme").get(0));
        assertEquals("John", request.getHeaders().get("NAme").get(1));
        assertEquals("Marco", request.getHeaders().get("Name").get(0));
        assertEquals("John", request.getHeaders().get("Name").get(1));

        JSONArray headers = request.asJson().getBody().getObject().getJSONArray("Name");

        Set<String> headerValues = StreamSupport.stream(headers.spliterator(), false)
                .map(String::valueOf).collect(Collectors.toSet());

        assertTrue(headerValues.contains("Marco"));
        assertTrue(headerValues.contains("John"));
    }


    @Test
    public void multipleClients() {
        RestClient client1 = RestClient.builder().build();
        int status = client1.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);

        RestClient client2 = RestClient.builder().build();
        status = client2.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);

        RestClient client3 = RestClient.builder().build();
        status = client3.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);

        client1.close();
        client2.close();
        client3.close();
    }

    @Test
    public void multipleClients_sameBuilder() {
        ClientBuilder builder = RestClient.builder();
        RestClient client1 = builder.build();
        int status = client1.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);
        client1.close();

        RestClient client2 = builder.build();
        status = client2.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);
        client2.close();

        RestClient client3 = builder.build();
        status = client3.get(BASE_URL + "/echo").asString().getStatus();
        assertEquals(200, status);
        client3.close();
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
    public void defaultObjectMapper() {
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
    public void binaryCloseConnection() {
        String sourceString = "AAAAAAAAAAAAA";

        try (HttpResponse<InputStream> response = client.post(BASE_URL + "/echoBinary")
                .header("Content-type", "text/plain")
                .body(sourceString.getBytes(StandardCharsets.UTF_8))
                .asBinary()) {

            InputStream body = response.getBody();
            String received = TestUtils.toString(body);
            assertEquals(sourceString, received);
        }
        ClientStats stats = client.stats();
        assertEquals(0, stats.sync.getLeased());
    }

    @Test
    public void customObjectMapper() {

        ObjectMappers.register(MediaType.valueOf(TestServer.contentType), new JsonMapper());

        TestData testData = new TestData("yolo");
        HttpResponse<TestData> response = client.post(BASE_URL + "/echoCustomType")
                .header("accept", TestServer.contentType)
                .header("Content-Type", TestServer.contentType)
                .body(testData).asObject(TestData.class);

        assertEquals(200, response.getStatus());
        assertEquals(testData, response.getBody());
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
    public void urlTransformer() {
        RestClient client = RestClient.builder()
                .baseUrl("http://invalid-url.abc")
                .urlTransformer((url) -> BASE_URL + "/echo")
                .build();

        HttpResponse<String> response = client.get("/get").asString();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void baseUrl() {
        try (RestClient customClient = RestClient.builder()
                .baseUrl(BASE_URL + "/echo")
                .build()) {
            HttpResponse<JsonNode> postResponse = customClient.get("/get")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .asJson();

            assertEquals(200, postResponse.getStatus());
        }
    }

    @Test
    public void noCookieSpec() {
        String value = "COOKIE-VALUE-A";
        String key = "COOKIEA";
        try (RestClient cookieClient = RestClient.builder().build()) {
            HttpResponse<String> response = cookieClient.get(BASE_URL + "/set-cookie")
                    .queryString(key, value)
                    .asString();

            assertEquals(200, response.getStatus());
            List<String> cookies = response.getHeaders().get("Set-Cookie");
            assertEquals(1, cookies.size());
            assertEquals(key + "=" + value, cookies.get(0));
        }

    }

    @Test
    public void compressionDisabled() {
        try (RestClient noCompressionClient = RestClient.builder().disableContentCompression().build()) {
            HttpResponse<JsonNode> response = noCompressionClient.get(BASE_URL + "/echoHeaders").asJson();
            assertFalse(response.getBody().getObject().has("Accept-Encoding"));
        }
    }

    @Test
    public void compressionEnabledByDefault() {
        try (RestClient noCompressionClient = RestClient.builder().build()) {
            HttpResponse<JsonNode> response = noCompressionClient.get(BASE_URL + "/echoHeaders").asJson();
            assertTrue(response.getBody().getObject().has("Accept-Encoding"));
            assertEquals(1, response.getBody().getObject().getJSONArray("Accept-Encoding").length()); //single entry

            String acceptEncodings = response.getBody().getObject().getJSONArray("Accept-Encoding").getString(0);

            assertTrue(acceptEncodings.contains("gzip"));
            assertTrue(acceptEncodings.contains("deflate"));
        }
    }

}
