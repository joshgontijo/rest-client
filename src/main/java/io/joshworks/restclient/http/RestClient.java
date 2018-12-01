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

import io.joshworks.restclient.http.utils.ClientStats;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.joshworks.restclient.http.utils.Constants.PATH_SEPARATOR;

public class RestClient implements Closeable {

    private static final int IDLE_CONNECTION_TIMEOUT = 30;

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
        this.id = newUUID();
    }

    private RestClient(HttpClientBuilder clientBuilder, HttpAsyncClientBuilder asyncClientBuilder) {
        this.id = newUUID();
        this.baseUrl = "";
        this.urlTransformer = url -> url;
        this.cookieStore = new BasicCookieStore();

        if (clientBuilder != null) {
            this.syncConnectionManager = new PoolingHttpClientConnectionManager();
            clientBuilder.setConnectionManager(syncConnectionManager);
            clientBuilder.setDefaultCookieStore(cookieStore);
            this.syncClient = clientBuilder.build();
        } else {
            this.syncClient = null;
            this.syncConnectionManager = null;
        }

        if (asyncClientBuilder != null) {
            try {
                PoolingNHttpClientConnectionManager asyncConnManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
                asyncClientBuilder.setConnectionManager(asyncConnManager);
                asyncClientBuilder.setDefaultCookieStore(cookieStore);
                this.asyncClient = asyncClientBuilder.build();
                this.asyncConnectionManager = asyncConnManager;
            } catch (IOReactorException e) {
                throw new IllegalStateException("Failed to Async IO reactor", e);

            }
        } else {
            this.asyncClient = null;
            this.asyncConnectionManager = null;
        }
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public static RestClient with(HttpClientBuilder clientBuilder) {
        return with(clientBuilder, null);
    }

    public static RestClient with(HttpClientBuilder clientBuilder, HttpAsyncClientBuilder asyncClientBuilder) {
        return new RestClient(clientBuilder, asyncClientBuilder);
    }

    public GetRequest get(String... url) {
        return new GetRequest(new ClientRequest(HttpMethod.GET, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public GetRequest head(String... url) {
        return new GetRequest(new ClientRequest(HttpMethod.HEAD, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody options(String... url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.OPTIONS, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody post(String... url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.POST, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody delete(String... url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.DELETE, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody patch(String... url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PATCH, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public HttpRequestWithBody put(String... url) {
        return new HttpRequestWithBody(new ClientRequest(HttpMethod.PUT, resolveUrl(url), syncClient, asyncClient, defaultHeaders));
    }

    public CookieStore cookieStore() {
        return cookieStore;
    }

    public ClientStats stats() {
        return new ClientStats(syncConnectionManager.getTotalStats(), asyncConnectionManager.getTotalStats());
    }

    String resolveUrl(String... paths) {
        StringJoiner pathJoiner = new StringJoiner(PATH_SEPARATOR);
        for (String path : paths) {
            if(path != null && !path.trim().isEmpty()) {
                path = path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path;
                path = path.endsWith(PATH_SEPARATOR) ? path.substring(0, path.length() - 1) : path;
                pathJoiner.add(path);
            }
        }

        if(baseUrl == null || baseUrl.trim().isEmpty()) {
            return pathJoiner.toString();
        }
        String base = urlTransformer.apply(baseUrl);
        base = !base.endsWith(PATH_SEPARATOR) ? base + PATH_SEPARATOR : base;
        return base + pathJoiner.toString();
    }

    void closeIdleConnections() {
        if (asyncConnectionManager != null) {
            asyncConnectionManager.closeExpiredConnections();
            asyncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }
        if (syncConnectionManager != null) {
            syncConnectionManager.closeExpiredConnections();
            syncConnectionManager.closeIdleConnections(IDLE_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    /**
     * Close the asynchronous client and its event loop. Use this method to close all the threads and allow an application to exit.
     */
    @Override
    public void close() {
        try {
            if (syncClient != null) {
                syncClient.close();
                syncConnectionManager.close();
            }

            if (asyncClient != null && asyncClient.isRunning()) {
                asyncClient.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        ClientContainer.removeClient(this);
    }

    private String newUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
