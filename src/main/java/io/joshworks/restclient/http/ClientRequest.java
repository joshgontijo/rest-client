package io.joshworks.restclient.http;

import io.joshworks.restclient.Constants;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.exceptions.RestClientException;
import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.entity.NByteArrayEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Josh Gontijo on 5/3/17.
 */
public class ClientRequest {

    private final CloseableHttpClient syncClient;
    private final CloseableHttpAsyncClient asyncClient;
    private final Map<String, Object> defaultHeaders;
    public final ObjectMapper objectMapper;
    public final String url;
    public final HttpMethod httpMethod;

    ClientRequest(HttpMethod httpMethod, String url, CloseableHttpClient syncClient, CloseableHttpAsyncClient asyncClient, Map<String, Object> defaultHeaders, ObjectMapper objectMapper) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.syncClient = syncClient;
        this.asyncClient = asyncClient;
        this.defaultHeaders = defaultHeaders;
        this.objectMapper = objectMapper;
    }

    private static final String USER_AGENT = "rest-client/1.5.1";

    private static <T> FutureCallback<org.apache.http.HttpResponse> prepareCallback(
            final Class<T> responseClass,
            final Callback<T> callback,
            final ObjectMapper objectMapper) {
        if (callback == null)
            return null;

        return new FutureCallback<org.apache.http.HttpResponse>() {

            public void cancelled() {
                callback.cancelled();
            }

            public void completed(org.apache.http.HttpResponse arg0) {
                callback.completed(new HttpResponse<>(arg0, responseClass, objectMapper));
            }

            public void failed(Exception arg0) {
                callback.failed(new RestClientException(arg0));
            }

        };
    }

    public <T> CompletableFuture<HttpResponse<T>> requestAsync(HttpRequest request, final Class<T> responseClass) {

        HttpUriRequest requestObj = prepareRequest(request, true);

        if (!asyncClient.isRunning()) {
            asyncClient.start();
        }

        CompletableFuture<HttpResponse<T>> completableFuture = new CompletableFuture<>();
        asyncClient.execute(requestObj, prepareCallback(responseClass, new Callback<T>() {
            @Override
            public void completed(HttpResponse<T> response) {
                completableFuture.complete(response);
            }

            @Override
            public void failed(RestClientException e) {
                completableFuture.completeExceptionally(e);
            }

            @Override
            public void cancelled() {
                //do nothing
            }
        }, objectMapper));

        return completableFuture;
    }

    public <T> Future<HttpResponse<T>> requestAsync(
            HttpRequest request,
            final Class<T> responseClass,
            Callback<T> callback) {

        HttpUriRequest requestObj = prepareRequest(request, true);

        if (!asyncClient.isRunning()) {
            asyncClient.start();
        }

        final Future<org.apache.http.HttpResponse> future = asyncClient.execute(requestObj, prepareCallback(responseClass, callback, objectMapper));

        return new Future<HttpResponse<T>>() {

            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            public boolean isCancelled() {
                return future.isCancelled();
            }

            public boolean isDone() {
                return future.isDone();
            }

            public HttpResponse<T> get() throws InterruptedException, ExecutionException {
                return this.getResponse();
            }

            public HttpResponse<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return this.getResponse(timeout, unit);
            }

            private HttpResponse<T> getResponse() throws ExecutionException, InterruptedException {
                org.apache.http.HttpResponse httpResponse = future.get();
                return new HttpResponse<>(httpResponse, responseClass, objectMapper);
            }

            private HttpResponse<T> getResponse(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                org.apache.http.HttpResponse httpResponse = future.get(timeout, unit);
                return new HttpResponse<>(httpResponse, responseClass, objectMapper);
            }
        };
    }

    public <T> HttpResponse<T> request(final HttpRequest request, final Class<T> responseClass) {
        return this.doRequest(request, responseClass);
    }

    private <T> HttpResponse<T> doRequest(HttpRequest request, Class<T> responseClass) {
        HttpRequestBase requestObj = prepareRequest(request, false);
        org.apache.http.HttpResponse response;
        try {

            response = syncClient.execute(requestObj);
            HttpResponse<T> httpResponse = new HttpResponse<>(response, responseClass, objectMapper);
            requestObj.releaseConnection();
            return httpResponse;
        } catch (Exception e) {
            throw new RestClientException(e);
        } finally {
            requestObj.releaseConnection();
        }

    }


    private HttpRequestBase prepareRequest(HttpRequest request, boolean async) {

        if (defaultHeaders != null) {
            for (Map.Entry<String, Object> entry : defaultHeaders.entrySet()) {
                request.header(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        if (!request.getHeaders().containsKey(HttpHeaders.USER_AGENT)) {
            request.header(HttpHeaders.USER_AGENT, USER_AGENT);
        }
        if (!request.getHeaders().containsKey(HttpHeaders.ACCEPT_ENCODING)) {
            request.header(HttpHeaders.ACCEPT_ENCODING, Constants.GZIP);
        }

        String urlToRequest;
        try {
            URL url = new URL(request.getUrl());
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), URLDecoder.decode(url.getPath(), Constants.UTF_8), "", url.getRef());
            urlToRequest = uri.toURL().toString();
            if (url.getQuery() != null && !url.getQuery().trim().equals("")) {
                if (!urlToRequest.substring(urlToRequest.length() - 1).equals(Constants.QUESTION_MARK)) {
                    urlToRequest += Constants.QUESTION_MARK;
                }
                urlToRequest += url.getQuery();
            } else if (urlToRequest.substring(urlToRequest.length() - 1).equals(Constants.QUESTION_MARK)) {
                urlToRequest = urlToRequest.substring(0, urlToRequest.length() - 1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final HttpRequestBase reqObj = newRequestObject(request, urlToRequest, async);

        Set<Map.Entry<String, List<String>>> entrySet = request.getHeaders().entrySet();
        for (Map.Entry<String, List<String>> entry : entrySet) {
            List<String> values = entry.getValue();
            if (values != null) {
                for (String value : values) {
                    reqObj.addHeader(entry.getKey(), value);
                }
            }
        }

        return reqObj;
    }

    private HttpRequestBase newRequestObject(HttpRequest request, String urlToRequest, boolean async) {
        switch (request.getHttpMethod()) {
            case GET:
                return new HttpGet(urlToRequest);
            case HEAD:
                return new HttpHead(urlToRequest);
            case POST:
                return withBody(new HttpPost(urlToRequest), request, async);
            case PUT:
                return withBody(new HttpPut(urlToRequest), request, async);
            case DELETE:
                return withBody(new HttpDeleteWithBody(urlToRequest), request, async);
            case PATCH:
                return withBody(new HttpPatchWithBody(urlToRequest), request, async);
            case OPTIONS:
                return withBody(new HttpOptionsWithBody(urlToRequest), request, async);
            default:
                throw new IllegalArgumentException("Unknown request method: " + request.getHttpMethod());
        }
    }

    private HttpRequestBase withBody(HttpEntityEnclosingRequestBase request, HttpRequest original, boolean async) {
        // Set body
        if (original.getBody() != null) {
            HttpEntity entity = original.getBody().getEntity();
            if (async) {
                if (request.getHeaders(HttpHeaders.CONTENT_TYPE) == null || request.getHeaders(HttpHeaders.CONTENT_TYPE).length == 0) {
                    request.setHeader(entity.getContentType());
                }
                try {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    entity.writeTo(output);
                    NByteArrayEntity en = new NByteArrayEntity(output.toByteArray());
                    request.setEntity(en);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                request.setEntity(entity);
            }
        }
        return request;
    }
}
