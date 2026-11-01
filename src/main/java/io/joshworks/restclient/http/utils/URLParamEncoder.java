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

package io.joshworks.restclient.http.utils;

import java.nio.charset.StandardCharsets;

public class URLParamEncoder {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private URLParamEncoder() {
    }

    /**
     * Percent-encodes a value using UTF-8 so it can be safely used as a path
     * segment. Only RFC 3986 unreserved characters are left untouched.
     *
     * @param input the value to encode
     * @return the encoded value
     */
    public static String encode(String input) {
        if (input == null) {
            throw new IllegalArgumentException("URL parameter value must not be null");
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int ch = value & 0xFF;
            if (isUnreserved(ch)) {
                result.append((char) ch);
            } else {
                result.append('%');
                result.append(HEX[ch >>> 4]);
                result.append(HEX[ch & 0x0F]);
            }
        }
        return result.toString();
    }

    private static boolean isUnreserved(int ch) {
        return (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9')
                || ch == '-'
                || ch == '.'
                || ch == '_'
                || ch == '~';
    }
}
