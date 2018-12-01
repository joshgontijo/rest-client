package io.joshworks.restclient.http;

import java.util.HashMap;
import java.util.List;

public class Headers extends HashMap<String, List<String>> {

    public String getFirst(Object key) {
        List<String> list = get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

}
