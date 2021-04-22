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

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class Filter {

    private final List<FilterAtom> aRules;

    /**
     * Filter class for HTML2POP3 use.
     */
    public Filter() {
        aRules = new ArrayList<>();
    }

    class FilterAtom {

        public String cRule = "";
        public String[] aFilter = {};

        @Override
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
     * @return
     */
    public boolean add(String rule, String[] filter) {
        FilterAtom fa = new FilterAtom();
        fa.cRule = rule;
        fa.aFilter = filter;
        return aRules.add(fa);
    }

    /**
     *
     * @param filter
     * @return
     */
    public boolean isAllow(String[] filter) {
        boolean bRet = true;
        for (int nPos = 0; nPos < aRules.size(); nPos++) {
            FilterAtom fa = (FilterAtom) aRules.get(nPos);
            if (fa.cRule.equalsIgnoreCase("allow")) {
                if (match(filter, fa.aFilter)) {
                    log.debug("Verified: " + fa);
                    break;
                }
            } else if (fa.cRule.equalsIgnoreCase("deny")) {
                if (match(filter, fa.aFilter)) {
                    bRet = false;
                    log.debug("Verified: " + fa);
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

}
