package io.joshworks.restclient.http;

import io.joshworks.restclient.helper.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.concurrent.Future;

import static io.joshworks.snappy.SnappyServer.get;
import static io.joshworks.snappy.SnappyServer.multipart;
import static io.joshworks.snappy.SnappyServer.start;
import static org.junit.Assert.assertEquals;

/**
 * These tests aims to detect excessive heap usage, change the STREAM_SIZE to fit specific memory capacities
 * All streams are fully read when processing the request / response by using TestUtils.streamSize(InputStream)
 * in order to simulate real scenarios
 */
public class LargeFileIT {

    private final static long STREAM_SIZE = 32212254720L; //30GB

    @BeforeClass
    public static void setUp() {
        get("/download", exchange -> {
            try (InputStream inputStream = TestUtils.mockStream(STREAM_SIZE)) {
                exchange.stream(inputStream);
            } finally {
                TestUtils.deleteBigFile();
            }
        });
        multipart("/upload", exchange -> {
            long consumedByteSize = TestUtils.streamSize(exchange.part("file").file().stream());
            exchange.send(consumedByteSize, "txt");
        });
        start();
    }

    @Test
    public void asBinary() {
        try (HttpResponse<InputStream> httpResponse = Unirest.get("http://localhost:9000/download").asBinary()) {
            System.out.println("Request sent");
            assertEquals(200, httpResponse.getStatus());
            assertEquals(STREAM_SIZE, TestUtils.streamSize(httpResponse.body()));
        }
    }

    @Test
    public void multipart_sync_upload() {
        try {
            HttpResponse<String> httpResponse = Unirest.post("http://localhost:9000/upload")
                    .part("file", TestUtils.mockStream(STREAM_SIZE), "someFile.txt")
                    .asString();

            assertEquals(200, httpResponse.getStatus());
            assertEquals(STREAM_SIZE, Long.parseLong(httpResponse.body()));

        } finally {
            TestUtils.deleteBigFile();
        }
    }

    @Test
    public void multipart_async_upload() throws Exception {
        try {
            Future<HttpResponse<String>> futureResponse = Unirest.post("http://localhost:9000/upload")
                    .part("file", TestUtils.mockStream(STREAM_SIZE), "someFile.txt")
                    .asStringAsync();

            HttpResponse<String> httpResponse = futureResponse.get();
            assertEquals(200, httpResponse.getStatus());
            assertEquals(STREAM_SIZE, Long.parseLong(httpResponse.body()));
        } finally {
            TestUtils.deleteBigFile();
        }
    }

}
