package io.joshworks.restclient.http;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MediaTypeTest {

    @Test
    public void parsesAllParameters() {
        MediaType mediaType = MediaType.valueOf("multipart/form-data; boundary=xyz; charset=UTF-8");

        assertEquals("multipart", mediaType.getType());
        assertEquals("form-data", mediaType.getSubtype());
        assertEquals("xyz", mediaType.getParameters().get("boundary"));
        assertEquals("UTF-8", mediaType.getParameters().get("charset"));
    }

    @Test
    public void parsesParametersWithoutWhitespace() {
        MediaType mediaType = MediaType.valueOf("application/json;charset=UTF-8");

        assertEquals("application", mediaType.getType());
        assertEquals("json", mediaType.getSubtype());
        assertEquals("UTF-8", mediaType.getParameters().get("charset"));
    }

    @Test
    public void parsesQuotedParameterValues() {
        MediaType mediaType = MediaType.valueOf("text/plain; charset=\"UTF-8\"");

        assertEquals("UTF-8", mediaType.getParameters().get("charset"));
    }

    @Test
    public void resolvesFileExtension() {
        assertEquals(MediaType.APPLICATION_JSON_TYPE, MediaType.valueOf("json"));
    }

    @Test
    public void nullParameterMapIsImmutable() {
        MediaType mediaType = new MediaType("application", "json", null);

        Map<String, String> parameters = mediaType.getParameters();
        assertTrue(parameters.isEmpty());
        assertEquals(Collections.emptyMap(), parameters);
        assertThrows(UnsupportedOperationException.class, () -> parameters.put("a", "b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidMediaType() {
        MediaType.valueOf("application");
    }
}
