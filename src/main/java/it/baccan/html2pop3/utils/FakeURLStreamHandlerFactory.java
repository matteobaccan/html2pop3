package it.baccan.html2pop3.utils;

/**
 * @author Matteo Baccan
 */
import java.net.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

// Se riesco, uso
/**
 *
 * @author matteo
 */
@Slf4j
public class FakeURLStreamHandlerFactory implements URLStreamHandlerFactory {

    // StreamHandler per gestire HTTPS con MS JDK
    private static URLStreamHandler WinINetsh = null;

    @Override
    public URLStreamHandler createURLStreamHandler(String s) {
        // StreamHandler vuoto
        URLStreamHandler urlstreamhandler = null;

        // Se https restituisco quello di WinINet, precalcolato in startup di HTML2POP3
        if (s.equalsIgnoreCase("https")) {
            urlstreamhandler = WinINetsh;

            // Altrimenti lo ricavo
        } else {
            String s1 = System.getProperty("java.protocol.handler.pkgs", "");
            if (!s1.isEmpty()) {
                s1 += "|";
            }
            s1 += "com.ms.net.www.protocol|";
            s1 += "sun.net.www.protocol";
            for (StringTokenizer stringtokenizer = new StringTokenizer(s1, "|"); urlstreamhandler == null && stringtokenizer.hasMoreTokens(); ) {
                String s2 = stringtokenizer.nextToken().trim();
                try {
                    String s3 = s2 + "." + s + ".Handler";
                    urlstreamhandler = (URLStreamHandler) Class.forName(s3).newInstance();
                } catch (Exception _ex) {
                }
            }
        }

        return urlstreamhandler;
    }

    static {
        try {
            // Provo a caricare la WinInet Factory
            // E' presente solo nel JDK MS e serve per gestire HTTPS
            Class WinINet = Class.forName("com.ms.net.wininet.WininetStreamHandlerFactory");
            URLStreamHandlerFactory WinINetshf = (URLStreamHandlerFactory) WinINet.newInstance();
            WinINetsh = WinINetshf.createURLStreamHandler("https");
        } catch (Exception e) {
            log.error("Error", e);
        }
    }
}
