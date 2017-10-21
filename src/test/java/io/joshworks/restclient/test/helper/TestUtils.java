package io.joshworks.restclient.test.helper;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

/**
 * Created by Josh Gontijo on 10/19/17.
 */
public class TestUtils {

    public static InputStream mockStream(long length) {
        try {
            RandomAccessFile raf = new RandomAccessFile("t", "rw");
            raf.setLength(length);

            return Channels.newInputStream(raf.getChannel());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }



}
