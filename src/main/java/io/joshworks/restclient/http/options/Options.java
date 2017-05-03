package io.joshworks.restclient.http.options;

import io.joshworks.restclient.http.ObjectMapper;
import io.joshworks.restclient.http.RestClient;
import io.joshworks.restclient.http.async.utils.AsyncIdleConnectionMonitorThread;
import io.joshworks.restclient.http.utils.SyncIdleConnectionMonitorThread;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import java.util.HashMap;
import java.util.Map;

public class Options {

    private int connectionTimeout = 10000;
    private int socketTimeout = 60000;
    private int maxTotal = 200;
    private int maxPerRoute = 20;

    private ObjectMapper objectMapper;
    private HttpHost proxy;
    private SyncIdleConnectionMonitorThread syncIdleConnectionMonitorThread;
    private AsyncIdleConnectionMonitorThread asyncIdleConnectionMonitorThread;

    private Map<String, Object> defaultHeaders = new HashMap<>();

    private CloseableHttpAsyncClient asyncClient;
    private CloseableHttpClient syncClient;

    public RestClient build() {
        // Create common default configuration
        RequestConfig clientConfig = RequestConfig.custom()
                .setConnectTimeout((connectionTimeout))
                .setSocketTimeout((socketTimeout))
                .setConnectionRequestTimeout((socketTimeout))
                .setProxy(proxy).build();

        PoolingHttpClientConnectionManager syncConnectionManager = new PoolingHttpClientConnectionManager();
        syncConnectionManager.setMaxTotal(maxTotal);
        syncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);


        //default client
        if (this.syncClient == null) {
            this.syncClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(clientConfig)
                    .setConnectionManager(syncConnectionManager)
                    .build();
        }

        this.syncIdleConnectionMonitorThread = new SyncIdleConnectionMonitorThread(syncConnectionManager);
        syncIdleConnectionMonitorThread.start();


        DefaultConnectingIOReactor ioreactor;
        PoolingNHttpClientConnectionManager asyncConnectionManager;
        try {
            ioreactor = new DefaultConnectingIOReactor();
            asyncConnectionManager = new PoolingNHttpClientConnectionManager(ioreactor);
            asyncConnectionManager.setMaxTotal(maxTotal);
            asyncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        } catch (IOReactorException e) {
            throw new RuntimeException(e);
        }

        if (this.asyncClient == null) {
            this.asyncClient = HttpAsyncClientBuilder.create()
                    .setDefaultRequestConfig(clientConfig)
                    .setConnectionManager(asyncConnectionManager)
                    .build();
        }

        this.asyncIdleConnectionMonitorThread = new AsyncIdleConnectionMonitorThread(asyncConnectionManager);


        throw new UnsupportedOperationException("TODO - NOT IMPLEMENTED YET");
    }


    public void addDefaultHeader(String key, String value) {
        this.defaultHeaders.put(key, value);
    }

    public void addDefaultHeader(String key, long value) {
        this.defaultHeaders.put(key, value);
    }

    /**
     * Set the HttpClient implementation to use for every synchronous request
     */
    public void httpClient(CloseableHttpClient httpClient) {
        this.syncClient = httpClient;
    }

    /**
     * Set the asynchronous AbstractHttpAsyncClient implementation to use for every asynchronous request
     */
    public void asyncHttpClient(CloseableHttpAsyncClient asyncHttpClient) {
        this.asyncClient = asyncHttpClient;
    }

    /**
     * Set a proxy
     */
    public void proxy(HttpHost proxy) {
        this.proxy = proxy;
    }

    /**
     * Set the ObjectMapper implementation to use for Response to Object binding
     *
     * @param objectMapper Custom implementation of ObjectMapper interface
     */
    public void objectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Set the connection timeout and socket timeout
     *
     * @param connectionTimeout The timeout until a connection with the server is established (in milliseconds). Default is 10000. Set to zero to disable the timeout.
     * @param socketTimeout     The timeout to receive data (in milliseconds). Default is 60000. Set to zero to disable the timeout.
     */
    public void timeouts(int connectionTimeout, int socketTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Set the concurrency levels
     *
     * @param maxTotal    Defines the overall connection limit for a connection pool. Default is 200.
     * @param maxPerRoute Defines a connection limit per one HTTP route (this can be considered a per target host limit). Default is 20.
     */
    public void concurrency(int maxTotal, int maxPerRoute) {
        this.maxTotal = maxTotal;
        this.maxPerRoute = maxPerRoute;
    }


}
