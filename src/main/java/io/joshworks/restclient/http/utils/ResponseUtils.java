package io.joshworks.restclient.http.utils;

import io.joshworks.restclient.Constants;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class ResponseUtils {

    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

    /**
     * Parse out a charset from a content type header.
     *
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    public static String getCharsetFromContentType(String contentType) {
        if (contentType == null)
            return null;

        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }
        return null;
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    /**
     * Opens the entity content, transparently decompressing gzip responses.
     * A chunked gzip response has a content length of {@code -1}, so the
     * stream must still be decompressed in that case.
     *
     * @param entity the response entity, may be {@code null}
     * @return the entity stream or {@code null} when there is no entity
     * @throws IOException if the content stream cannot be opened
     */
    public static InputStream openEntityStream(HttpEntity entity) throws IOException {
        if (entity == null) {
            return null;
        }
        if (isGzipped(entity.getContentEncoding()) && entity.getContentLength() != 0) {
            return new GZIPInputStream(entity.getContent());
        }
        return entity.getContent();
    }

    public static boolean isGzipped(Header contentEncoding) {
        if (contentEncoding == null || contentEncoding.getValue() == null) {
            return false;
        }
        for (String token : contentEncoding.getValue().split(",")) {
            String value = token.trim();
            if (Constants.GZIP.equalsIgnoreCase(value) || "x-gzip".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

}
