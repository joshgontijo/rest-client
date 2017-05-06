package io.joshworks.restclient.test.helper;

import com.google.gson.Gson;
import io.joshworks.restclient.http.mapper.ObjectMapper;

/**
 * Created by Josh Gontijo on 5/5/17.
 */
public class JsonMapper implements ObjectMapper {
    private final Gson gson = new Gson();
    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        return gson.fromJson(value, valueType);
    }

    @Override
    public String writeValue(Object value) {
        return gson.toJson(value);
    }
}
