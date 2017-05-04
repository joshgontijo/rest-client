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

package io.joshworks.restclient.request;

import io.joshworks.restclient.http.HttpClientHelper;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.JsonNode;
import io.joshworks.restclient.http.ObjectMapper;
import io.joshworks.restclient.http.ClientConfig;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.async.AsyncIdleConnectionMonitorThread;
import io.joshworks.restclient.http.exceptions.RestClientException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

public abstract class BaseRequest {


    protected HttpRequest httpRequest;
    private final CloseableHttpClient syncClient;
    private final CloseableHttpAsyncClient asyncClient;
    private final Map<String, Object> defaultHeaders;
    private final ObjectMapper mapper;
    private AsyncIdleConnectionMonitorThread monitor;

    protected BaseRequest(ClientConfig config) {
        this.syncClient = config.syncClient;
        this.asyncClient = config.asyncClient;
        this.defaultHeaders = config.defaultHeaders;
        this.mapper = config.mapper;
        this.monitor = config.monitor;
    }


    public HttpRequest getHttpRequest() {
        return this.httpRequest;
    }

    public HttpResponse<String> asString() throws RestClientException {
        return HttpClientHelper.request(httpRequest, String.class, syncClient, defaultHeaders, mapper);
    }

    public Future<HttpResponse<String>> asStringAsync() {
        return HttpClientHelper.requestAsync(httpRequest, String.class, null, asyncClient, defaultHeaders, mapper, monitor);
    }

    public Future<HttpResponse<String>> asStringAsync(Callback<String> callback) {
        return HttpClientHelper.requestAsync(httpRequest, String.class, callback, asyncClient, defaultHeaders, mapper, monitor);
    }

    public HttpResponse<JsonNode> asJson() throws RestClientException {
        return HttpClientHelper.request(httpRequest, JsonNode.class, syncClient, defaultHeaders, mapper);
    }

    public Future<HttpResponse<JsonNode>> asJsonAsync() {
        return HttpClientHelper.requestAsync(httpRequest, JsonNode.class, null, asyncClient, defaultHeaders, mapper, monitor);
    }

    public Future<HttpResponse<JsonNode>> asJsonAsync(Callback<JsonNode> callback) {
        return HttpClientHelper.requestAsync(httpRequest, JsonNode.class, callback, asyncClient, defaultHeaders, mapper, monitor);
    }

    public <T> HttpResponse<T> asObject(Class<? extends T> responseClass) throws RestClientException {
        return HttpClientHelper.request(httpRequest, (Class) responseClass, syncClient, defaultHeaders, mapper);
    }

    public <T> Future<HttpResponse<T>> asObjectAsync(Class<? extends T> responseClass) {
        return HttpClientHelper.requestAsync(httpRequest, (Class) responseClass, null, asyncClient, defaultHeaders, mapper, monitor);
    }

    public <T> Future<HttpResponse<T>> asObjectAsync(Class<? extends T> responseClass, Callback<T> callback) {
        return HttpClientHelper.requestAsync(httpRequest, (Class) responseClass, callback,asyncClient, defaultHeaders, mapper, monitor);
    }

    public HttpResponse<InputStream> asBinary() throws RestClientException {
        return HttpClientHelper.request(httpRequest, InputStream.class, syncClient, defaultHeaders, mapper);
    }

    public Future<HttpResponse<InputStream>> asBinaryAsync() {
        return HttpClientHelper.requestAsync(httpRequest, InputStream.class, null, asyncClient, defaultHeaders, mapper, monitor);
    }

    public Future<HttpResponse<InputStream>> asBinaryAsync(Callback<InputStream> callback) {
        return HttpClientHelper.requestAsync(httpRequest, InputStream.class, callback, asyncClient, defaultHeaders, mapper, monitor);
    }

}
