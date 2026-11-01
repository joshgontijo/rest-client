package io.joshworks.restclient.http;

import io.joshworks.restclient.http.exceptions.RestClientException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HttpResponseTest {

    @Test
    public void binaryResponseWithNoEntityReturnsNullBody() {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 204, "No Content");
        HttpGet request = new HttpGet("http://localhost/");

        HttpResponse<InputStream> responseWithNoBody = HttpResponse.create(request, response, InputStream.class);

        assertNull(responseWithNoBody.body());
        responseWithNoBody.close();
    }

    @Test
    public void unknownContentTypeHasClearError() {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-unknown");
        response.setEntity(new StringEntity("{}", "UTF-8"));
        HttpGet request = new HttpGet("http://localhost/");

        HttpResponse<Json> responseWithUnknownType = HttpResponse.create(request, response, Json.class);

        RestClientException exception = assertThrows(RestClientException.class,
                () -> responseWithUnknownType.as(Map.class));
        assertTrue(exception.getMessage().contains("application/x-unknown"));
    }

    @Test
    public void successfulStatusCodes() {
        assertTrue(response(200).successful());
        assertTrue(response(204).successful());
        assertTrue(response(304).successful());
        assertFalse(response(400).successful());
    }

    private HttpResponse<String> response(int status) {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, "Status");
        return HttpResponse.create(new HttpGet("http://localhost/"), response, String.class);
    }
}
