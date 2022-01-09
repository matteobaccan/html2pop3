/*
 * HTTP/POP3 generic plugin
 *
 * Copyright 2004 Matteo Baccan
 * www - https://www.baccan.it
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
 * Title:        HTML2POP3 POP32HTML
 * Description:  Convertitore da POP3 a HTML generico
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.utils.CharsetCoding;
import it.baccan.html2pop3.utils.Version;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginPOP3 extends POP3Base implements POP3Plugin {

    private static String cDefaultServer = "https://www.baccan.it/pop3/";

    private String cServer = "";
    private String cLocalUser = "";
    private String cLocalPwd = "";
    private String cLocalServer = "";
    private String cLocalPort = "110";
    private ArrayList<String> oMsgDel = null;

    /**
     *
     * @param cS
     */
    public static void setDefaultServer(String cS) {
        cDefaultServer = cS;
    }

    /**
     * Vettore email.
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Pop3: login init");

            cServer = cDefaultServer;
            cLocalUser = "";
            cLocalPwd = cPwd;
            cLocalServer = "";
            cLocalPort = "110";
            {
                int nTok = 0;
                StringTokenizer st = new StringTokenizer(cUser, ";");
                while (st.hasMoreTokens()) {
                    String cTok = st.nextToken();
                    nTok++;
                    if (nTok == 1) {
                        cLocalUser = cTok;
                    } else if (nTok == 2) {
                        cLocalServer = cTok;
                    } else if (nTok == 3) {
                        cLocalPort = cTok;
                    } else if (nTok == 4) {
                        cServer = cTok;
                    }
                }
            }

            if (!cServer.endsWith("/")) {
                cServer += "/";
            }

            String cPost = "action=list&server=" + cLocalServer + "&port=" + cLocalPort + "&user=" + URLEncoder.encode(cLocalUser, CharsetCoding.UTF_8) + "&pass=" + URLEncoder.encode(cLocalPwd, CharsetCoding.UTF_8);
            cPost += "&ver=" + URLEncoder.encode(Version.getVersion(), CharsetCoding.UTF_8);
            String sb = postPage(cServer + "msglist.php", "", cPost).toString();

            {
                int nTok = 0;
                StringTokenizer st = new StringTokenizer(sb, "\n");
                while (st.hasMoreTokens()) {
                    String cTok = st.nextToken();
                    nTok++;
                    if (nTok == 1) {
                        bRet = cTok.startsWith("+OK");
                        if (!bRet && cTok.startsWith("-ERR")) {
                            setLastErr(cTok.substring(4).replace((char) 13, ' ').replace((char) 10, ' ').trim());
                            log.error("Pop3: " + getLastErr());
                        } else {
                            setLastErr("");
                        }
                    } else {
                        addEmailInfo(cTok.substring(0, cTok.indexOf(" ")).trim(),
                                Double.valueOf(cTok.substring(cTok.indexOf(" ")).trim()).intValue());
                    }
                }
            }
            log.error("Pop3: login end");
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
        StringBuilder oMail;
        try {
            log.error("Pop3: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Pop3: getmail ID (" + cMsgId + ")");

            String cPost = "action=get&server=" + cLocalServer + "&port=" + cLocalPort + "&user=" + URLEncoder.encode(cLocalUser, CharsetCoding.UTF_8) + "&pass=" + URLEncoder.encode(cLocalPwd, CharsetCoding.UTF_8) + "&msgid=" + cMsgId;
            cPost += "&ver=" + URLEncoder.encode(Version.getVersion(), CharsetCoding.UTF_8);

            oMail = new StringBuilder();
            oMail.append(getPage(cServer + "msglist.php?" + cPost, "", nLine, bAll));

            log.error("Pop3: getmail end");
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : oMail.toString());
    }

    /**
     * Stream a message.
     *
     * @param outputStream
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     * @throws Exception
     */
    @Override
    public boolean streamMessage(OutputStream outputStream, int nPos, int nLine, boolean bAll) throws Exception {
        boolean bRet = false;
        // Non si puo' fare, perche', nel caso ci sia un errore di connessione al POP3, vengono restituiti messaggi vuoti
        // viene dato un +OK e poi un -ERR
        return bRet;
    }

    /**
     *
     */
    public void delMessageStart() {
        oMsgDel = new ArrayList<>();
    }

    /**
     *
     */
    public void delMessageEnd() {

        try {
            if (!oMsgDel.isEmpty()) {
                StringBuffer oAllMsg = new StringBuffer();
                for (int nPos = 0; nPos < oMsgDel.size(); nPos++) {
                    String cMsgId = oMsgDel.get(nPos);
                    if (nPos > 0) {
                        oAllMsg.append("***");
                    }
                    oAllMsg.append(URLEncoder.encode(cMsgId, CharsetCoding.UTF_8));
                }

                String cPost = "msglist=" + oAllMsg.toString();
                cPost += "&action=delete&server=" + cLocalServer + "&port=" + cLocalPort + "&user=" + URLEncoder.encode(cLocalUser, CharsetCoding.UTF_8) + "&pass=" + URLEncoder.encode(cLocalPwd, CharsetCoding.UTF_8);
                cPost += "&ver=" + URLEncoder.encode(Version.getVersion(), CharsetCoding.UTF_8);

                //String cPage = postPage( cServer +"msglist.php?action=delete&server=" +cLocalServer +"&port=" +cLocalPort +"&user=" +URLEncoder.encode(cLocalUser) +"&pass=" +URLEncoder.encode(cLocalPwd), null, cPost ).toString();
                String cPage = postPage(cServer + "msglist.php", null, cPost).toString();

                if (cPage.endsWith("\r\n")) {
                    cPage = cPage.substring(0, cPage.length() - 2);
                }

                log.error("Pop3: delmessageresult " + cPage);
            }
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Pop3: delmessage");

            String cMsgId = getMessageID(nPos);

            log.error("Pop3: delmessage " + cMsgId);

            oMsgDel.add(cMsgId);

            bRet = true;

            log.error("Pop3: delmessage end");
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
        PluginPOP3 infinito = new PluginPOP3();
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
