package io.joshworks.restclient.http;

import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;

/**
 * Created by Josh Gontijo on 6/3/17.
 */
public class SimpleClient {

    private static RestClient client = RestClient.newClient().build();

    public static void objectMapper(ObjectMapper mapper) {
        client.shutdown();
        client = RestClient.newClient().objectMapper(mapper).build();
    }

    public static void close() {
        client.shutdown();
    }

    public static GetRequest get(String url) {
        return client.get(url);
    }

    public static GetRequest head(String url) {
        return client.head(url);
    }

    public static HttpRequestWithBody options(String url) {
        return client.options(url);
    }

    public static HttpRequestWithBody post(String url) {
        return client.post(url);
    }

    public static HttpRequestWithBody delete(String url) {
        return client.delete(url);
    }

    public static HttpRequestWithBody patch(String url) {
        return client.patch(url);
    }

    public static HttpRequestWithBody put(String url) {
        return client.put(url);
    }

}
