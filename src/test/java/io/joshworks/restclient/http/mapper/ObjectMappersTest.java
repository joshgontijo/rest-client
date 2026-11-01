package io.joshworks.restclient.http.mapper;

import io.joshworks.restclient.http.MediaType;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ObjectMappersTest {

    @After
    public void reset() {
        ObjectMappers.clear();
    }

    @Test
    public void defaultsAreAvailableWithoutPriorRegistration() {
        ObjectMappers.clear();

        assertNotNull(ObjectMappers.getMapper(MediaType.APPLICATION_JSON_TYPE));
        assertNotNull(ObjectMappers.getMapper(MediaType.TEXT_PLAIN_TYPE));
    }

    @Test
    public void registeringCustomMapperKeepsDefaults() {
        ObjectMappers.clear();
        ObjectMapper custom = new ObjectMapper() {
            @Override
            public <T> T readValue(String value, Class<T> valueType) {
                return null;
            }

            @Override
            public String writeValue(Object value) {
                return String.valueOf(value);
            }
        };

        ObjectMappers.register(MediaType.APPLICATION_XML_TYPE, custom);

        assertEquals(custom, ObjectMappers.getMapper(MediaType.APPLICATION_XML_TYPE));
        assertNotNull(ObjectMappers.getMapper(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void parametersDoNotPreventCompatibleMapperLookup() {
        ObjectMapper mapper = ObjectMappers.getMapper(MediaType.valueOf("application/json; charset=UTF-8"));

        assertNotNull(mapper);
        assertEquals(JsonMapper.class, mapper.getClass());
    }

    @Test
    public void unknownMediaTypeReturnsNull() {
        assertNull(ObjectMappers.getMapper(MediaType.valueOf("application/x-unknown")));
    }

    @Test
    public void nullMediaTypeReturnsNull() {
        assertNull(ObjectMappers.getMapper(null));
    }
}
