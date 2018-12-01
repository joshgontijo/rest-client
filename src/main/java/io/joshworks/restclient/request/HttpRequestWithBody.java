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
import io.joshworks.restclient.http.JsonNode;
import io.joshworks.restclient.http.MediaType;
import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.http.mapper.ObjectMappers;
import io.joshworks.restclient.request.body.FormEncodedBody;
import io.joshworks.restclient.request.body.MultipartBody;
import io.joshworks.restclient.request.body.RawBody;
import io.joshworks.restclient.request.body.RequestBodyEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public class HttpRequestWithBody extends HttpRequest {

    private final ClientRequest config;

    public HttpRequestWithBody(ClientRequest clientRequest) {
        super(clientRequest);
        this.config = clientRequest;
    }

    @Override
    public HttpRequestWithBody routeParam(String name, String value) {
        super.routeParam(name, value);
        return this;
    }

    @Override
    public HttpRequestWithBody contentType(String contentType) {
        return (HttpRequestWithBody) super.contentType(contentType);
    }

    @Override
    public HttpRequestWithBody contentType(MediaType contentType) {
        return (HttpRequestWithBody) super.contentType(contentType);
    }

    @Override
    public HttpRequestWithBody header(String name, Long value) {
        return (HttpRequestWithBody) super.header(name, value);
    }

    @Override
    public HttpRequestWithBody header(String name, Integer value) {
        return (HttpRequestWithBody) super.header(name, value);
    }

    @Override
    public HttpRequestWithBody header(String name, Double value) {
        return (HttpRequestWithBody) super.header(name, value);
    }

    @Override
    public HttpRequestWithBody header(String name, String value) {
        return (HttpRequestWithBody) super.header(name, value);
    }

    @Override
    public HttpRequestWithBody headers(Map<String, String> headers) {
        return (HttpRequestWithBody) super.headers(headers);
    }

    @Override
    public HttpRequestWithBody basicAuth(String username, String password) {
        return (HttpRequestWithBody) super.basicAuth(username, password);
    }

    @Override
    public HttpRequestWithBody queryString(Map<String, Object> parameters) {
        return (HttpRequestWithBody) super.queryString(parameters);
    }

    @Override
    public HttpRequestWithBody queryString(String name, Object value) {
        return (HttpRequestWithBody) super.queryString(name, value);
    }

    @Override
    public HttpRequestWithBody queryString(String name, Collection<?> value) {
        return (HttpRequestWithBody) super.queryString(name, value);
    }

    //-------------- form params
    public FormEncodedBody field(String name, String value) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, value);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody field(String name, Integer value) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, value);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody field(String name, Long value) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, value);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody field(String name, Boolean value) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, value);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody field(String name, Double value) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, value);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody fields(Map<String, Object> parameters) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).fields(parameters);
        this.body = formBody;
        return formBody;
    }

    public FormEncodedBody field(String name, Collection<Object> values) {
        FormEncodedBody formBody = new FormEncodedBody(this, config).field(name, values);
        this.body = formBody;
        return formBody;
    }

    //------------ multipart
    public MultipartBody part(String name, String value) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Integer value) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Long value) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Boolean value) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Double value) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, String value, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Integer value, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Long value, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Boolean value, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, Double value, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, value, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, File file) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, file);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, File file, String contentType) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, file, contentType);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, InputStream stream, String fileName) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, stream, fileName);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, InputStream stream, String contentType, String fileName) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, stream, contentType, fileName);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, byte[] bytes, String fileName) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, bytes, fileName);
        this.body = partBody;
        return partBody;
    }

    public MultipartBody part(String name, byte[] bytes, String contentType, String fileName) {
        MultipartBody partBody = new MultipartBody(this, config).part(name, bytes, contentType, fileName);
        this.body = partBody;
        return partBody;
    }

    public RequestBodyEntity body(JsonNode body) {
        return body(body.toString());
    }

    public RequestBodyEntity body(String body) {
        RequestBodyEntity b = new RequestBodyEntity(this, config).body(body);
        this.body = b;
        return b;
    }

    public RequestBodyEntity body(Object body) {
        MediaType mediaType = getContentType();
        ObjectMapper mapper = ObjectMappers.getMapper(mediaType);
        if (mapper == null) {
            throw new RuntimeException("Cannot serialize object. No ObjectMapper implementation for type: " + mediaType.toString());
        }

        return body(mapper.writeValue(body));
    }

    public RawBody body(byte[] body) {
        RawBody b = new RawBody(this, config).body(body);
        this.body = b;
        return b;
    }

    /**
     * Sugar method for body operation
     *
     * @param body raw org.JSONObject
     * @return RequestBodyEntity instance
     */
    public RequestBodyEntity body(JSONObject body) {
        return body(body.toString());
    }

    /**
     * Sugar method for body operation
     *
     * @param body raw org.JSONArray
     * @return RequestBodyEntity instance
     */
    public RequestBodyEntity body(JSONArray body) {
        return body(body.toString());
    }

}