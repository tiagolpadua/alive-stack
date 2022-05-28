package org.prober.utils;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@ApplicationScoped
public class NetworkService {
    private static final int EXECUTOR_THREAD_POOL_SIZE = 5;

    private ExecutorService executorService = Executors.newFixedThreadPool(EXECUTOR_THREAD_POOL_SIZE);

    private HttpClient httpClient = HttpClient.newBuilder()
            .sslContext(insecureContext())
            .executor(executorService)
            .build();

    // https://adambien.blog/roller/abien/entry/how_to_connect_to_an
    private SSLContext insecureContext() {
        TrustManager[] noopTrustManager = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] xcs, String string) {
                    }

                    public void checkServerTrusted(X509Certificate[] xcs, String string) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance("ssl");
            sc.init(null, noopTrustManager, null);
            return sc;
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public HttpClient getHttpClient() {
        return this.httpClient;
    }

}
