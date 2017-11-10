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

import io.joshworks.restclient.http.ClientRequest;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.JsonNode;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.exceptions.RestClientException;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class BaseRequest {

    protected HttpRequest httpRequest;
    protected final ClientRequest clientRequest;

    protected BaseRequest(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    public HttpRequest getHttpRequest() {
        return this.httpRequest;
    }

    public HttpResponse<String> asString() throws RestClientException {
        return clientRequest.request(httpRequest, String.class);
    }

    public CompletableFuture<HttpResponse<String>> asStringAsync() {
        return clientRequest.requestAsync(httpRequest, String.class);
    }

    public void asStringAsync(Callback<String> callback) {
        clientRequest.requestAsync(httpRequest, String.class, callback);
    }

    public HttpResponse<JsonNode> asJson() throws RestClientException {
        return clientRequest.request(httpRequest, JsonNode.class);
    }

    public CompletableFuture<HttpResponse<JsonNode>> asJsonAsync() {
        return clientRequest.requestAsync(httpRequest, JsonNode.class);
    }

    public Future<HttpResponse<JsonNode>> asJsonAsync(Callback<JsonNode> callback) {
        return clientRequest.requestAsync(httpRequest, JsonNode.class, callback);
    }

    public <T> HttpResponse<T> asObject(Class<T> responseClass) throws RestClientException {
        return clientRequest.request(httpRequest, responseClass);
    }

    public <T> CompletableFuture<HttpResponse<T>> asObjectAsync(Class<T> responseClass) {
        return clientRequest.requestAsync(httpRequest, responseClass);
    }

    public <T> Future<HttpResponse<T>> asObjectAsync(Class<T> responseClass, Callback<T> callback) {
        return clientRequest.requestAsync(httpRequest,responseClass, callback);
    }

    public HttpResponse<InputStream> asBinary() throws RestClientException {
        return clientRequest.request(httpRequest, InputStream.class);
    }

    public CompletableFuture<HttpResponse<InputStream>> asBinaryAsync() {
        return clientRequest.requestAsync(httpRequest, InputStream.class);
    }

    public Future<HttpResponse<InputStream>> asBinaryAsync(Callback<InputStream> callback) {
        return clientRequest.requestAsync(httpRequest, InputStream.class, callback);
    }

}
