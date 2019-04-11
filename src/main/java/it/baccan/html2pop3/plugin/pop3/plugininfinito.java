/*
 * Infinito plugin
 *
 * Copyright 2003 Matteo Baccan
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
 * Title:        Infinito HTML2POP3
 * Description:  Convertitore da HTML a POP3 per infinito.it
 * Copyright:    Copyright (c) 2003
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.net.*;
import java.util.*;

import it.baccan.html2pop3.utils.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class plugininfinito extends POP3Base implements POP3Plugin {

    /**
     *
     */
    public plugininfinito() {
    }

    // Server di riferimento
    private String cServer = "http://www.infinito.it";

    // Vettore email
    //private Vector aEmail = new Vector();
    // Property per variabili hidden
    private Properties p = new Properties();

    // Sessione
    private String cSessionCook = "";
    private String cSession = "";

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Infinito: login init");

            StringBuffer sb = new StringBuffer();

            //123456789012
            if (cUser.toUpperCase().endsWith("@INFINITO.IT")) {
                cUser = cUser.substring(0, cUser.length() - 12);
            } else if (cUser.toUpperCase().endsWith("@GENIE.IT")) {
                cUser = cUser.substring(0, cUser.length() - 9);
            }

            // Preparo I parametri
            String cPost = "uname=" + URLEncoder.encode(cUser) + "&upassword=" + URLEncoder.encode(cPwd);

            sb = postPage(cServer + "/InfTransactionLog/1,2818,SAVE_STATE,00.html", null, cPost); //, cServer +"/homepage" );

            // Passata la prima pagina faccio il login alla inbox vera e propria
            cSessionCook = getCookie();
            sb = getPage(getLocation(), cSessionCook);//, 0, true, cServer +"/homepage" );
            cSessionCook += getCookie();

            // Lista email
            sb = getPage(cServer + "/web/1,2647,transEmail,00.html", cSessionCook);//, 0, true, cServer +"/homepage" );

            // redir
            String cLoginWithSession = getLocation();
            int nSes = cLoginWithSession.indexOf("session=");
            if (nSes > 0) {
                int nEnd = cLoginWithSession.indexOf("&", nSes);
                if (nEnd > 0) {
                    cSession = cLoginWithSession.substring(nSes + 8, nEnd);
                    sb = getPage(cServer + cLoginWithSession, cSessionCook);
                    bRet = true;
                }
            }

            String cFrame = sb.toString();
            int nPageIni = cFrame.indexOf("http://213.92.8.142/Session/");
            int nPageEnd = cFrame.indexOf("\"", nPageIni + 1);
            if (nPageIni != -1 && nPageEnd != -1) {
                sb = getPage(cFrame.substring(nPageIni, nPageEnd));
                // Cerco la lista dei messaggi
                // Cerca javascript:doitMsg(
                String cPage = sb.toString();
                int nPos = 0;
                nPos = 0;
                while (nPos != -1) {   //1234567890123456789012345678901234567890
                    nPos = cPage.indexOf("<A HREF=\"Message.wssp?Mailbox=INBOX&MSG", nPos);
                    if (nPos == -1) {
                        break;
                    }
                    int nPosEnd = cPage.indexOf("\"", nPos + 10);
                    if (nPosEnd == -1) {
                        break;
                    }

                    int nLen = 0;
                    try {
                        int nSiz = cPage.indexOf("<TD NOWRAP align=RIGHT>", nPosEnd);
                        if (nSiz != -1) {
                            int nSizEnd = cPage.indexOf("</TD>", nSiz);
                            if (nSizEnd != -1) {
                                String cSiz = cPage.substring(nSiz + 23, nSizEnd).replace((char) 9, ' ').replace((char) 13, ' ').replace((char) 10, ' ').trim();
                                if (cSiz.charAt(cSiz.length() - 1) == 'K') {
                                    nLen = Double.valueOf(cSiz.substring(0, cSiz.length() - 1)).intValue() * 1024;
                                } else {
                                    nLen = Double.valueOf(cSiz).intValue();
                                }
                            }
                        }
                    } catch (Throwable ex) {
                    }

                    String cPos = cPage.substring(nPos + 40, nPosEnd);
                    addEmailInfo(cPos, nLen);
                    nPos = nPosEnd;
                }
            }
            log.error("Infinito: login end");

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    public String getMessage(int nPos, int nLine, boolean bAll) {
        StringBuffer oMail = new StringBuffer();
        try {
            log.error("Infinito: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Infinito: getmail ID (" + cMsgId + ")");

            oMail = getPage("http://213.92.8.142/Session/" + cSession + "/MessagePart/INBOX/" + cMsgId + "-P.txt", cSessionCook, nLine, bAll);

            if (getContentType().indexOf("text/plain") == -1) {
                oMail = null;
            }

            log.error("Infinito: getmail end");

        } catch (java.io.FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : lineFormat.format(oMail.toString()));
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Infinito: delmessage");

            String cMsgId = getMessageID(nPos);

            log.error("Infinito: delmessage " + cMsgId);

            // ## verificare come pulire il cestino
            //StringBuffer oMail =
            getPage("http://213.92.8.142/Session/" + cSession + "/Mailbox.wssp?Mailbox=INBOX&MSG=" + cMsgId + "&Delete=&", cSessionCook);

            bRet = true;

            log.error("Infinito: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        plugininfinito infinito = new plugininfinito();
        if (infinito.login(args[0], args[1])) {
            int nNum = infinito.getMessageNum();
            int nSiz = infinito.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + infinito.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + infinito.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + infinito.getMessage(nPos));
            }
        }
    }

}
