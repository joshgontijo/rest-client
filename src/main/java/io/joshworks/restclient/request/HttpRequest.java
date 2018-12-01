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

import io.joshworks.restclient.Constants;
import io.joshworks.restclient.http.ClientRequest;
import io.joshworks.restclient.http.HttpMethod;
import io.joshworks.restclient.http.MediaType;
import io.joshworks.restclient.http.utils.Base64Coder;
import io.joshworks.restclient.http.utils.MimeMappings;
import io.joshworks.restclient.http.utils.URLParamEncoder;
import io.joshworks.restclient.request.body.Body;
import org.apache.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequest extends BaseRequest {

    private static final MimeMappings mappings = MimeMappings.builder().build();

    private String url;
    protected Body body;
    private Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
    private HttpMethod httpMethod;

    public HttpRequest(ClientRequest clientRequest) {
        super(clientRequest);
        this.url = clientRequest.url;
        this.httpMethod = clientRequest.httpMethod;
        super.httpRequest = this;

    }

    public HttpRequest routeParam(String name, String value) {
        Matcher matcher = Pattern.compile("\\{" + name + "\\}").matcher(url);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        if (count == 0) {
            throw new RuntimeException("Can't find route parameter name \"" + name + "\"");
        }
        this.url = url.replaceAll("\\{" + name + "\\}", URLParamEncoder.encode(value));
        return this;
    }

    public HttpRequest basicAuth(String username, String password) {
        header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Coder.encodeString(username + ":" + password));
        return this;
    }

    public HttpRequest header(String name, Long value) {
        return header(name, value == null ? null : String.valueOf(value));
    }

    public HttpRequest header(String name, Integer value) {
        return header(name, value == null ? null : String.valueOf(value));
    }

    public HttpRequest header(String name, Double value) {
        return header(name, value == null ? null : String.valueOf(value));
    }

    public HttpRequest header(String name, String value) {
        List<String> list = this.headers.get(name.trim());
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(value);
        this.headers.put(name.trim(), list);
        return this;
    }

    public HttpRequest contentType(MediaType contentType) {
        if (contentType == null) {
            return this;
        }
        return this.contentType(contentType.toString());
    }

    public HttpRequest contentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return this;
        }
        if (!contentType.contains("/")) {
            String mimeType = mappings.getMimeType(contentType);
            if (mimeType != null) {
                return this.header(HttpHeaders.CONTENT_TYPE, mimeType);
            }
        }

        this.header(HttpHeaders.CONTENT_TYPE, contentType);
        return this;
    }

    public HttpRequest headers(Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public HttpRequest queryString(String name, Collection<?> value) {
        for (Object cur : value) {
            queryString(name, cur);
        }
        return this;
    }

    public HttpRequest queryString(String name, Object value) {
        StringBuilder queryString = new StringBuilder();
        if (this.url.contains(Constants.QUESTION_MARK)) {
            queryString.append(Constants.AMPERSAND);
        } else {
            queryString.append(Constants.QUESTION_MARK);
        }
        try {
            queryString
                    .append(URLEncoder.encode(name, Constants.UTF_8))
                    .append(Constants.EQUALS)
                    .append(URLEncoder.encode(value == null ? "" : value.toString(), Constants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        this.url += queryString.toString();
        return this;
    }

    public HttpRequest queryString(Map<String, Object> parameters) {
        if (parameters != null) {
            for (Entry<String, Object> param : parameters.entrySet()) {
                if (param.getValue() instanceof String || param.getValue() instanceof Number || param.getValue() instanceof Boolean) {
                    queryString(param.getKey(), param.getValue());
                } else {
                    throw new RuntimeException("Parameter \"" + param.getKey() + "\" can't be sent with a GET request because of type: " + param.getValue().getClass().getName());
                }
            }
        }
        return this;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        if (headers == null)
            return new HashMap<>();
        return headers;
    }

    public Body getBody() {
        return body;
    }

    protected MediaType getContentType() {
        List<String> types = this.headers.get(HttpHeaders.CONTENT_TYPE);
        if (types == null || types.isEmpty()) {
            throw new IllegalStateException("Content-Type not specified");
        }
        //Content-type is always a single value
        String type = types.get(0);
        return MediaType.valueOf(type);
    }

}
