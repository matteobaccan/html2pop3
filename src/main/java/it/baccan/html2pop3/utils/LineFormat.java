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

import java.io.*;

/**
 *
 * @author matteo
 */
public class LineFormat {

    private LineFormat() {
    }

    /**
     *
     * @param s
     * @return
     */
    public static String format(String s) {
        String cRet;
        try {
            StringBuilder stringbuffer = new StringBuilder();

            BufferedReader in = new BufferedReader(new StringReader(s));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(".")) {
                    stringbuffer.append(".");
                }
                stringbuffer.append(inputLine).append((char) 13).append((char) 10);
            }
            cRet = stringbuffer.toString();
        } catch (Throwable e) {
            cRet = null;
        }
        return cRet;
    }

}
