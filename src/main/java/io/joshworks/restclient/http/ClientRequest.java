package io.joshworks.restclient.http;

import io.joshworks.restclient.Constants;
import io.joshworks.restclient.http.async.Callback;
import io.joshworks.restclient.http.exceptions.RestClientException;
import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
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

    ClientRequest(HttpMethod httpMethod, String url, RestClient.Configuration config) {

        this.syncClient = config.getSyncClient();
        this.asyncClient = config.getAsyncClient();
        this.defaultHeaders = config.getDefaultHeaders();
        this.objectMapper = config.getObjectMapper();
        this.url = url;
        this.httpMethod = httpMethod;
    }

    private static final String CONTENT_TYPE = "content-type";
    private static final String ACCEPT_ENCODING_HEADER = "accept-encoding";
    private static final String USER_AGENT_HEADER = "user-agent";
    private static final String USER_AGENT = "restclient-java/1.3.11";

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
                org.apache.http.HttpResponse httpResponse = future.get();
                return new HttpResponse<>(httpResponse, responseClass, objectMapper);
            }

            public HttpResponse<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                org.apache.http.HttpResponse httpResponse = future.get(timeout, unit);
                return new HttpResponse<>(httpResponse, responseClass, objectMapper);
            }
        };
    }

    public <T> HttpResponse<T> request(HttpRequest request, Class<T> responseClass) {

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

        if (!request.getHeaders().containsKey(USER_AGENT_HEADER)) {
            request.header(USER_AGENT_HEADER, USER_AGENT);
        }
        if (!request.getHeaders().containsKey(ACCEPT_ENCODING_HEADER)) {
            request.header(ACCEPT_ENCODING_HEADER, Constants.GZIP);
        }

        HttpRequestBase reqObj = null;

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

        switch (request.getHttpMethod()) {
            case GET:
                reqObj = new HttpGet(urlToRequest);
                break;
            case POST:
                reqObj = new HttpPost(urlToRequest);
                break;
            case PUT:
                reqObj = new HttpPut(urlToRequest);
                break;
            case DELETE:
                reqObj = new HttpDeleteWithBody(urlToRequest);
                break;
            case PATCH:
                reqObj = new HttpPatchWithBody(urlToRequest);
                break;
            case OPTIONS:
                reqObj = new HttpOptions(urlToRequest);
                break;
            case HEAD:
                reqObj = new HttpHead(urlToRequest);
                break;
        }

        Set<Map.Entry<String, List<String>>> entrySet = request.getHeaders().entrySet();
        for (Map.Entry<String, List<String>> entry : entrySet) {
            List<String> values = entry.getValue();
            if (values != null) {
                for (String value : values) {
                    reqObj.addHeader(entry.getKey(), value);
                }
            }
        }

        // Set body
        if (!(request.getHttpMethod() == HttpMethod.GET || request.getHttpMethod() == HttpMethod.HEAD)) {
            if (request.getBody() != null) {
                HttpEntity entity = request.getBody().getEntity();
                if (async) {
                    if (reqObj.getHeaders(CONTENT_TYPE) == null || reqObj.getHeaders(CONTENT_TYPE).length == 0) {
                        reqObj.setHeader(entity.getContentType());
                    }
                    try {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        entity.writeTo(output);
                        NByteArrayEntity en = new NByteArrayEntity(output.toByteArray());
                        ((HttpEntityEnclosingRequestBase) reqObj).setEntity(en);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    ((HttpEntityEnclosingRequestBase) reqObj).setEntity(entity);
                }
            }
        }

        return reqObj;
    }
}
