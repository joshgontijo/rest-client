package io.joshworks.restclient.http.utils;

import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponseUtilsTest {

    @Test
    public void readsAllBytesWhenAvailableIsNotReliable() throws IOException {
        byte[] data = "some response body".getBytes(StandardCharsets.UTF_8);
        InputStream stream = new InputStream() {
            private int index;

            @Override
            public int available() {
                return 0;
            }

            @Override
            public int read() {
                return index < data.length ? data[index++] & 0xFF : -1;
            }
        };

        assertArrayEquals(data, ResponseUtils.readBytes(stream));
    }

    @Test
    public void detectsGzipInMultipleContentEncodings() {
        assertTrue(ResponseUtils.isGzipped(new BasicHeader("Content-Encoding", "gzip")));
        assertTrue(ResponseUtils.isGzipped(new BasicHeader("Content-Encoding", "br, gzip")));
        assertTrue(ResponseUtils.isGzipped(new BasicHeader("Content-Encoding", "x-gzip")));
        assertFalse(ResponseUtils.isGzipped(new BasicHeader("Content-Encoding", "deflate")));
    }

    @Test
    public void decompressesChunkedGzipContent() throws IOException {
        byte[] original = "chunked gzip response".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(original);
        }

        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(compressed.toByteArray()));
        entity.setContentEncoding("gzip");
        entity.setContentLength(-1);

        try (InputStream stream = ResponseUtils.openEntityStream(entity)) {
            assertArrayEquals(original, ResponseUtils.readBytes(stream));
        }
    }
}
