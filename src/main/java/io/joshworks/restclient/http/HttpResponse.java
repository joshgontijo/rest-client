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

import io.joshworks.restclient.Constants;
import io.joshworks.restclient.http.exceptions.RestClientException;
import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.http.mapper.ObjectMappers;
import io.joshworks.restclient.http.utils.ResponseUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class HttpResponse<T> implements Closeable {

    private final int statusCode;
    private final String statusText;
    private final Headers headers;
    protected final InputStream rawBody;
    private final Class<T> responseClass;
    private T body;

    HttpResponse(org.apache.http.HttpResponse response, Class<T> responseClass) {
        this.headers = responseHeaders(response);
        this.rawBody = consumeBody(response);
        this.responseClass = responseClass;

        StatusLine statusLine = response.getStatusLine();
        if (statusLine != null) {
            this.statusCode = statusLine.getStatusCode();
            this.statusText = statusLine.getReasonPhrase();
        } else {
            this.statusCode = -1;
            this.statusText = "NOT_SET";
        }
    }

    public static <T> HttpResponse<T> create(HttpRequestBase request, org.apache.http.HttpResponse response, Class<T> responseClass) {
        if (responseClass == InputStream.class) {
            return new HttpStreamResponse<>(response, responseClass, request);
        }
        return new HttpResponse<>(response, responseClass);
    }


    protected InputStream consumeBody(org.apache.http.HttpResponse response) {
        HttpEntity responseEntity = response.getEntity();
        try {
            if (responseEntity != null) {
                InputStream entity = getEntity(responseEntity);
                return new ByteArrayInputStream(ResponseUtils.readBytes(entity));
            }
            return null;
        } catch (IOException ex) {
            throw new RestClientException(ex);
        } finally {
            EntityUtils.consumeQuietly(responseEntity);
        }
    }

    private InputStream getEntity(HttpEntity responseEntity) throws IOException {
        if (ResponseUtils.isGzipped(responseEntity.getContentEncoding())) {
            return new GZIPInputStream(responseEntity.getContent());
        }
        return responseEntity.getContent();
    }

    private Headers responseHeaders(org.apache.http.HttpResponse response) {
        Headers headers = new Headers();
        Header[] allHeaders = response.getAllHeaders();
        for (Header header : allHeaders) {
            String headerName = header.getName();
            List<String> list = headers.get(headerName);
            if (list == null)
                list = new ArrayList<>();
            list.add(header.getValue());
            headers.put(headerName, list);
        }
        return headers;
    }

    public int getStatus() {
        return statusCode;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300 || statusCode == 304;
    }

    public String getStatusText() {
        return statusText;
    }

    /**
     * @return Response Headers (map) with <b>same case</b> as server response.
     * For instance use <code>getHeaders().getFirst("Location")</code> and not <code>getHeaders().getFirst("location")</code> to get first header "Location"
     */
    public Headers getHeaders() {
        return headers;
    }

    public InputStream getRawBody() {
        return rawBody;
    }

    public T getBody() {
        if (this.body == null) {
            this.body = parseBody();
        }
        return body;
    }

    private T parseBody() {
        if (InputStream.class.equals(responseClass)) {
            return (T) this.rawBody;
        }

        String bodyString = readBodyAsString();

        if (JsonNode.class.equals(responseClass)) {
            return (T) new JsonNode(bodyString);
        } else if (String.class.equals(responseClass)) {
            return (T) bodyString;
        } else {
            return getObjectMapper().readValue(bodyString, responseClass);
        }
    }

    private String readBodyAsString() {
        try {
            if (this.rawBody == null) {
                return null;
            }
            String charset = getCharset();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(this.rawBody, charset));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RestClientException(e);
        }
    }

    private String getCharset() {
        String charset = Constants.UTF_8;

        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            String responseCharset = ResponseUtils.getCharsetFromContentType(contentType);
            if (responseCharset != null && !responseCharset.trim().equals("")) {
                charset = responseCharset;
            }
        }
        return charset;
    }

    private ObjectMapper getObjectMapper() {
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return null;
        }
        MediaType mediaType = MediaType.valueOf(contentType);
        ObjectMapper mapper = ObjectMappers.getMapper(mediaType);
        if (mapper == null) {
            throw new RestClientException("No ObjectMapper found for response with Content-Type: " + contentType);
        }
        return mapper;
    }

    @Override
    public void close() {
        if (rawBody != null) {
            try {
                rawBody.close();
            } catch (IOException ignore) {

            }
        }
    }
}
