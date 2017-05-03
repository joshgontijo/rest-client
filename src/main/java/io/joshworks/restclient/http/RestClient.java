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

import io.joshworks.restclient.http.async.utils.AsyncIdleConnectionMonitorThread;
import com.mashape.unirest.http.options.Option;
import io.joshworks.restclient.http.options.Options;
import io.joshworks.restclient.http.utils.SyncIdleConnectionMonitorThread;
import io.joshworks.restclient.request.GetRequest;
import io.joshworks.restclient.request.HttpRequestWithBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.IOException;

public class RestClient {

    /**
     * Close the asynchronous client and its event loop. Use this method to close all the threads and allow an application to exit.
     */
    public void shutdown() throws IOException {
        // Closing the Sync HTTP client
        CloseableHttpClient syncClient = (CloseableHttpClient) Options.getOption(Option.HTTPCLIENT);
        if (syncClient != null) {
            syncClient.close();
        }

        SyncIdleConnectionMonitorThread syncIdleConnectionMonitorThread = (SyncIdleConnectionMonitorThread) Options.getOption(Option.SYNC_MONITOR);
        if (syncIdleConnectionMonitorThread != null) {
            syncIdleConnectionMonitorThread.interrupt();
        }

        // Closing the Async HTTP client (if running)
        CloseableHttpAsyncClient asyncClient = (CloseableHttpAsyncClient) Options.getOption(Option.ASYNCHTTPCLIENT);
        if (asyncClient != null && asyncClient.isRunning()) {
            asyncClient.close();
        }

        AsyncIdleConnectionMonitorThread asyncMonitorThread = (AsyncIdleConnectionMonitorThread) Options.getOption(Option.ASYNC_MONITOR);
        if (asyncMonitorThread != null) {
            asyncMonitorThread.interrupt();
        }
    }

    public static GetRequest get(String url) {
        return new GetRequest(HttpMethod.GET, url);
    }

    public static GetRequest head(String url) {
        return new GetRequest(HttpMethod.HEAD, url);
    }

    public static HttpRequestWithBody options(String url) {
        return new HttpRequestWithBody(HttpMethod.OPTIONS, url);
    }

    public static HttpRequestWithBody post(String url) {
        return new HttpRequestWithBody(HttpMethod.POST, url);
    }

    public static HttpRequestWithBody delete(String url) {
        return new HttpRequestWithBody(HttpMethod.DELETE, url);
    }

    public static HttpRequestWithBody patch(String url) {
        return new HttpRequestWithBody(HttpMethod.PATCH, url);
    }

    public static HttpRequestWithBody put(String url) {
        return new HttpRequestWithBody(HttpMethod.PUT, url);
    }

    public static class Builder {


    }

}
