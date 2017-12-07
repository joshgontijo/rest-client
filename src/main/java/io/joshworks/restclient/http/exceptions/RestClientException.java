package io.joshworks.restclient.http.exceptions;

public class RestClientException extends RuntimeException {

    public RestClientException(String message) {
        super(message);
    }

    public RestClientException(Exception e) {
        super(e);
    }

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
