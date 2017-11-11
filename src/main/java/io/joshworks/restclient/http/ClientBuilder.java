package io.joshworks.restclient.http;

import io.joshworks.restclient.http.mapper.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ClientBuilder {


    ClientBuilder() {
    }

    private int connectionTimeout = 10000;
    private int socketTimeout = 60000;
    private int maxTotal = 20;
    private boolean followredirect = true;
    private String baseUrl = "";
//        private int maxPerRoute = 20;

    private Function<String, String> urlTransformer = (url) -> url;
    private ObjectMapper objectMapper = new JsonMapper();

    private HttpHost proxy;
    private String cookieSpec = CookieSpecs.STANDARD;

    private Map<String, Object> defaultHeaders = new HashMap<>();

    public RestClient build() {
        try {
            // Create common default configuration
            RequestConfig clientConfig = RequestConfig.custom()
                    .setRedirectsEnabled(followredirect)
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
                    .setConnectionRequestTimeout(socketTimeout)
                    .setProxy(proxy)
                    .setCookieSpec(cookieSpec)
                    .build();

            PoolingHttpClientConnectionManager syncConnectionManager = new PoolingHttpClientConnectionManager();
            syncConnectionManager.setMaxTotal(maxTotal);
//            syncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);


            CloseableHttpClient syncClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(clientConfig)
                    .setConnectionManager(syncConnectionManager)
                    .build();


            DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor();
            PoolingNHttpClientConnectionManager asyncConnectionManager = new PoolingNHttpClientConnectionManager(ioreactor);
            asyncConnectionManager.setMaxTotal(maxTotal);
//                asyncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);

            CloseableHttpAsyncClient asyncClient = HttpAsyncClientBuilder.create()
                    .setDefaultRequestConfig(clientConfig)
                    .setConnectionManager(asyncConnectionManager)
                    .build();


            RestClient restClient = new RestClient(baseUrl, objectMapper, defaultHeaders, urlTransformer, asyncConnectionManager, syncConnectionManager, asyncClient, syncClient);
            ClientContainer.addClient(restClient);
            return restClient;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ClientBuilder defaultHeader(String key, String value) {
        this.defaultHeaders.put(key, value);
        return this;
    }

    public ClientBuilder followRedirect(boolean followRedirect) {
        this.followredirect = followRedirect;
        return this;
    }

    public ClientBuilder defaultHeader(String key, long value) {
        this.defaultHeaders.put(key, value);
        return this;
    }

    public ClientBuilder urlTransformer(Function<String, String> transformer) {
        this.urlTransformer = transformer;
        return this;
    }

    public ClientBuilder cookieSpec(String cookieSpec) {
        this.cookieSpec = cookieSpec;
        return this;
    }

    /**
     * Set a proxy
     */
    public ClientBuilder proxy(HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * Set the ObjectMapper implementation to use for Response to Object binding
     *
     * @param objectMapper Custom implementation of ObjectMapper interface
     */
    public ClientBuilder objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * Set the connection timeout and socket timeout
     *
     * @param connectionTimeout The timeout until a connection with the server is established (in milliseconds). Default is 10000. Set to zero to disable the timeout.
     * @param socketTimeout     The timeout to receive data (in milliseconds). Default is 60000. Set to zero to disable the timeout.
     */
    public ClientBuilder timeout(int connectionTimeout, int socketTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * Set the concurrency levels
     *
     * @param maxTotal Defines the overall connection limit for a connection pool. Default is 20.
     */
    public ClientBuilder concurrency(int maxTotal) {
        this.maxTotal = maxTotal;
        return this;
    }

}
