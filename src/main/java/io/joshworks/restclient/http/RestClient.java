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

import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RestClient {

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private static final int IDLE_CONNECTION_TIMEOUT = 30;

    private final Configuration configuration;
    final String id;

    private RestClient(Configuration configuration) {
        this.configuration = configuration;
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public static Configuration newClient() {
        return new Configuration();
    }

    public GetRequest get(String url) {
        return new GetRequest(new ClientRequest(HttpMethod.GET, configuration.resolveUrl(url), configuration));
    }

    public GetRequest head(String url) {
        return new GetRequest(new ClientRequest(HttpMethod.HEAD, configuration.resolveUrl(url), configuration));
    }

    public HttpRequestWithBody options(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.OPTIONS, configuration.resolveUrl(url), configuration));
    }

    public HttpRequestWithBody post(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.POST, configuration.resolveUrl(url), configuration));
    }

    public HttpRequestWithBody delete(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.DELETE, configuration.resolveUrl(url), configuration));
    }

    public HttpRequestWithBody patch(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PATCH, configuration.resolveUrl(url), configuration));
    }

    public HttpRequestWithBody put(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PUT, configuration.resolveUrl(url), configuration));
    }

    void closeIdleConnections() {
        configuration.asyncConnectionManager.closeExpiredConnections();
        configuration.asyncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

        configuration.syncConnectionManager.closeExpiredConnections();
        configuration.syncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Close the asynchronous client and its event loop. Use this method to close all the threads and allow an application to exit.
     */
    public void shutdown() {
        try {
            // Closing the Sync HTTP client
            CloseableHttpClient syncClient = configuration.getSyncClient();
            if (syncClient != null) {
                syncClient.close();
            }

            // Closing the Async HTTP client (if running)
            CloseableHttpAsyncClient asyncClient = configuration.getAsyncClient();
            if (asyncClient != null && asyncClient.isRunning()) {
                asyncClient.close();
            }
        }catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        ClientContainer.removeClient(this);
    }

    public static class Configuration {

        private Configuration() {

        }

        private int connectionTimeout = 10000;
        private int socketTimeout = 60000;
        private int maxTotal = 20;
        private String baseUrl = "";
//        private int maxPerRoute = 20;

        private Function<String, String> urlTransformer = (url) -> url;
        private ObjectMapper objectMapper = new JsonMapper();

        private HttpHost proxy;
        private PoolingNHttpClientConnectionManager asyncConnectionManager;
        private PoolingHttpClientConnectionManager syncConnectionManager;

        private Map<String, Object> defaultHeaders = new HashMap<>();

        private CloseableHttpAsyncClient asyncClient;
        private CloseableHttpClient syncClient;

        //failsafe
        private RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(0);
        private CircuitBreaker circuitBreaker = new CircuitBreaker();

        public RestClient build() {
            // Create common default configuration
            RequestConfig clientConfig = RequestConfig.custom()
                    .setConnectTimeout((connectionTimeout))
                    .setSocketTimeout((socketTimeout))
                    .setConnectionRequestTimeout((socketTimeout))
                    .setProxy(proxy)
                    .build();

            syncConnectionManager = new PoolingHttpClientConnectionManager();
            syncConnectionManager.setMaxTotal(maxTotal);
//            syncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);


            //default client
            if (this.syncClient == null) {
                this.syncClient = HttpClientBuilder.create()
                        .setDefaultRequestConfig(clientConfig)
                        .setConnectionManager(syncConnectionManager)
                        .build();
            }


            try {
                DefaultConnectingIOReactor ioreactor = new DefaultConnectingIOReactor();
                asyncConnectionManager = new PoolingNHttpClientConnectionManager(ioreactor);
                asyncConnectionManager.setMaxTotal(maxTotal);
//                asyncConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
            } catch (IOReactorException e) {
                throw new RuntimeException(e);
            }

            if (this.asyncClient == null) {
                this.asyncClient = HttpAsyncClientBuilder.create()
                        .setDefaultRequestConfig(clientConfig)
                        .setConnectionManager(asyncConnectionManager)
                        .build();
            }

            RestClient restClient = new RestClient(this);
            ClientContainer.addClient(restClient);
            return restClient;
        }

        public Configuration baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Configuration defaultHeader(String key, String value) {
            this.defaultHeaders.put(key, value);
            return this;
        }

        public Configuration defaultHeader(String key, long value) {
            this.defaultHeaders.put(key, value);
            return this;
        }

        public Configuration urlTransformer(Function<String, String> transformer) {
            this.urlTransformer = transformer;
            return this;
        }

        /**
         * Set the HttpClient implementation to use for every synchronous request
         */
        public Configuration httpClient(CloseableHttpClient httpClient) {
            this.syncClient = httpClient;
            return this;
        }

        /**
         * Set the asynchronous AbstractHttpAsyncClient implementation to use for every asynchronous request
         */
        public Configuration asyncHttpClient(CloseableHttpAsyncClient asyncHttpClient) {
            this.asyncClient = asyncHttpClient;
            return this;
        }

        /**
         * Set a proxy
         */
        public Configuration proxy(HttpHost proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Set the ObjectMapper implementation to use for Response to Object binding
         *
         * @param objectMapper Custom implementation of ObjectMapper interface
         */
        public Configuration objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Set the connection timeout and socket timeout
         *
         * @param connectionTimeout The timeout until a connection with the server is established (in milliseconds). Default is 10000. Set to zero to disable the timeout.
         * @param socketTimeout     The timeout to receive data (in milliseconds). Default is 60000. Set to zero to disable the timeout.
         */
        public Configuration timeouts(int connectionTimeout, int socketTimeout) {
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
//        public Configuration concurrency(int maxTotal, int maxPerRoute) {
//            this.maxTotal = maxTotal;
//            this.maxPerRoute = maxPerRoute;
//            return this;
//        }

        /**
         * Set the concurrency levels
         *
         * @param maxTotal Defines the overall connection limit for a connection pool. Default is 20.
         */
        public Configuration concurrency(int maxTotal) {
            this.maxTotal = maxTotal;
            return this;
        }

        /**
         * Configure the retry policy for this client. this feature is disabled if nothing is set
         *
         * @param retryPolicy The Failsafe's RetryPolicy to be used on thi client.
         */
        public Configuration retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Configure the circuit breaker for this client. this feature is disabled if nothing is set
         *
         * @param circuitBreaker The Failsafe's CircuitBreaker to be used on thi client.
         */
        public Configuration circuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
            return this;
        }

        //Only applicable for the base url
        private String resolveUrl(String url) {
            return urlTransformer.apply(baseUrl) + url;
        }

        Map<String, Object> getDefaultHeaders() {
            return defaultHeaders;
        }

        CloseableHttpAsyncClient getAsyncClient() {
            return asyncClient;
        }

        CloseableHttpClient getSyncClient() {
            return syncClient;
        }

        ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }
    }

}
