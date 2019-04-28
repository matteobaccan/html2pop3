/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.html2pop3.utils;

import it.baccan.html2pop3.plugin.pop3.POP3Plugin;
import it.baccan.html2pop3.plugin.pop3.PluginLibero;
import it.baccan.html2pop3.plugin.pop3.PluginTin;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Matteo
 */
@Slf4j
public final class PluginTester {

    /**
     * Private constructor.
     */
    private PluginTester() {

    }

    /**
     *
     * @param args
     */
    public static void main(final String[] args) {
        String user = args[0];
        String pass = args[1];
        POP3Plugin plugin = null;
        if (user.contains("@tim.it")) {
            plugin = new PluginTin();
        } else if (user.contains("@libero.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_LIBERO);
        } else {
            log.error("Unknow plugin for [{}]", user);
        }

        if (plugin != null && plugin.login(user, pass)) {
            int nNum = plugin.getMessageNum();
            int nSiz = plugin.getMessageSize();
            log.info("getMessageNum  [{}]", nNum);
            log.info("getMessageSize [{}]", nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   [{}]:[{}]", nPos, plugin.getMessageID(nPos));
                log.info("getMessageSize [{}]:[{}]", nPos, plugin.getMessageSize(nPos));
                log.info("getMessage     [{}]:[{}]", nPos, plugin.getMessage(nPos));
            }
        }
    }
}
