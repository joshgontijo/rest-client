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
import io.joshworks.restclient.request.BaseRequest;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.InputStream;

public class MultipartBody extends BaseRequest implements Body {

    private final MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    public MultipartBody(HttpRequest httpRequest, ClientRequest config) {
        super(config);
        super.httpRequest = httpRequest;
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
        builder.addTextBody(name, value, ContentType.parse(contentType));
        return this;
    }

    public MultipartBody part(String name, Integer value, String contentType) {
        builder.addTextBody(name, String.valueOf(value), ContentType.parse(contentType));
        return this;
    }

    public MultipartBody part(String name, Long value, String contentType) {
        builder.addTextBody(name, String.valueOf(value), ContentType.parse(contentType));
        return this;
    }

    public MultipartBody part(String name, Boolean value, String contentType) {
        builder.addTextBody(name, String.valueOf(value), ContentType.parse(contentType));
        return this;
    }

    public MultipartBody part(String name, Double value, String contentType) {
        builder.addTextBody(name, String.valueOf(value), ContentType.parse(contentType));
        return this;
    }

    public MultipartBody part(String name, File file) {
        builder.addBinaryBody(name, file);
        return this;
    }

    public MultipartBody part(String name, File file, String contentType) {
        builder.addBinaryBody(name, file, ContentType.parse(contentType), file.getName());
        return this;
    }

    public MultipartBody part(String name, InputStream inputStream, String fileName) {
        builder.addBinaryBody(name, inputStream, ContentType.APPLICATION_OCTET_STREAM, fileName);
        return this;
    }

    public MultipartBody part(String name, InputStream inputStream, String contentType, String fileName) {
        builder.addBinaryBody(name, inputStream, ContentType.parse(contentType), fileName);
        return this;
    }

    public MultipartBody part(String name, byte[] bytes, String fileName) {
        builder.addBinaryBody(name, bytes, ContentType.APPLICATION_OCTET_STREAM, fileName);
        return this;
    }

    public MultipartBody part(String name, byte[] bytes, String contentType, String fileName) {
        builder.addBinaryBody(name, bytes, ContentType.parse(contentType), fileName);
        return this;
    }

    public MultipartBody mode(HttpMultipartMode mode) {
        builder.setMode(mode);
        return this;
    }

    public HttpEntity getEntity() {
        return builder.build();
    }

    @Override
    public boolean implicitContentType() {
        return true;
    }

}
