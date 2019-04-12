package it.baccan.html2pop3.utils;

/**
 * @author Matteo Baccan
 */
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 *
 * @author matteo
 */
public class FakeX509TrustManager implements X509TrustManager {

    /**
     *
     * @param cert
     * @param authType
     */
    public void checkClientTrusted(X509Certificate[] cert, String authType) {
    }

    /**
     *
     * @param cert
     * @param authType
     */
    public void checkServerTrusted(X509Certificate[] cert, String authType) {
    }

    /**
     *
     * @return
     */
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
