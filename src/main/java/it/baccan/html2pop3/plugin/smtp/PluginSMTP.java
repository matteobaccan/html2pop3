/*
 * HTTP/SMTP generic plugin
 *
 * Copyright 2004 Matteo Baccan
 * www - http://www.baccan.it
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA (or visit
 * their web site at http://www.gnu.org/).
 *
 */
/**
 * Title:        HTML2POP3 SMTP2HTML
 * Description:  Convertitore da SMTP a HTML generico
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.smtp;

import java.net.*;
import java.util.*;

import it.baccan.html2pop3.utils.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginSMTP extends SMTPBase implements SMTPPlugin {

    private static String cDefaultServer = "http://www.baccan.it/pop3/";

    private String cError = "";

    private String cServer = "";
    private String cLocalUser = "";
    private String cLocalPwd = "";
    private String cLocalServer = "";
    private String cLocalPort = "25";

    /**
     *
     * @param cS
     */
    public static void setDefaultServer(String cS) {
        cDefaultServer = cS;
    }

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {

        log.error("Smtp: login init");

        cServer = cDefaultServer;
        cLocalUser = "";
        cLocalPwd = cPwd;
        cLocalServer = "";
        cLocalPort = "25";
        {
            // ## Sostituire ;; con ; ;
            int nTok = 0;
            StringTokenizer st = new StringTokenizer(cUser, ";");
            while (st.hasMoreTokens()) {
                String cTok = st.nextToken();
                nTok++;
                if (nTok == 1) {
                    cLocalUser = cTok.trim();
                } else if (nTok == 2) {
                    cLocalServer = cTok.trim();
                } else if (nTok == 3) {
                    cLocalPort = cTok.trim();
                } else if (nTok == 4) {
                    cServer = cTok.trim();
                }
            }
        }

        if (!cServer.endsWith("/")) {
            cServer += "/";
        }

        log.error("Smtp: login end");

        return true;
    }

    /**
     *
     * @param cFrom
     * @param aTo
     * @param cMsg
     * @return
     */
    public boolean sendMessage(String cFrom, Vector aTo, String cMsg) {
        boolean bRet = false;
        try {
            log.error("Smtp: sendmail init");

            String cTo = "";
            for (int nPos = 0; nPos < aTo.size(); nPos++) {
                cTo += (nPos > 0 ? ";" : "") + ((String) aTo.elementAt(nPos)).trim();
            }

            //log.error( "Smtp: sendmail " +cMsg );
            String cPost = "server=" + URLEncoder.encode(cLocalServer,CharsetCoding.UTF_8) + "&port=" + URLEncoder.encode(cLocalPort,CharsetCoding.UTF_8) + "&user=" + URLEncoder.encode(cLocalUser,CharsetCoding.UTF_8) + "&pwd=" + URLEncoder.encode(cLocalPwd,CharsetCoding.UTF_8);
            cPost += "&from=" + URLEncoder.encode(cFrom,CharsetCoding.UTF_8) + "&to=" + URLEncoder.encode(cTo,CharsetCoding.UTF_8) + "&msg=" + URLEncoder.encode(cMsg,CharsetCoding.UTF_8);
            cPost += "&ver=" + URLEncoder.encode(Version.getVersion(),CharsetCoding.UTF_8);

            String cPage = postPage(cServer + "postmsg.php", null, cPost).toString();

            if (cPage.endsWith("\r\n")) {
                cPage = cPage.substring(0, cPage.length() - 2);
            }

            bRet = cPage.startsWith("250");

            if (!bRet) {
                cError = cPage;
                if (cError.length() > 4) {
                    cError = cError.substring(3).trim();
                }
            }

            log.error("Smtp: sendmail end");
        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }

        return bRet;
    }

    /**
     *
     * @return
     */
    public String getLastErr() {
        return cError;
    }

}
