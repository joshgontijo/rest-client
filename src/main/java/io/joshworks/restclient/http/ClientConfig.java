package io.joshworks.restclient.http;

import io.joshworks.restclient.http.mapper.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.util.Map;

/**
 * Created by Josh Gontijo on 5/3/17.
 */
public class ClientConfig {

    public final CloseableHttpClient syncClient;
    public final CloseableHttpAsyncClient asyncClient;
    public final Map<String, Object> defaultHeaders;
    public final ObjectMapper mapper;
    public final String url;
    public final HttpMethod httpMethod;

    public ClientConfig(HttpMethod httpMethod, String url, RestClient.Configuration builder) {

        this.syncClient = builder.getSyncClient();
        this.asyncClient = builder.getAsyncClient();
        this.defaultHeaders = builder.getDefaultHeaders();
        this.mapper = builder.getObjectMapper();
        this.url = url;
        this.httpMethod = httpMethod;
    }
}
