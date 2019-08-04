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

package io.joshworks.restclient.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.joshworks.restclient.http.exceptions.JsonParsingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json {

    private static Gson gson = new Gson();

    private JSONObject jsonObject;
    private JSONArray jsonArray;
    private final String json;

    private boolean array;

    public Json(String json) {
        this.json = json;
        if (json == null || "".equals(json.trim())) {
            jsonObject = new JSONObject();
        } else {
            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                // It may be an array
                try {
                    jsonArray = new JSONArray(json);
                    array = true;
                } catch (JSONException e1) {
                    throw new JsonParsingException("Failed to parse " + json, e1);
                }
            }
        }
    }

    public JSONObject getObject() {
        return this.jsonObject;
    }

    public JSONArray getArray() {
        JSONArray result = this.jsonArray;
        if (!array) {
            result = new JSONArray();
            result.put(jsonObject);
        }
        return result;
    }

    public boolean isArray() {
        return this.array;
    }

    public <T> T as(Class<T> type) {
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, type);
    }

    public <T> List<T> asListOf(Class<T> type) {
        if (json == null) {
            return new ArrayList<>();
        }
        return gson.fromJson(json, new TypedList<>(type));
    }

    public Map<String, Object> asMap() {
        if (json == null) {
            return new HashMap<>();
        }
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    @Override
    public String toString() {
        if (isArray()) {
            if (jsonArray == null)
                return null;
            return jsonArray.toString();
        }
        if (jsonObject == null)
            return "";
        return jsonObject.toString();
    }

    public static class TypedList<T> implements ParameterizedType {
        private Class<?> wrapped;

        public TypedList(Class<T> wrapper) {
            this.wrapped = wrapper;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{wrapped};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

}
