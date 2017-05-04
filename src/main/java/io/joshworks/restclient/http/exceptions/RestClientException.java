package io.joshworks.restclient.http.exceptions;

public class RestClientException extends RuntimeException {

    public RestClientException(Exception e) {
        super(e);
    }
}
