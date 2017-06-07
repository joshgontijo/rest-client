package io.joshworks.restclient.http;

import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;

/**
 * Created by Josh Gontijo on 6/3/17.
 */
public class SimpleClient {

    private static RestClient client = RestClient.newClient().build();
    private static ObjectMapper mapper = new JsonMapper();

    public static void objectMapper(ObjectMapper mapper) {
        SimpleClient.mapper = mapper;
        client.shutdown();
        client = null;
    }

    private static RestClient getClient() {
        if (client == null) {
            synchronized (RestClient.class) {
                if (client == null) {
                    client = RestClient.newClient().objectMapper(mapper).build();
                }
            }
        }
        return client;
    }

    public static void close() {
        if (client != null) {
            client.shutdown();
        }
    }

    public static GetRequest get(String url) {
        return getClient().get(url);
    }

    public static GetRequest head(String url) {
        return getClient().head(url);
    }

    public static HttpRequestWithBody options(String url) {
        return getClient().options(url);
    }

    public static HttpRequestWithBody post(String url) {
        return getClient().post(url);
    }

    public static HttpRequestWithBody delete(String url) {
        return getClient().delete(url);
    }

    public static HttpRequestWithBody patch(String url) {
        return getClient().patch(url);
    }

    public static HttpRequestWithBody put(String url) {
        return getClient().put(url);
    }


}
