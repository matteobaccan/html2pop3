/*
 * Plugin per www.tim.it derivato da libero.java
 *
 * Copyright 2004 Francesco Sarzana <fsarzana@tim.it>
 *
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
 * Title:        Tim HTML2POP3
 * Description:  Convertitore da HTML a POP3 per www.tim.it
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Francesco Sarzana
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.utils.CharsetCoding;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import it.baccan.html2pop3.utils.message.*;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginTim extends POP3Base implements POP3Plugin {

    // Server di riferimento
    private final String cServer = "http://webmail1.posta.tim.it";

    // Sessione
    private String cSessionCook = "";

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Tim: login init");

            StringBuffer sb = new StringBuffer();

            // Preparo I parametri
            String cPost
                    = "USERNAME="
                    + URLEncoder.encode(cUser,CharsetCoding.UTF_8)
                    + "@tim.it"
                    + "&PASSWORD="
                    + URLEncoder.encode(cPwd,CharsetCoding.UTF_8)
                    + "&LOCALE=it_IT-TIM&SELECTEDLOCALE=it_IT-TIM";

            // Apro la pagina di posta
            sb = postPage(cServer + "/servlet/TransLogin", null, cPost);
            cSessionCook = getCookie();

            if (sb.toString().indexOf("<title>Erre di sessione</title>") == -1) {
                bRet = true;

                // Cerco la lista dei messaggi
                String cPage = sb.toString();
                int nPos = 0;
                nPos = 0;
                while (nPos != -1) {
                    //12345678 9012345678901234567890123456789012345678
                    nPos
                            = cPage.indexOf(
                                    "<a href=\"/cgi-bin/gx.cgi/AppLogic+mobmain?msgvw=",
                                    nPos);
                    if (nPos == -1) {
                        break;
                    }
                    int nPosEnd = cPage.indexOf("\"", nPos + 10);
                    if (nPosEnd == -1) {
                        break;
                    }

                    int nLen = 0;
                    try { //1234567890 1234567 89
                        int nSiz = cPage.indexOf("<td align=\"center\">", nPosEnd);
                        if (nSiz != -1) {
                            int nSizEnd = cPage.indexOf("</td>", nSiz);
                            if (nSizEnd != -1) {
                                String cSiz
                                        = cPage
                                                .substring(nSiz + 19, nSizEnd)
                                                .replace((char) 9, ' ')
                                                .replace((char) 13, ' ')
                                                .replace((char) 10, ' ')
                                                .trim();
                                if (cSiz.charAt(cSiz.length() - 1) == 'k') {
                                    nLen
                                            = Double
                                                    .valueOf(
                                                            cSiz.substring(
                                                                    0,
                                                                    cSiz.length() - 1))
                                                    .intValue()
                                            * 1024;
                                } else {
                                    nLen = Double.valueOf(cSiz).intValue();
                                }
                            }
                        }
                    } catch (Throwable ex) {
                    }

                    String cPos = cPage.substring(nPos + 48, nPosEnd);
                    addEmailInfo(cPos, nLen);
                    nPos = nPosEnd;
                }
            }
            log.error("Tim: login end");
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
            log.error("Tim: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Tim: getmail ID (" + cMsgId + ")");

            String sMail
                    = getPage(
                            cServer
                            + "/cgi-bin/gx.cgi/AppLogic+mobmain?msgvw="
                            + cMsgId,
                            cSessionCook)
                            .toString();

            int nI1 = sMail.indexOf("Messaggio multimediale:");
            if (nI1 != -1) {
                int nI2 = sMail.indexOf("<a href=", nI1);
                int nI3
                        = sMail.indexOf("\">Visualizza parti separatamente", nI2);
                if (nI2 != -1 && nI3 != -1) {
                    String cNewp
                            = replace(sMail.substring(nI2 + 9, nI3), "&amp;", "&");
                    sMail
                            = getPage(cServer + cNewp, cSessionCook)
                                    .toString();
                }
            }

            POP3Message pop3 = new POP3Message();
            pop3.setDa(getInfo(sMail, "Da:", true));
            pop3.setA(getInfo(sMail, "A:", true));
            pop3.setCc(getInfo(sMail, "Cc:", true));
            pop3.setOggetto(getInfo(sMail, "Oggetto:", false));
            pop3.setData(getInfo(sMail, "Data:", false));
            pop3.setBody(getMessage(sMail));
            pop3.addHTMLAttach("source.htm", sMail.getBytes());

            // TOP optimization
            if (bAll || nLine > 0) {
                //12345678901234567890
                int nAtt = sMail.indexOf("\"/agent/mobmain/");
                while (nAtt != -1) {
                    int nSla = sMail.indexOf("?", nAtt);
                    int nVir = sMail.indexOf("\"", nAtt + 19);
                    if (nSla != -1 && nVir != -1) {
                        String cSubPage = sMail.substring(nAtt + 1, nVir);
                        byte[] cFile = getPageBytes(cServer + cSubPage, cSessionCook);
                        pop3.addAttach(sMail.substring(nAtt + 16, nSla), cFile);
                    }
                    nAtt = sMail.indexOf("\"/agent/mobmain/", nAtt + 1);
                }
            }

            oMail.append(pop3.getMessage(nLine, bAll));

            log.error("Tim: getmail end");

        } catch (FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : oMail.toString());
    }

    private String getInfo(String cMail, String cInfo, boolean corr) {
        String cRet = "";
        int nInfo = cMail.indexOf("<b>" + cInfo + " </b></td><td>");
        int nInfo2 = cMail.indexOf("<", nInfo + 17 + cInfo.length());
        if (nInfo != -1 && nInfo2 != -1) {
            cRet = cMail.substring(nInfo + 17 + cInfo.length(), nInfo2);
            cRet = replace(cRet, "&nbsp;", "");
            cRet = replace(cRet, "&quot;", "\"");
            cRet = replace(cRet, "&lt;", "<");
            cRet = replace(cRet, "&gt;", ">");
            cRet = replace(cRet, "&#39;", "'");
            if (corr) {
                if (cRet == null || cRet.equals("\"") || cRet.equals("")) {
                    cRet = "\"\"";
                } else if (!cRet.startsWith("\"") && !cRet.startsWith("<")) {
                    if (cRet.indexOf("<") == -1) {
                        cRet = "<" + cRet + ">";
                    } else {
                        cRet = cRet = "\"" + replace(cRet, "<", "\"<");
                    }
                }
            }
            if ("Data:".equals(cInfo)) {
                Calendar cal = Calendar.getInstance();
                cal.set(
                        Integer.parseInt(cRet.substring(0, 4)),
                        Integer.parseInt(cRet.substring(5, 7)) - 1,
                        Integer.parseInt(cRet.substring(8, 10)),
                        Integer.parseInt(cRet.substring(18, 20)) - 1,
                        Integer.parseInt(cRet.substring(21, 23)),
                        Integer.parseInt(cRet.substring(24, 26)));
                Locale bLocale = new Locale("en", "US");
                SimpleDateFormat formatter
                        = new SimpleDateFormat(
                                "EEE, dd MMM yyyy HH:mm:ss z",
                                bLocale);
                cRet = formatter.format(cal.getTime());
            }
        }
        return cRet;
    }

    private String getMessage(String cMail) {
        String cRet = "";
        //123456789012345678901234567890123456789012345678901234567890
        int nInfo = cMail.indexOf("<XXBODYXX>");
        int nInfo2 = cMail.indexOf("</html>");
        if (nInfo != -1 && nInfo2 != -1) {
            cRet = cMail.substring(nInfo + 10, nInfo2).trim();
        } else {
            int nInf
                    = cMail.indexOf("<!-- Inizio visualizzazione messaggio -->");
            //123456789012345678901234567890123456789012345678901234567890
            nInfo = cMail.indexOf("<font size=3>", nInf);
            nInfo2
                    = cMail.indexOf(
                            "</font>\r\n                </td>\r\n              </tr>\r\n            </table>",
                            nInfo);
            if (nInfo != -1 && nInfo2 != -1) {
                cRet = cMail.substring(nInfo + 13, nInfo2).trim();
            }
        }
        return cRet;
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Tim: delmessage");

            String cMsgId = getMessageID(nPos);

            log.error("Tim: delmessage " + cMsgId);

            //StringBuffer oMail =
            getPage(cServer
                    + "/cgi-bin/gx.cgi/AppLogic+mobmain?del.x=1&msgOp"
                    + cMsgId
                    + "=on",
                    cSessionCook);
            bRet = true;

            log.error("Tim: delmessage end");
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
        PluginTim tim = new PluginTim();
        if (tim.login(args[0], args[1])) {
            int nNum = tim.getMessageNum();
            int nSiz = tim.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info(
                        "getMessageID   (" + nPos + "):" + tim.getMessageID(nPos));
                log.info(
                        "getMessageSize ("
                        + nPos
                        + "):"
                        + tim.getMessageSize(nPos));
                log.info(
                        "getMessage     (" + nPos + "):" + tim.getMessage(nPos));
            }
        }
    }

}
