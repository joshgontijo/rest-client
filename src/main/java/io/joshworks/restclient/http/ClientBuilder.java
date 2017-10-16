package io.joshworks.restclient.http;

import io.joshworks.restclient.http.mapper.ObjectMapper;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;
import net.jodah.failsafe.function.CheckedConsumer;
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

    //failsafe
    private SyncFailsafe<Object> failsafe;

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


            RestClient restClient = new RestClient(baseUrl, objectMapper, defaultHeaders, urlTransformer, failsafe, asyncConnectionManager, syncConnectionManager, asyncClient, syncClient);
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
    public ClientBuilder timeouts(int connectionTimeout, int socketTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        return this;
    }

//        /**
//         * Set the concurrency levels
//         *
//         * @param maxTotal    Defines the overall connection limit for a connection pool. Default is 200.
//         * @param maxPerRoute Defines a connection limit per one HTTP route (this can be considered a per target host limit). Default is 20.
//         */
//        public ClientBuilder concurrency(int maxTotal, int maxPerRoute) {
//            this.maxTotal = maxTotal;
//            this.maxPerRoute = maxPerRoute;
//            return this;
//        }

    /**
     * Set the concurrency levels
     *
     * @param maxTotal Defines the overall connection limit for a connection pool. Default is 20.
     */
    public ClientBuilder concurrency(int maxTotal) {
        this.maxTotal = maxTotal;
        return this;
    }

    /**
     * Configure the retry policy for this client. this feature is disabled if nothing is set
     *
     * @param retryPolicy The Failsafe's RetryPolicy to be used on thi client.
     */
    public ClientBuilder retryPolicy(RetryPolicy retryPolicy) {
        failsafe = failsafe == null ? Failsafe.with(retryPolicy) : failsafe.with(retryPolicy);
        return this;
    }

    /**
     * Configure the circuit breaker for this client. this feature is disabled if nothing is set
     *
     * @param circuitBreaker The Failsafe's CircuitBreaker to be used on thi client.
     */
    public ClientBuilder circuitBreaker(CircuitBreaker circuitBreaker) {
        failsafe = failsafe == null ? Failsafe.with(circuitBreaker) : failsafe.with(circuitBreaker);
        return this;
    }

    public ClientBuilder onFailedAttempt(CheckedConsumer<Exception> onFailedAttempt) {
        this.failsafe = this.failsafe == null ? Failsafe.with(new RetryPolicy()) : this.failsafe.onFailedAttempt(onFailedAttempt);
        return this;
    }

    public ClientBuilder onRetriesExceeded(CheckedConsumer<Exception> onRetriesExceeded) {
        this.failsafe = this.failsafe == null ? Failsafe.with(new RetryPolicy()) : this.failsafe.onRetriesExceeded(onRetriesExceeded);
        return this;
    }

    public ClientBuilder failsafe(SyncFailsafe<Object> failsafe) {
        this.failsafe = failsafe;
        return this;
    }


}
