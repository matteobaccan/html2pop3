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
import java.util.*;

/**
 *
 * @author matteo
 */
public class lineFormat {

    /**
     *
     * @param s
     * @return
     */
    public static String format(String s) {
        String cRet = null;
        try {
            //log.info( s );
            StringBuffer stringbuffer = new StringBuffer();

            BufferedReader in = new BufferedReader(new StringReader(s));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(".")) {
                    inputLine = "." + inputLine;
                }
                stringbuffer.append(inputLine + (char) 13 + (char) 10);
            }
            cRet = stringbuffer.toString();
            //log.info( cRet );
        } catch (Throwable e) {
            cRet = null;
        }
        return cRet;
    }

}
