package io.joshworks.restclient.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Josh Gontijo on 10/19/17.
 */
public class TestUtils {

    private static Path tempFile;

    public static InputStream mockStream(long length) {
        try {
            tempFile = Files.createTempFile("BIG_FILE", null);
            RandomAccessFile raf = new RandomAccessFile(tempFile.toFile(), "rw");
            raf.setLength(length);

            return Channels.newInputStream(raf.getChannel());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void deleteBigFile() {
        if (tempFile != null) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String toString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static long streamSize(InputStream is) {
        try {
            System.out.println("Calculating stream size");
            long mega = 1048576;
            long size = 0;

            byte[] buff = new byte[1048576];

            int read;
            while ((read = is.read(buff)) != -1) {
                size += read;
            }
            System.out.println("Read: " + size / mega + "mb");
            return size;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
