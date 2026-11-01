package io.joshworks.restclient.http;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClientBuilderTest {

    @Test
    public void defaultSyncClientVerifiesHostnames() throws Exception {
        ClientBuilder builder = RestClient.builder();

        HostnameVerifier verifier = hostnameVerifier(builder.sslConnectionSocketFactory());

        assertNotNull(verifier);
        assertFalse(verifier instanceof NoopHostnameVerifier);
    }

    @Test
    public void defaultAsyncClientVerifiesHostnames() throws Exception {
        ClientBuilder builder = RestClient.builder();

        HostnameVerifier verifier = hostnameVerifier(builder.sslSessionStrategy());

        assertNotNull(verifier);
        assertFalse(verifier instanceof NoopHostnameVerifier);
    }

    @Test
    public void disablingSslCheckUsesNonVerifyingFactories() throws Exception {
        ClientBuilder builder = RestClient.builder().disableSSLCheck();

        assertTrue(hostnameVerifier(builder.sslConnectionSocketFactory()) instanceof NoopHostnameVerifier);
        assertTrue(hostnameVerifier(builder.sslSessionStrategy()) instanceof NoopHostnameVerifier);
    }

    private static HostnameVerifier hostnameVerifier(Object factory) throws Exception {
        Field field = factory.getClass().getDeclaredField("hostnameVerifier");
        field.setAccessible(true);
        return (HostnameVerifier) field.get(factory);
    }
}
