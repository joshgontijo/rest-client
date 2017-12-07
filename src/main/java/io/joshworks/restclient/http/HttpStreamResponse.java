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

import io.joshworks.restclient.http.exceptions.RestClientException;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;

class HttpStreamResponse<T> extends HttpResponse<T> {

    private final HttpRequestBase request;

    HttpStreamResponse(org.apache.http.HttpResponse response, Class<T> responseClass, HttpRequestBase request) {
        super(response, responseClass);
        this.request = request;
    }

    @Override
    public T getBody() {
        return (T) super.rawBody;
    }

    @Override
    protected InputStream consumeBody(org.apache.http.HttpResponse response) {
        try {
            return response.getEntity().getContent();
        } catch (IOException e) {
            throw new RestClientException(e);
        }
    }

    @Override
    public void close() {
       super.close();
       request.releaseConnection();
    }
}
