package io.joshworks.restclient.http.mapper;

import io.joshworks.restclient.http.MediaType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ObjectMappers {

    private static final AtomicReference<Map<MediaType, ObjectMapper>> mappers =
            new AtomicReference<>(defaultMappers());

    private ObjectMappers() {
    }

    private static Map<MediaType, ObjectMapper> defaultMappers() {
        Map<MediaType, ObjectMapper> defaults = new HashMap<>();
        defaults.put(MediaType.TEXT_PLAIN_TYPE, new TextPlainMapper());
        defaults.put(MediaType.APPLICATION_JSON_TYPE, new JsonMapper());
        return Collections.unmodifiableMap(defaults);
    }

    public static void register(MediaType mediaType, ObjectMapper mapper) {
        Objects.requireNonNull(mediaType, "MediaType must be provided");
        Objects.requireNonNull(mapper, "ObjectMapper must be provided");
        mappers.updateAndGet(current -> {
            Map<MediaType, ObjectMapper> updated = new HashMap<>(current);
            updated.put(mediaType, mapper);
            return Collections.unmodifiableMap(updated);
        });
    }

    /**
     * Removes all registered mappers and restores the default text and JSON
     * mappers.
     */
    public static void clear() {
        mappers.set(defaultMappers());
    }

    /**
     * Finds a mapper for the given media type.
     *
     * @param type the response media type
     * @return a compatible mapper, or {@code null} when none is registered
     */
    public static ObjectMapper getMapper(MediaType type) {
        if (type == null) {
            return null;
        }
        Map<MediaType, ObjectMapper> current = mappers.get();
        ObjectMapper mapper = current.get(type);
        if (mapper != null) {
            return mapper;
        }
        return current.entrySet().stream()
                .filter(kv -> kv.getKey().isCompatible(type))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
