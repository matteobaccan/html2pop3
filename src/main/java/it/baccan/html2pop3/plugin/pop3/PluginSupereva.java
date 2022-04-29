/*
 * supereva plugin
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
 * Title:        supereva HTML2POP3
 * Description:  Convertitore da HTML a POP3 per supereva.it
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.utils.CharsetCoding;
import java.net.*;
import java.util.*;

import it.baccan.html2pop3.utils.message.*;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginSupereva extends POP3Base implements POP3Plugin {

    // Server di riferimento
    private String cServer = "http://email.dada.it";

    // Property per variabili hidden
    private Properties p = new Properties();

    // Sessione
    private String cSessionCook = "";

    //private boolean bDebug = true;
    private boolean bDebug = false;

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Supereva: login init");

            StringBuffer sb = new StringBuffer();

            // Preparo I parametri
            //String cPost = "username=" +URLEncoder.encode( cUser ) +"&password=" +URLEncoder.encode( cPwd ) +"&Submite=%20%20Entra%20%20&prov=";
            //String cPost = "username=" +URLEncoder.encode( cUser ) +"&password=" +URLEncoder.encode( cPwd ) +"&uri=http%3A%2F%2Fit.email.dada.net%2Fcgi-bin%2Flogin.chm&act=1&dontask=1";
            String cPost = "username=" + URLEncoder.encode(cUser, CharsetCoding.UTF_8) + "&password=" + URLEncoder.encode(cPwd, CharsetCoding.UTF_8) + "&uri=http%3A%2F%2Femail.dada.it%2Fcgi-bin%2Flogin.chm&act=1&dontask=1";

            //String cLogin = postPage( cServer +"/cgi-bin/login.chm", null, cPost ).toString();
            String cLogin = postPage("http://sso.dada.it/cgi-bin/sso/login.cgi", null, cPost).toString();
            if (bDebug) {
                putFile(cLogin, "-login.cgi");
            }

            String cNewPage = getLocation();
            String cNewCook = getCookie();

            log.info("Supereva: " + cNewPage);
            log.info("Supereva: " + cNewCook);

            cLogin = getPage(cNewPage, cNewCook).toString();
            if (bDebug) {
                putFile(cLogin, "-login.cgi-forward");
            }

            cNewPage = cServer + getLocation();
            //cNewPage = getLocation();
            //cNewCook = getCookie();

            log.info("Supereva: " + cNewPage);
            //log.info(cNewCook);

            cLogin = getPage(cNewPage, cNewCook).toString();
            //putFile(cLogin,"3");
            if (bDebug) {
                putFile(cLogin, "-login.cgi-forward2");
            }

            //if( cLogin.indexOf( "main.chm?changefolder=" )!=-1 ){
            if (cLogin.indexOf("/cgi-bin/main.chm") != -1) {
                bRet = true;

                // Passata la pagina di login, accedo alla inbox
                //cSessionCook = getCookie();
                cSessionCook = cNewCook;

                log.error("Supereva: login inbox");

                int nPage = 0;
                while (true) {
                    nPage++;
                    sb = getPage("http://email.dada.it/cgi-bin/main.chm?changefolder=changefolder&mailfolder=in&mlt_msgs=" + nPage, cSessionCook);

                    // Cerco la lista dei messaggi
                    // Cerco "nrmail03.chm?setflags
                    //if( nPage==1 ) putFile( sb, "lista" );
                    if (bDebug) {
                        putFile(sb, "-main.chm-page" + nPage);
                    }
                    //cNewPage = getLocation();
                    //log.info("Supereva: "+cNewPage);
                    String cPage = sb.toString();

                    int nPos = 0;
                    nPos = 0;
                    int nEmail = 0;
                    while (nPos != -1) {
                        nPos = cPage.indexOf("\"/cgi-bin/nrmail03.chm?setflags", nPos);
                        //log.info( "Supereva: 2" );
                        if (nPos == -1) {
                            break;
                        }
                        int nPosEnd = cPage.indexOf("\"", nPos + 1);
                        //log.info( "Supereva: 3" );
                        if (nPosEnd == -1) {
                            break;
                        }

                        int nLen = 1;
                        String cSiz = "";
                        try {
                            String cFind = "</tr>";
                            int nSiz = cPage.indexOf(cFind, nPosEnd);
                            if (nSiz != -1) {
                                int nSizEnd = cPage.lastIndexOf("<td", nSiz);
                                if (nSizEnd != -1) {
                                    cSiz = cPage.substring(nSizEnd, nSiz);
                                    cSiz = filter(cSiz, "<", ">");
                                    cSiz = replace(cSiz, "&nbsp;", "");
                                    cSiz = cSiz.trim();
                                    if (cSiz.charAt(cSiz.length() - 1) == 'K') {
                                        nLen = Double.valueOf(cSiz.substring(0, cSiz.length() - 1)).intValue() * 1024;
                                    } else {
                                        nLen = Double.valueOf(cSiz).intValue();
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            log.info("Supereva: errore di calcolo size: [" + cSiz + "] segnalare all'autore di html2pop3");
                        }

                        String cPos = cPage.substring(nPos + 1, nPosEnd);

                        // Il CRC e' meglio farlo sull'url pulito
                        String cPosCRC = replace(cPos, "=new/", "=cur/");

                        // Provo a identificare il msgnum e di quello fare il crc
                        // &msgnum=cur/1091161523.3506.2170416.adamo30&
                        int nPos2 = cPosCRC.indexOf("&msgnum=");
                        int nPos3 = cPosCRC.indexOf("&", nPos2 + 1);

                        // Nuovo valore
                        cPosCRC = cPosCRC.substring(nPos2, nPos3);

                        // Ora dovrebbe andare bene il dato e' calcolato su un valore che non cambia
                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        crc.update(cPosCRC.getBytes(), 0, cPosCRC.length());
                        String cCrc = "" + crc.getValue();

                        p.put(cCrc, cPos);

                        if (addEmailInfo(cCrc, nLen)) {
                            nEmail++;
                        }
                        nPos = nPosEnd;
                    }
                    if (nEmail == 0) {
                        break;
                    }
                }
            }
            log.error("Supereva: login end");

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    private String getMessageIDFull(int nPos) {
        String cID = getMessageID(nPos);
        return (String) p.get(cID);
    }

    /**
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    @Override
    public String getMessage(int nPos, int nLine, boolean bAll) {
        StringBuffer oMail = new StringBuffer();
        try {
            log.error("Supereva: getmail init");

            String cMsgId = getMessageIDFull(nPos);

            log.error("Supereva: getmail ID (" + cMsgId + ")");

            String sb = getPage(cServer + cMsgId, cSessionCook).toString();
            //putFile( sb );

            // Workaround per download doppi e mancata cancellazione
            String cID = getMessageID(nPos);
            cMsgId = replace(cMsgId, "=new/", "=cur/");
            p.put(cID, cMsgId);
            // Workaround per download doppi e mancata cancellazione

            POP3Message pop3 = new POP3Message();
            pop3.setCharset("ISO-8859-1");
            pop3.setDa(getInfo(sb, "Da:"));
            pop3.setA(getInfo(sb, "A:"));
            pop3.setCc(getInfo(sb, "CC:"));
            pop3.setOggetto(getInfo(sb, "Oggetto:"));
            pop3.setData(formatDate(getInfo(sb, "Data:")));
            pop3.setBody(getMessage(sb));
            pop3.addHTMLAttach("source.htm", sb.getBytes());

            // TOP optimization
            if (bAll || nLine > 0) {

                int nAtt = sb.indexOf("<a href=\"#a_/attach/");
                while (nAtt != -1) {
                    int nSla = sb.indexOf("\"", nAtt + 9);
                    int nTit1 = sb.indexOf(">", nSla);
                    int nTit2 = sb.indexOf("<", nTit1);
                    if (nSla != -1 && nTit1 != -1 && nTit2 != -1) {
                        String cSubPage = sb.substring(nAtt + 12, nSla);
                        String cFileName = sb.substring(nTit1 + 1, nTit2);

                        log.error("Supereva: " + cServer + cSubPage);
                        log.error("Supereva: " + cFileName);

                        byte[] cFile = getPageBytes(cServer + cSubPage, cSessionCook);
                        pop3.addAttach(cFileName, cFile);
                    }
                    nAtt = sb.indexOf("<a href=\"#a_/attach/", nAtt + 1);
                }
            }

            oMail.append(pop3.getMessage(nLine, bAll));

            log.error("Supereva: getmail end");

        } catch (FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return oMail == null ? null : oMail.toString();
    }

    /**
     *
     * @param nPos
     * @return
     */
    @Override
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Supereva: delmessage");

            String cMsgId = getMessageIDFull(nPos);

            log.error("Supereva: delmessage " + cMsgId);

            String cMsg = "";
            int nM = cMsgId.indexOf("msgnum=");
            if (nM != -1) {
                int nM2 = cMsgId.indexOf("&", nM);
                if (nM2 != -1) {
                    cMsg = cMsgId.substring(nM + 7, nM2);
                    String cPost = "mailfolder=in&max_msg=1&MSG0=" + URLEncoder.encode(cMsg, CharsetCoding.UTF_8) + "&plmove=Sposta+il+messaggio+in&tobox=trash&tobox=sent";

                    //String cDelete =
                    postPage(cServer + "/cgi-bin/del_mov.cgi", cSessionCook, cPost).toString();
                    bRet = true;
                }
            }

            log.error("Supereva: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    private String getMessage(String cMail) {
        String cRet = "";        //12345678 901234567 89012345 678901234 56789012345678901234567890
        int nInfo = cMail.indexOf("<div id=\"mailbody\" class=\"mailtext\">");
        if (nInfo != -1) {
            //int nInfo2 = cMail.indexOf("<table width=\"100%\" border=\"0\" cellpadding=\"2\" cellspacing=\"0\" id=\"readmsg\">",nInfo);
            //int nInfo3 = cMail.indexOf("<div id=allegati>",nInfo);
            //if( nInfo3<nInfo2 && nInfo3!=-1 ) nInfo2 = nInfo3;

            int nInfo2 = cMail.indexOf("<!-- google_ad_section_end -->", nInfo);
            if (nInfo2 != -1) {
                cRet = cMail.substring(nInfo + 36, nInfo2).trim();
            }
        }
        return cRet;
    }

    private String getInfo(String cMail, String cInfo) {
        String cRet = "";
        int nInfo = cMail.indexOf("<b>" + cInfo + "</b>");
        int nInfo2 = cMail.indexOf("<th", nInfo);
        int nInfo3 = cMail.indexOf(">", nInfo2);
        int nInfo4 = cMail.indexOf("<", nInfo3);
        if (nInfo != -1 && nInfo2 != -1 && nInfo3 != -1 && nInfo4 != -1) {
            cRet = replace(cMail.substring(nInfo3 + 1, nInfo4), "&nbsp;", "").trim();
            cRet = replace(cRet, "&quot;", "\"");
            cRet = replace(cRet, "&lt;", "<");
            cRet = replace(cRet, "&gt;", ">");
            cRet = replace(cRet, "" + (char) 13, "");
            cRet = replace(cRet, "" + (char) 10, "");
        }
        return cRet;
    }

    private String formatDate(String cDate) {
        String cRet = getCurDate();

        try {
            //    21 Mar 2004 - 15:45 : old
            //Tue 15 Jul 2008 - 23:57 : new

            int nYear = 0;
            int nMonth = 0;
            int nDay = 0;
            int nH = 0;
            int nM = 0;
            int nS = 0;

            int nTok = 0;
            StringTokenizer st = new StringTokenizer(cDate, " -:");
            while (st.hasMoreTokens()) {
                String cTok = st.nextToken();
                nTok++;
                if (nTok == 2) {
                    nDay = Integer.parseInt(cTok);
                } else if (nTok == 3) {
                    nMonth = month2numEng(cTok);
                } else if (nTok == 4) {
                    nYear = Integer.parseInt(cTok) - 1900;
                } else if (nTok == 5) {
                    nH = Integer.parseInt(cTok);
                } else if (nTok == 6) {
                    nM = Integer.parseInt(cTok);
                } else if (nTok == 7) {
                    nS = Integer.parseInt("0");
                }
            }
            if (nTok > 4) {
                cRet = formatDate(nYear, nMonth, nDay, nH, nM, nS);
            }
        } catch (Throwable ex) {
            log.info("Supereva: errore di formattazione data: [" + cDate + "] segnalare all'autore di html2pop3");
        }

        return cRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginSupereva supereva = new PluginSupereva();
        if (supereva.login(args[0], args[1])) {
            int nNum = supereva.getMessageNum();
            int nSiz = supereva.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + supereva.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + supereva.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + supereva.getMessage(nPos));
            }
        }
    }

}
