/*
 * HTTP/SMTP cgiemail plugin
 *
 * Copyright 2009 Maurizio Scaglione
 * www - http://www.sck.netsons.org
 * email - scheggione@libero.it
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
 * Description:  Convertitore da SMTP a HTML per cgiemail
 * Copyright:    Copyright (c) 2009
 * Company:
 *
 * @author Maurizio Scaglione, Matteo Baccan
 * @version 1.0
 */

/*
Per usare questo plugin occorre iscriversi a portali come netsons
(www.netsons.org), che mettono a disposizione il programma cgiemail
(web.mit.edu/wwwdev/cgiemail), adatto per form di posta inseriti in
pagine html;
 */
package it.baccan.html2pop3.plugin.smtp;

import java.net.*;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginCGIEmail extends SMTPBase implements SMTPPlugin {

    /* URL dello script cgiemail */
    private static String cDefaultServer = "";

    private String cError = "";
    private String cServer = "";
    //private String cLocalUser = "";
    //private String cLocalPwd = "";

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
        /* allow/deny policy */
        //cLocalUser = "";
        //cLocalPwd = cPwd;

        // ## Sostituire ;; con ; ;
        // per compatibilità con pluginsmtp
        int nTok = 0;
        StringTokenizer st = new StringTokenizer(cUser, ";");
        while (st.hasMoreTokens()) {
            String cTok = st.nextToken();
            nTok++;
            if (nTok == 1) {
                //cLocalUser = cTok.trim();
            } else if (nTok == 2) ;//cLocalServer = cTok.trim();
            else if (nTok == 3) ;//cLocalPort   = cTok.trim();
            else if (nTok == 4) {
                cServer = cTok.trim();
            }
        }

        /* put here allow/deny policy code */
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
            String cPost = "source=" + URLEncoder.encode(cMsg);
            String cPage = postPage(cServer, null, cPost).toString();
            bRet = cPage.startsWith("<HEAD><TITLE>Success</TITLE></HEAD>");
            if (!bRet) {
                cError = cPage;
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
