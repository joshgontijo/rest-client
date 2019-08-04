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
import io.joshworks.restclient.http.Json;
import io.joshworks.restclient.http.Response;
import io.joshworks.restclient.http.async.Callback;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class BaseRequest {

    protected Request request;
    protected final ClientRequest clientRequest;

    protected BaseRequest(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    public Request getRequest() {
        return this.request;
    }

    public Response<String> asString() {
        return clientRequest.request(request, String.class);
    }

    public CompletableFuture<Response<String>> asStringAsync() {
        return clientRequest.requestAsync(request, String.class);
    }

    public void asStringAsync(Callback<String> callback) {
        clientRequest.requestAsync(request, String.class, callback);
    }

    public Response<Json> asJson() {
        return clientRequest.request(request, Json.class);
    }

    public CompletableFuture<Response<Json>> asJsonAsync() {
        return clientRequest.requestAsync(request, Json.class);
    }

    public Future<Response<Json>> asJsonAsync(Callback<Json> callback) {
        return clientRequest.requestAsync(request, Json.class, callback);
    }

    public <T> Response<T> asObject(Class<T> responseClass) {
        return clientRequest.request(request, responseClass);
    }

    public <T> CompletableFuture<Response<T>> asObjectAsync(Class<T> responseClass) {
        return clientRequest.requestAsync(request, responseClass);
    }

    public <T> Future<Response<T>> asObjectAsync(Class<T> responseClass, Callback<T> callback) {
        return clientRequest.requestAsync(request, responseClass, callback);
    }

    public Response<InputStream> asBinary() {
        return clientRequest.request(request, InputStream.class);
    }

    public CompletableFuture<Response<InputStream>> asBinaryAsync() {
        return clientRequest.requestAsync(request, InputStream.class);
    }

    public Future<Response<InputStream>> asBinaryAsync(Callback<InputStream> callback) {
        return clientRequest.requestAsync(request, InputStream.class, callback);
    }

}
