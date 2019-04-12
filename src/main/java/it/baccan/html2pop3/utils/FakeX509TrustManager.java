package it.baccan.html2pop3.utils;

/**
 * @author Matteo Baccan
 */
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class FakeX509TrustManager implements X509TrustManager {

    /**
     *
     * @param cert
     * @param authType
     */
    public void checkClientTrusted(X509Certificate[] cert, String authType) {
        log.info("checkClientTrusted: not implemented");
    }

    /**
     *
     * @param cert
     * @param authType
     */
    public void checkServerTrusted(X509Certificate[] cert, String authType) {
        log.info("checkServerTrusted: not implemented");
    }

    /**
     *
     * @return
     */
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
