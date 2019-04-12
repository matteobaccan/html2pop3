/**
 * Title:        Libero HTML2POP3
 * Description:  Filter class
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.utils;

import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class Filter {

    Vector aRules;

    /**
     *
     */
    public Filter() {
        aRules = new Vector();
    }

    class filterAtom {

        public String cRule = "";
        public String[] aFilter = {};

        public String toString() {
            String cFilter = "";
            for (int nPos = 0; nPos < aFilter.length; nPos++) {
                cFilter += (nPos > 0 ? ";" : "") + aFilter[nPos];
            }
            return cRule + " : " + cFilter;
        }
    }

    /**
     *
     * @param rule
     * @param filter
     */
    public void add(String rule, String[] filter) {
        filterAtom fa = new filterAtom();
        fa.cRule = rule;
        fa.aFilter = filter;
        aRules.addElement(fa);
    }

    /**
     *
     * @param filter
     * @return
     */
    public boolean isAllow(String[] filter) {
        boolean bRet = true;
        for (int nPos = 0; nPos < aRules.size(); nPos++) {
            filterAtom fa = (filterAtom) aRules.elementAt(nPos);
            //log.info( "Check " +fa );
            if (fa.cRule.equalsIgnoreCase("allow")) {
                if (match(filter, fa.aFilter)) {
                    bRet = true;
                    log.info("Verified: " + fa);
                    break;
                }
            } else if (fa.cRule.equalsIgnoreCase("deny")) {
                if (match(filter, fa.aFilter)) {
                    bRet = false;
                    log.info("Verified: " + fa);
                    break;
                }
            }
        }
        return bRet;
    }

    private boolean match(String[] element, String[] rule) {
        boolean bRet = true;
        for (int nPos = 0; nPos < element.length && nPos < rule.length; nPos++) {
            if (!rule[nPos].equalsIgnoreCase("all")) {
                if (!element[nPos].toUpperCase().startsWith(rule[nPos].toUpperCase())) {
                    bRet = false;
                    break;
                }
            }
        }
        return bRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Filter f = new Filter();
        f.add("allow", new String[]{"all", "192.168", "pippo"});
        f.add("allow", new String[]{"pluto"});
        f.add("deny", new String[]{"all"});

        if (f.isAllow(new String[]{"pippo", "127.0.0.1"})) {
            log.info("Filter 1 error");
        }
        if (!f.isAllow(new String[]{"pippo", "192.168.0.1"})) {
            log.info("Filter 2 error");
        }
        if (!f.isAllow(new String[]{"pluto", "192.168.0.1"})) {
            log.info("Filter 3 error");
        }
        if (f.isAllow(new String[]{"xxxxx", "192.168.0.1", ""})) {
            log.info("Filter 4 error");
        }
        if (f.isAllow(new String[]{"xxxxx", "192.168.0.1", "x"})) {
            log.info("Filter 5 error");
        }
        if (f.isAllow(new String[]{"xxxxx", "127.0.0.1"})) {
            log.info("Filter 6 error");
        }

    }

}
