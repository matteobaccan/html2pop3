/*
 * fastwebnet plugin
 *
 * Copyright 2003 Matteo Baccan
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
 * Title:        fastwebnet HTML2POP3
 * Description:  Convertitore da HTML a POP3 per fastwebnet.it, usano lo stesso software di tiscali
 * Copyright:    Copyright (c) 2003
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.utils.CharsetCoding;
import java.net.*;

import it.baccan.html2pop3.utils.message.*;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginFastwebnet extends POP3Base implements POP3Plugin {

    // Server di riferimento
    private String cServer = "http://ms002msg.fastwebnet.it";

    // Sessione
    private String cSessionCook = "";

    // ID
    private String cLeft = "";
    private String cRight = "";
    private boolean bDebug = false;

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Fastwebnet: login init");

            // Trovo il SID
            if (bDebug) {
                log.error("Fastwebnet: " + cServer + "/mail/Login?domain=default&style=default&plain=0");
            }
            String sb = getPage(cServer + "/mail/Login?domain=default&style=default&plain=0").toString();
            if (bDebug) {
                putFile(sb, "-login");
            }

            int nSid = sb.indexOf(" name=\"SID\" value=\"");
            if (nSid != -1) {
                String cSid = sb.substring(nSid + 19, nSid + 19 + 40);

                // Preparo I parametri per il post
                String cPost = "AUTH=&ACT=&SEQ=&SID=" + cSid + "&FLIST=timezone&timezone=-60&userid=" + URLEncoder.encode(cUser, CharsetCoding.UTF_8) + "&password=" + URLEncoder.encode(cPwd, CharsetCoding.UTF_8) + "&style=default&domain=default";
                String cCookie = ""; //"WEBUSER=" +URLEncoder.encode( cUser ) +"; WEBPASS=" +URLEncoder.encode( cPwd ) +"; ss=1; TEMPLATE=default";

                sb = postPage(cServer + "/mail/LoginPlain", cCookie, cPost).toString();
                if (bDebug) {
                    putFile(sb, "-LoginPlain");
                }

                cSessionCook = getCookie();

                // Autenticato, provo a recuperare l'inbox
                int nIn = sb.indexOf("/mail/MessageList?sid=");
                int nIn2 = sb.indexOf("\"", nIn);
                if (nIn != -1 && nIn2 != -1) {
                    String cPage = sb.substring(nIn, nIn2);
                    sb = getPage(cServer + cPage + "&allmsgs=1", cSessionCook).toString();

                    // Elaboro I risultati
                    int nPos = 0;
                    nPos = 0;
                    while (nPos != -1) {   //12345678901234567890123456789012345678901234567890
                        nPos = sb.indexOf("<input type=\"checkbox\" name=\"msguid\" value=\"", nPos);
                        if (nPos == -1) {
                            break;
                        }
                        int nPosEnd = sb.indexOf("\"", nPos + 44);
                        if (nPosEnd == -1) {
                            break;
                        }
                        try {
                            int nSiz = sb.indexOf("/mail/MessageRead?", nPosEnd);
                            if (nSiz != -1) {
                                int nSizEnd = sb.indexOf("'", nSiz);
                                if (nSizEnd != -1) {
                                    String cPos = sb.substring(nSiz, nSizEnd);
                                    int nPos1 = cPos.indexOf("&uid=");
                                    if (nPos1 != -1) {
                                        int nPos2 = cPos.indexOf("&", nPos1 + 1);
                                        if (nPos2 != -1) {
                                            cLeft = cPos.substring(0, nPos1 + 5);
                                            cRight = cPos.substring(nPos2);

                                            int nLen = 1;
                                            String cSiz = "";
                                            try {                       //123456789012345678901234567890
                                                int nSiz2 = sb.indexOf("size=\"1\">", nSiz);
                                                if (nSiz2 != -1) {
                                                    nSiz2 = sb.indexOf("size=\"1\">", nSiz2 + 1);
                                                    if (nSiz2 != -1) {
                                                        int nSizEnd2 = sb.indexOf("</font>", nSiz2 + 1);
                                                        if (nSizEnd2 != -1) {
                                                            cSiz = sb.substring(nSiz2 + 9, nSizEnd2).replace('<', ' ').replace('>', ' ').replace('b', ' ').replace('/', ' ').trim();
                                                            int nSpa = cSiz.indexOf(' ');
                                                            if (cSiz.charAt(cSiz.length() - 2) == 'K') {
                                                                nLen = Double.valueOf(cSiz.substring(0, cSiz.length() - 2)).intValue() * 1024;
                                                            } else {
                                                                nLen = Double.valueOf(cSiz.substring(0, nSpa)).intValue();
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Throwable ex) {
                                                log.info("Fastwebnet: errore di calcolo size: [" + cSiz + "] segnalare all'autore di html2pop3");
                                            }

                                            addEmailInfo(cPos.substring(nPos1 + 5, nPos2), nLen);
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                        }

                        nPos = nPosEnd;
                    }
                    bRet = true;
                }
            }
            log.error("Fastwebnet: login end");

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
            log.error("Fastwebnet: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Fastwebnet: getmail ID (" + cMsgId + ")");
            //                     log.info(cServer +cLeft +cMsgId +cRight);
            String sb = getPage(cServer + cLeft + cMsgId + cRight, cSessionCook).toString();

            POP3Message pop3 = new POP3Message();
            pop3.setDa(getInfo(sb, "Da:"));
            pop3.setA(getInfo(sb, "A:"));
            pop3.setCc(getInfo(sb, "Cc:"));
            pop3.setOggetto(getInfo(sb, "Oggetto:"));
            pop3.setData(getInfo(sb, "Spedito:"));
            pop3.setBody(getMessage(sb));
            pop3.addHTMLAttach("source.htm", sb.getBytes());

            // TOP optimization
            if (bAll || nLine > 0) {
                //12345678901234567890
                int nAtt = sb.indexOf("/file/Attachment/");
                while (nAtt != -1) {
                    int nStart = sb.indexOf(">", nAtt);
                    int nEnd = sb.indexOf("<", nStart);
                    int nVir = sb.indexOf("\"", nAtt);
                    if (nStart != -1 && nEnd != -1 && nVir != -1) {
                        String cSubPage = sb.substring(nAtt, nVir);
                        byte[] cFile = getPageBytes(cServer + cSubPage, cSessionCook);
                        pop3.addAttach(sb.substring(nStart + 1, nEnd), cFile);
                    }
                    nAtt = sb.indexOf("/file/Attachment/", nAtt + 1);
                }
            }

            oMail.append(pop3.getMessage(nLine, bAll));

            //oMail = getPage( "http://webmail.fastwebnet.it/Session/" +cSession +"/MessagePart/INBOX/" +cMsgId +"-P.txt", cSessionCook, nLine, bAll );
            //if( getContentType().indexOf("text/plain")==-1 ) oMail = null;
            log.error("Fastwebnet: getmail end");

        } catch (FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : oMail.toString());
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Fastwebnet: delmessage");

            String cMsgId = getMessageID(nPos);

            log.error("Fastwebnet: delmessage " + cMsgId);

            //StringBuffer oMail = getPage( "http://webmail.fastwebnet.it/Session/" +cSession +"/Mailbox.wssp?Mailbox=INBOX&MSG=" +cMsgId +"&Delete=&", cSessionCook );
            //bRet = true;
            log.error("Fastwebnet: DA FARE");

            log.error("Fastwebnet: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    private String getMessage(String cMail) {
        String cRet = "";        //123456789012345678901234567890123456789012345678901234567890
        int nInfo = cMail.indexOf("<!-- INBOX:MESSAGE:CONTENT -->");
        int nInfo2 = cMail.indexOf("<!-- INBOX:MESSAGE:/CONTENT -->");
        if (nInfo != -1 && nInfo2 != -1) {
            cRet = cMail.substring(nInfo + 30 + 2, nInfo2).trim();
        }
        return cRet;
    }

    private String getInfo(String cMail, String cInfo) {
        String cRet = "";
        int nInfo = cMail.indexOf("<b>" + cInfo + "</b>");
        int nInfo2 = cMail.indexOf("size=\"1\">", nInfo);
        int nInfo3 = cMail.indexOf("</font>", nInfo2);
        if (nInfo != -1 && nInfo2 != -1 && nInfo3 != -1) {
            cRet = replace(cMail.substring(nInfo2 + 9, nInfo3), "&nbsp;", "").trim();
            cRet = replace(cRet, "&quot;", "\"");
            cRet = replace(cRet, "&lt;", "<");
            cRet = replace(cRet, "&gt;", ">");
        }
        return cRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginFastwebnet fastwebnet = new PluginFastwebnet();
        if (fastwebnet.login(args[0], args[1])) {
            int nNum = fastwebnet.getMessageNum();
            int nSiz = fastwebnet.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + fastwebnet.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + fastwebnet.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + fastwebnet.getMessage(nPos));
            }
        }
    }

}
