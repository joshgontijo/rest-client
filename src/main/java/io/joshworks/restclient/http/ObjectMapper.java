package io.joshworks.restclient.http;

public interface ObjectMapper {

    <T> T readValue(String value, Class<T> valueType);

    String writeValue(Object value);
}
