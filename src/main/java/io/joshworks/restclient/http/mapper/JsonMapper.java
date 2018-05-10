package io.joshworks.restclient.http.mapper;

import com.google.gson.Gson;
import io.joshworks.restclient.http.exceptions.JsonParsingException;

/**
 * Created by Josh Gontijo on 5/5/17.
 */
public class JsonMapper implements ObjectMapper {
    private final Gson gson = new Gson();

    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        try {
            return gson.fromJson(value, valueType);
        } catch (Exception e) {
            throw new JsonParsingException("Failed to parse " + value, e);
        }
    }

    @Override
    public String writeValue(Object value) {
        try {
            return gson.toJson(value);
        } catch (Exception e) {
            throw new JsonParsingException("Failed to create json of " + value, e);
        }

    }
}
