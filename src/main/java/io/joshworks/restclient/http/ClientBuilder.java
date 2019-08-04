package io.joshworks.restclient.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ClientBuilder {

    private CredentialsProvider credentialsProvider;
    private int maxTotal = 20;
    private int maxRoute = 2;
    private String baseUrl = "";
    private boolean sslCheckDisabled;
    private Function<String, String> urlTransformer = url -> url;
    private Map<String, Object> defaultHeaders = new HashMap<>();

    private RequestConfig.Builder configBuilder = RequestConfig.custom();
    private List<HttpRequestInterceptor> requestInterceptors = new LinkedList<>();
    private List<HttpResponseInterceptor> responseInterceptor = new LinkedList<>();
    private final CookieStore cookieStore = new BasicCookieStore();
    private SSLContext sslContext;
    private long connectionTTL = -1;

    ClientBuilder() {

    }

    public RestClient build() {
        try {

            RequestConfig clientConfig = configBuilder.build();

            PoolingHttpClientConnectionManager syncConnectionManager = createSyncPoolingConnectionManager();
            CloseableHttpClient syncClient = createSyncClient(clientConfig, syncConnectionManager);

            PoolingNHttpClientConnectionManager asyncConnectionManager = createAsyncPoolingConnectionManager();
            CloseableHttpAsyncClient asyncClient = createAsyncClient(clientConfig, asyncConnectionManager);

            RestClient restClient = new RestClient(baseUrl, defaultHeaders, urlTransformer, asyncConnectionManager, syncConnectionManager, asyncClient, syncClient, cookieStore);
            ClientContainer.addClient(restClient);

            return restClient;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PoolingNHttpClientConnectionManager createAsyncPoolingConnectionManager() throws Exception {
        PoolingNHttpClientConnectionManager asyncConnectionManager = new PoolingNHttpClientConnectionManager(
                new DefaultConnectingIOReactor(),
                null,
                asyncSessionStrategy(),
                null,
                null,
                connectionTTL,
                TimeUnit.MILLISECONDS);

        asyncConnectionManager.setMaxTotal(maxTotal);
        asyncConnectionManager.setDefaultMaxPerRoute(maxRoute);
        return asyncConnectionManager;
    }

    private PoolingHttpClientConnectionManager createSyncPoolingConnectionManager() throws Exception {
        PoolingHttpClientConnectionManager syncConnectionManager = new PoolingHttpClientConnectionManager(
                syncSessionStrategy(),
                null,
                null,
                null,
                connectionTTL,
                TimeUnit.MILLISECONDS);

        syncConnectionManager.setMaxTotal(maxTotal);
        syncConnectionManager.setDefaultMaxPerRoute(maxRoute);
        return syncConnectionManager;
    }


    private Registry<ConnectionSocketFactory> syncSessionStrategy() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(byPassCertificate(), new NoopHostnameVerifier()))
                .build();
    }


    private Registry<SchemeIOSessionStrategy> asyncSessionStrategy() throws Exception {
        SchemeIOSessionStrategy httpsSessionStrategy = sslCheckDisabled ? noCertificateCheck() : SSLIOSessionStrategy.getDefaultStrategy();

        return RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", httpsSessionStrategy)
                .build();

    }

    private SSLIOSessionStrategy noCertificateCheck() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return new SSLIOSessionStrategy(byPassCertificate(), NoopHostnameVerifier.INSTANCE);

    }

    private SSLContext byPassCertificate() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        return new SSLContextBuilder()
                .loadTrustMaterial(null, (x509Certificates, s) -> true)
                .build();
    }

    private CloseableHttpAsyncClient createAsyncClient(RequestConfig clientConfig, PoolingNHttpClientConnectionManager manager) {
        HttpAsyncClientBuilder asyncBuilder = HttpAsyncClientBuilder.create()
                .setDefaultRequestConfig(clientConfig)
                .setDefaultCookieStore(cookieStore)
                .setSSLContext(sslContext)
                .setConnectionManager(manager);

        return addInterceptors(asyncBuilder).build();
    }

    private CloseableHttpClient createSyncClient(RequestConfig clientConfig, HttpClientConnectionManager manager) {

        HttpClientBuilder syncBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(clientConfig)
                .setDefaultCookieStore(cookieStore)
                .setSSLContext(sslContext)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(manager);

        if (sslContext != null) {
            syncBuilder.setSSLContext(sslContext);
        }

        return addInterceptors(syncBuilder).build();
    }

    private HttpClientBuilder addInterceptors(HttpClientBuilder builder) {
        for (HttpRequestInterceptor interceptor : requestInterceptors) {
            builder.addInterceptorLast(interceptor);
        }
        for (HttpResponseInterceptor interceptor : responseInterceptor) {
            builder.addInterceptorLast(interceptor);
        }
        return builder;
    }

    private HttpAsyncClientBuilder addInterceptors(HttpAsyncClientBuilder builder) {
        for (HttpRequestInterceptor interceptor : requestInterceptors) {
            builder.addInterceptorLast(interceptor);
        }
        for (HttpResponseInterceptor interceptor : responseInterceptor) {
            builder.addInterceptorLast(interceptor);
        }
        return builder;
    }

    public ClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ClientBuilder defaultHeader(String key, String value) {
        this.defaultHeaders.put(key, value);
        return this;
    }

    public ClientBuilder followRedirect(boolean followRedirect) {
        configBuilder.setRedirectsEnabled(followRedirect);
        return this;
    }

    public ClientBuilder disableSSLCheck() {
        this.sslCheckDisabled = true;
        return this;
    }

    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public ClientBuilder interceptor(HttpRequestInterceptor interceptor) {
        this.requestInterceptors.add(interceptor);
        return this;
    }

    public ClientBuilder interceptor(HttpResponseInterceptor interceptor) {
        this.responseInterceptor.add(interceptor);
        return this;
    }

    public ClientBuilder defaultHeader(String key, long value) {
        this.defaultHeaders.put(key, value);
        return this;
    }

    public ClientBuilder credentialProvider(CredentialsProvider provider) {
        this.credentialsProvider = provider;
        return this;
    }

    public ClientBuilder urlTransformer(Function<String, String> transformer) {
        this.urlTransformer = transformer;
        return this;
    }

    public ClientBuilder cookieSpec(String cookieSpec) {
        configBuilder.setCookieSpec(cookieSpec);
        return this;
    }

    public ClientBuilder proxy(HttpHost proxy) {
        configBuilder.setProxy(proxy);
        return this;
    }

    public ClientBuilder proxy(HttpHost proxy, Credentials credentials) {
        this.credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()), credentials);
        return this.proxy(proxy);
    }

    /**
     * Set the connection timeout and socket timeout
     *
     * @param connectionTimeout The timeout until a connection with the server is established (in milliseconds). Default is 10000. Set to zero to disable the timeout.
     * @param readTimeout       The timeout to receive data (in milliseconds). Default is 60000. Set to zero to disable the timeout.
     */
    public ClientBuilder timeout(int connectionTimeout, int readTimeout) {
        configBuilder.setSocketTimeout(readTimeout).setConnectTimeout(connectionTimeout);
        return this;
    }

    /**
     * Set the concurrency levels
     *
     * @param maxTotal Defines the overall connection limit for a connection pool. Default is 20.
     */
    public ClientBuilder concurrency(int maxTotal) {
        this.maxTotal = maxTotal;
        return this;
    }

    /**
     * Set the concurrency levels per host
     *
     * @param maxRoute Defines the connection limit for a connection pool per host. Default is 2.
     */
    public ClientBuilder routeConcurrency(int maxRoute) {
        this.maxRoute = maxRoute;
        return this;
    }

    /**
     * Total time to live (TTL)  defines maximum life span of persistent connections regardless of their expiration setting.
     * No persistent connection will be re-used past its TTL value.
     *
     * @return this config object
     */
    public ClientBuilder connectionTTL(long ttlMillis) {
        this.connectionTTL = ttlMillis;
        return this;
    }

}
