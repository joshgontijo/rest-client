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

package io.joshworks.restclient.request.async;

import io.joshworks.restclient.http.ClientRequest;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.exceptions.RestClientException;
import io.joshworks.restclient.request.HttpRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class AsyncRequest<T> {

    private final ClientRequest clientRequest;
    private final HttpRequest httpRequest;
    private final Class<T> responseType;

    private Consumer<HttpResponse<T>> completed = (v) -> {
    };
    private Consumer<Exception> failed = Throwable::printStackTrace;

    private final Map<Integer, Consumer<HttpResponse<T>>> statusCodeHandlers = new HashMap<>();

    public AsyncRequest(ClientRequest clientRequest, HttpRequest httpRequest, Class<T> responseType) {

        this.clientRequest = clientRequest;
        this.httpRequest = httpRequest;
        this.responseType = responseType;
    }

    public AsyncRequest<T> completed(Consumer<HttpResponse<T>> onComplete) {
        Objects.requireNonNull(onComplete, "Function cannot be null");
        this.completed = onComplete;
        return this;
    }

    public AsyncRequest<T> failed(Consumer<Exception> onError) {
        Objects.requireNonNull(onError, "Function cannot be null");
        this.failed = onError;
        return this;
    }

    public void request() {
        clientRequest.requestAsync(httpRequest, responseType, new Callback<T>() {
            @Override
            public void completed(HttpResponse<T> response) {
                completed.accept(response);
                statusCodeHandlers.getOrDefault(response.getStatus(), (r) -> {
                }).accept(response);
            }

            @Override
            public void failed(RestClientException e) {
                failed.accept(e);
            }

            @Override
            public void cancelled() {

            }
        });
    }

}