package io.joshworks.restclient.request.body;

import io.joshworks.restclient.Constants;
import io.joshworks.restclient.http.ClientRequest;
import io.joshworks.restclient.request.BaseRequest;
import io.joshworks.restclient.request.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Josh Gontijo on 10/25/17.
 */
public class FormEncodedBody extends BaseRequest implements Body {

    private Map<String, List<Object>> parameters = new LinkedHashMap<String, List<Object>>();

    public FormEncodedBody(HttpRequest httpRequest, ClientRequest config) {
        super(config);
        super.httpRequest = httpRequest;
    }

    public FormEncodedBody field(String name, String value) {
        return add(name, value);
    }

    public FormEncodedBody field(String name, Integer value) {
        return add(name, value);
    }

    public FormEncodedBody field(String name, Long value) {
        return add(name, value);
    }

    public FormEncodedBody field(String name, Boolean value) {
        return add(name, value);
    }

    public FormEncodedBody field(String name, Double value) {
        return add(name, value);
    }

    public FormEncodedBody fields(Map<String, Object> params) {
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (param.getValue() == null) {
                    field(param.getKey(), "");
                    continue;
                }
                addField(param.getKey(), param.getValue());
            }
        }
        return this;
    }

    public FormEncodedBody field(String name, Collection<Object> values) {
        for (Object value : values) {
            addField(name, value);
        }
        return this;
    }

    private void addField(String key, Object value) {
        if (value instanceof String) {
            field(key, (String) value);
        } else if (value instanceof Integer) {
            field(key, (Integer) value);
        } else if (value instanceof Double) {
            field(key, (Double) value);
        } else if (value instanceof Long) {
            field(key, (Long) value);
        } else if (value instanceof Boolean) {
            field(key, (Boolean) value);
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid value type, valid values [%s,%s,%s,%s,%s,%s]",
                            String.class.getSimpleName(),
                            Integer.class.getSimpleName(),
                            Double.class.getSimpleName(),
                            Long.class.getSimpleName(),
                            Boolean.class.getSimpleName(),
                            Integer.class.getSimpleName()));
        }
    }

    private FormEncodedBody add(String name, Object value) {
        if (!parameters.containsKey(name)) {
            parameters.put(name, new LinkedList<>());
        }
        parameters.get(name).add(value);
        return this;
    }

    @Override
    public HttpEntity getEntity() {
        try {
            return new UrlEncodedFormEntity(getList(parameters), Constants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean implicitContentType() {
        return true;
    }

    private List<NameValuePair> getList(Map<String, List<Object>> parameters) {
        List<NameValuePair> result = new ArrayList<NameValuePair>();
        if (parameters != null) {
            TreeMap<String, List<Object>> sortedParameters = new TreeMap<String, List<Object>>(parameters);
            for (Map.Entry<String, List<Object>> entry : sortedParameters.entrySet()) {
                List<Object> entryValue = entry.getValue();
                if (entryValue != null) {
                    for (Object cur : entryValue) {
                        if (cur != null) {
                            result.add(new BasicNameValuePair(entry.getKey(), cur.toString()));
                        }
                    }
                }
            }
        }
        return result;
    }
}
