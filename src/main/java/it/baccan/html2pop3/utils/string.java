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

/**
 *
 * @author matteo
 */
public class string {

    /**
     *
     * @param s
     * @param s1
     * @param s2
     * @return
     */
    public static String replace(String s, String s1, String s2) {
        StringBuffer stringbuffer = new StringBuffer();
        long l = s.length() - s1.length();
        long l1 = s.length();
        long l2 = s1.length() - 1;
        for (int i = 0; (long) i < l1; i++) {
            if ((long) i <= l) {
                if (s.startsWith(s1, i)) {
                    stringbuffer.append(s2);
                    i = (int) ((long) i + l2);
                } else {
                    stringbuffer.append(s.charAt(i));
                }
            } else {
                stringbuffer.append(s.charAt(i));
            }
        }

        return stringbuffer.toString();
    }

}
