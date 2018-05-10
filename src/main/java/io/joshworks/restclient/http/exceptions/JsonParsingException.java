package io.joshworks.restclient.http.exceptions;

public class JsonParsingException extends RuntimeException {

    public JsonParsingException() {
    }

    public JsonParsingException(String message) {
        super(message);
    }

    public JsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonParsingException(Throwable cause) {
        super(cause);
    }
}
