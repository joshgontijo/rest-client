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

package io.joshworks.restclient.request.body;

import io.joshworks.restclient.http.ClientRequest;
import io.joshworks.restclient.http.utils.MimeMappings;
import io.joshworks.restclient.request.BaseRequest;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MultipartBody extends BaseRequest implements Body {

    private static final MimeMappings mimeMappings = MimeMappings.builder().build();

    private Map<String, List<AbstractContentBody>> parameters = new LinkedHashMap<String, List<AbstractContentBody>>();

    private HttpMultipartMode mode = HttpMultipartMode.STRICT;

    public MultipartBody(HttpRequest httpRequest, ClientRequest config) {
        super(config);
        super.httpRequest = httpRequest;
        httpRequest.header(HttpHeaders.CONTENT_TYPE, ContentType.MULTIPART_FORM_DATA.getMimeType());
    }

    public MultipartBody part(String name, String value) {
        return part(name, value, ContentType.TEXT_PLAIN.getMimeType());
    }

    public MultipartBody part(String name, Integer value) {
        return part(name, value, ContentType.TEXT_PLAIN.getMimeType());
    }

    public MultipartBody part(String name, Long value) {
        return part(name, value, ContentType.TEXT_PLAIN.getMimeType());
    }

    public MultipartBody part(String name, Boolean value) {
        return part(name, value, ContentType.TEXT_PLAIN.getMimeType());
    }

    public MultipartBody part(String name, Double value) {
        return part(name, value, ContentType.TEXT_PLAIN.getMimeType());
    }

    public MultipartBody part(String name, String value, String contentType) {
        return addPart(name, new StringBody(value, ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8)));
    }

    public MultipartBody part(String name, Integer value, String contentType) {
        return addPart(name, new StringBody(String.valueOf(value), ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8)));
    }

    public MultipartBody part(String name, Long value, String contentType) {
        return addPart(name, new StringBody(String.valueOf(value), ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8)));
    }

    public MultipartBody part(String name, Boolean value, String contentType) {
        return addPart(name, new StringBody(String.valueOf(value), ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8)));
    }

    public MultipartBody part(String name, Double value, String contentType) {
        return addPart(name, new StringBody(String.valueOf(value), ContentType.parse(contentType).withCharset(StandardCharsets.UTF_8)));
    }

    public MultipartBody part(String name, File file) {
        String fileMime = getMimeForFile(file);
        return addPart(name, new FileBody(file, ContentType.parse(fileMime), file.getName()));
    }

    public MultipartBody part(String name, File file, String contentType) {
        return addPart(name, new FileBody(file, ContentType.parse(contentType), file.getName()));
    }

    public MultipartBody part(String name, InputStream stream, String fileName) {
        ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;
        return addPart(name, new InputStreamBody(stream, contentType, fileName));
    }

    public MultipartBody part(String name, InputStream stream, String contentType, String fileName) {
        return addPart(name, new InputStreamBody(stream, ContentType.parse(contentType), fileName));
    }

    public MultipartBody part(String name, byte[] bytes, String fileName) {
        ContentType contentType = ContentType.APPLICATION_OCTET_STREAM;
        return addPart(name, new ByteArrayBody(bytes, contentType, fileName));
    }

    public MultipartBody part(String name, byte[] bytes, String contentType, String fileName) {
        return addPart(name, new ByteArrayBody(bytes, ContentType.parse(contentType), fileName));
    }

    private MultipartBody addPart(String name, AbstractContentBody value) {
        List<AbstractContentBody> list = parameters.get(name);
        if (list == null)
            list = new LinkedList<>();
        list.add(value);
        parameters.put(name, list);

        return this;
    }

    public MultipartBody mode(HttpMultipartMode mode) {
        this.mode = mode;
        return this;
    }

    public HttpEntity getEntity() {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(mode);

        for (String key : parameters.keySet()) {
            List<AbstractContentBody> value = parameters.get(key);
            for (AbstractContentBody cur : value) {
                builder.addPart(key, cur);
            }
        }
        return builder.build();
    }

    private String getMimeForFile(File file) {
        if (file == null) {
            return null;
        }
        String[] split = file.getName().split(".");
        if (split.length == 0) {
            return ContentType.APPLICATION_OCTET_STREAM.getMimeType();
        }
        String ext = split[split.length - 1];

        return mimeMappings.getMimeType(ext);
    }

}
