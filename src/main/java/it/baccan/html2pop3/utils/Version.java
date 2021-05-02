/**
 * Title:        Libero HTML2POP3
 * Description:  Version class
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.utils;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class Version {

    private Version() {
    }

    private static String html2pop3version = null;

    /**
     *
     * @return
     */
    public static String getVersion() {
        if (html2pop3version == null) {
            Class clazz = Version.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            if (classPath.startsWith("jar")) {
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                try {
                    Manifest manifest = new Manifest(new URL(manifestPath).openStream());
                    Attributes attr = manifest.getMainAttributes();
                    html2pop3version = attr.getValue("Implementation-Version");
                } catch (IOException iOException) {
                    log.error("Errr", iOException);
                }
            } else {
                html2pop3version = "";
            }
        }
        return html2pop3version;
    }
}
