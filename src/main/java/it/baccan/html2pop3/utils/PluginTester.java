/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.html2pop3.utils;

import it.baccan.html2pop3.plugin.pop3.POP3Plugin;
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

        String server = POP3Selector.user2Server(user);
        log.info("Server used [{}]", server);

        POP3Plugin plugin = POP3Selector.server2POP3Plugin(server);

        if (plugin == null) {
            log.error("Unknow plugin for [{}]", user);
            System.exit(1);
        } else if (plugin.login(user, pass)) {
            int nNum = plugin.getMessageNum();
            int nSiz = plugin.getMessageSize();
            log.info("getMessageNum  [{}]", nNum);
            log.info("getMessageSize [{}]", nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   [{}]:[{}]", nPos, plugin.getMessageID(nPos));
                log.info("getMessageSize [{}]:[{}]", nPos, plugin.getMessageSize(nPos));
                log.info("getMessage     [{}] real length [{}]", nPos, plugin.getMessage(nPos).length());
            }
            log.info("getContactXML  [{}]", plugin.getContactXML());
        } else {
            log.error("Login error on plugin [{}]", plugin.getClass().getName());
            System.exit(1);
        }
    }
}
