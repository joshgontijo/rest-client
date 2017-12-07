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

import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RestClient implements Closeable {

    static final int IDLE_CONNECTION_TIMEOUT = 30;

    public final String id;

    private final Function<String, String> urlTransformer;

    private final String baseUrl;
    private final PoolingNHttpClientConnectionManager asyncConnectionManager;
    private final PoolingHttpClientConnectionManager syncConnectionManager;

    private final Map<String, Object> defaultHeaders = new HashMap<>();

    private final CloseableHttpAsyncClient asyncClient;
    private final CloseableHttpClient syncClient;
    private final CookieStore cookieStore;

    RestClient(String baseUrl,
               Map<String, Object> defaultHeaders,
               Function<String, String> urlTransformer,
               PoolingNHttpClientConnectionManager asyncConnectionManager,
               PoolingHttpClientConnectionManager syncConnectionManager,
               CloseableHttpAsyncClient asyncClient,
               CloseableHttpClient syncClient, CookieStore cookieStore) {
        this.baseUrl = baseUrl;
        this.urlTransformer = urlTransformer;
        this.asyncConnectionManager = asyncConnectionManager;
        this.syncConnectionManager = syncConnectionManager;
        this.asyncClient = asyncClient;
        this.syncClient = syncClient;
        this.cookieStore = cookieStore;
        this.defaultHeaders.putAll(defaultHeaders);
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public GetRequest get(String url) {
        return new GetRequest(new ClientRequest(HttpMethod.GET, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public GetRequest head(String url) {
        return new GetRequest(new ClientRequest(HttpMethod.HEAD, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody options(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.OPTIONS, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody post(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.POST, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody delete(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.DELETE, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody patch(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PATCH, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody put(String url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PUT, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public CookieStore cookieStore() {
        return cookieStore;
    }

    public PoolStats stats(ClientType type) {
        return ClientType.SYNC.equals(type) ? syncConnectionManager.getTotalStats() : asyncConnectionManager.getTotalStats();
    }

   public enum ClientType {
        SYNC, ASYNC
   }

    private String resolveUrl(String url) {
        return urlTransformer.apply(baseUrl) + url;
    }

    void closeIdleConnections() {
        asyncConnectionManager.closeExpiredConnections();
        asyncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);

        syncConnectionManager.closeExpiredConnections();
        syncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Close the asynchronous client and its event loop. Use this method to close all the threads and allow an application to exit.
     */
    @Override
    public void close() {
        try {
            // Closing the Sync HTTP client
            if (syncClient != null) {
                syncClient.close();
                syncConnectionManager.close();
            }

            // Closing the Async HTTP client (if running)
            if (asyncClient != null && asyncClient.isRunning()) {
                asyncClient.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        ClientContainer.removeClient(this);
    }
}
