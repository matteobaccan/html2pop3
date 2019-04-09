/*
 * Tiscali plugin
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
 * Title:        tiscali HTML2POP3
 * Description:  Convertitore da HTML a POP3 per tiscali.it
 * Copyright:    Copyright (c) 2003
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.net.URLEncoder;
import java.util.*;

import it.baccan.html2pop3.utils.*;
import it.baccan.html2pop3.utils.message.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class plugintiscali extends pop3base implements pop3plugin {

    /**
     *
     */
    public plugintiscali() {
    }

    // Server di riferimento  
    private String cServer = "http://mail.tiscali.it";

    // Property per variabili hidden
    private Properties p = new Properties();

    // Sessione
    private String cSessionCook = "";

    private boolean bDebug = true;
    //private boolean bDebug = false;

    // Login
    private boolean bLogin = false;

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        //boolean bRet = false;
        bLogin = false;
        try {
            log.error("Tiscali: login init");

            String cFakeCook = "";//roundcube_sessid=7qlnckkjcharvgmlao4sb2bdl3; __utma=133587473.516011248.1400799269.1400799269.1400799269.1; __utmb=133587473.4.10.1400799269; __utmc=133587473; __utmz=133587473.1400799269.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); CLASS=Z2; TiscaliLastUser=testplugin01; oroscopo_dasboard_mail=ariete; geolocationNew=ipFROM%3D1587941376%2CipTO%3D1587942399%2CcountrySHORT%3DIT%2CcountryLONG%3DITALY%2CipREGION%3DLAZIO%2CipCITY%3DROMA%2CipLATITUDE%3D41.90%2CipLONGITUDE%3D12.48%2CipZIPCODE%3D-%2CipTIMEZONE%3D%2CipISP%3D%2CipDOMAIN%3D%2CipRequest%3D94.166.17.105%2Cerror%3D0%2Cversion%3D2.0; geolocation=ipFROM%3D1587941376%2CipTO%3D1587942399%2CcountrySHORT%3DIT%2CcountryLONG%3DITALY%2CipREGION%3DLAZIO%2CipCITY%3DROMA%2CipLATITUDE%3D41.90%2CipLONGITUDE%3D12.48%2CipZIPCODE%3D-%2CipTIMEZONE%3D%2CipISP%3D%2CipDOMAIN%3D%2CipRequest%3D94.166.17.105%2Cerror%3D0%2Cversion%3D2.0; mailviewsplitter=205; cto_tiscali=meteo300600%2Cansa300%2Cansa728%2Ctis300%2Ctis728%2Ctislp300%2Cmaillp300%2Cansalp300%2Cmeteo300600lp%2Cmail120%2Cmailp120%2Cansa300600%2C; roundcube_sessauth=S6ac8107200cef829e139a4790ccf5f7a9045f34f; ssoUser=testplugin01; ssoDomain=tiscali.it; ssoToken=qyFY1AJA%2FsCPflyytoIi1KPv7Qu4SCDA9Z%2BpvCBRO9NKAYppovLds6s1%2FWrmoWiW;";
            String fake = getPage("http://mail.tiscali.it/?_action=plugin.tiscali_dashboard&_task=mail", cFakeCook).toString();

            // Prendo il token
            String sbToken = getPage(cServer).toString();
            int nTok1 = sbToken.indexOf("<input type=\"hidden\" name=\"_token\" value=\"");
            int nTok2 = sbToken.indexOf("\"", nTok1 + 42);
            String _token = sbToken.substring(nTok1 + 42, nTok2);

            // Trovo il SID
            String cCook = getCookie();
            String cDomain = cUser.substring(cUser.indexOf("@") + 1).toLowerCase();
            cUser = URLEncoder.encode(cUser.substring(0, cUser.indexOf("@")).toLowerCase());
            cPwd = URLEncoder.encode(cPwd);

            p.put("userid", cUser);
            p.put("password", cPwd);
            p.put("domain", cDomain);

            String cPostLogin = "_token=" + _token
                    + "&_task=login"
                    + "&_action=login"
                    + "&_timezone=2"
                    + "&_url=_task%3Dlogin%26_err%3Dsession"
                    + "&_user=" + cUser
                    + "&_pass=" + cPwd;

            // Chiedo il logout, nel caso ci fosse una sessione attiva
            String sb = postPage(cServer, cCook, cPostLogin).toString();

            String cAllCookie = getCookie();
            cSessionCook = "";
            cSessionCook += getFullCook(cAllCookie, "roundcube_sessid");
            cSessionCook += getFullCook(cAllCookie, "roundcube_sessauth");
            cSessionCook += getFullCook(cAllCookie, "TiscaliLastUser");
            cSessionCook += getFullCook(cAllCookie, "ssoUser");
            cSessionCook += getFullCook(cAllCookie, "ssoDomain");
            cSessionCook += getFullCook(cAllCookie, "ssoToken");


            /*
            if(bDebug) log.error( sb );
            if(bDebug) log.error( getLocation() );
            if(bDebug) log.error( cSessionCook );
            if(bDebug) log.error( getCook(cSessionCook,"ssoToken") );
            if(bDebug) log.error( ""+getCook(cSessionCook,"ssoToken").length() );
             */
            String cDshboard = cServer + getLocation();
            /*cSessionCook += "WT_FPC=id=94.164.150.142-450949216.30308979:lv=1400683140761:ss=1400679238842; "
                    + "__utma=133587473.1979384475.1373709952.1400791240.1400796335.28; "
                    + "__utmz=133587473.1400791240.27.9.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); "
                    + "_ga=GA1.2.1032224085.1373273767; "
                    + "location=Piemonte%257CAsti%257CAsti%257C606; "
                    + "oroscopo_dasboard_mail=ariete; "
                    + "CLASS=Z2; "
                    + "geolocationNew=ipFROM%3D1587941376%2CipTO%3D1587942399%2CcountrySHORT%3DIT%2CcountryLONG%3DITALY%2CipREGION%3DLAZIO%2CipCITY%3DROMA%2CipLATITUDE%3D41.90%2CipLONGITUDE%3D12.48%2CipZIPCODE%3D-%2CipTIMEZONE%3D%2CipISP%3D%2CipDOMAIN%3D%2CipRequest%3D94.166.17.105%2Cerror%3D0%2Cversion%3D2.0; "
                    + "geolocation=ipFROM%3D1587941376%2CipTO%3D1587942399%2CcountrySHORT%3DIT%2CcountryLONG%3DITALY%2CipREGION%3DLAZIO%2CipCITY%3DROMA%2CipLATITUDE%3D41.90%2CipLONGITUDE%3D12.48%2CipZIPCODE%3D-%2CipTIMEZONE%3D%2CipISP%3D%2CipDOMAIN%3D%2CipRequest%3D94.166.17.105%2Cerror%3D0%2Cversion%3D2.0; "
                    + "mailviewsplitter=205; "
                    + "__utmc=133587473; "
                    + "__utmb=133587473.4.10.1400796335; "
                    + "cto_tiscali=meteo300600%2Cansa300%2Cansa728%2Ctis300%2Ctis728%2Cuni728%2Cmail300%2Ctislp300%2Cmaillp300%2Cansalp300%2Cmeteo300600lp%2Cmail120%2Cmailp120%2Cansa300600%2C;";            
            //*/
            String dashboard = getPage(cDshboard, cSessionCook).toString();

            String inbox = getPage(cServer + "/?_page=1&_task=mail&_mbox=INBOX", cSessionCook).toString();

            int nUrl1 = sb.indexOf("&t=");
            int nUrl2 = sb.indexOf("&", nUrl1 + 1);

            String cT = sb.substring(nUrl1 + 3, nUrl2);
            p.put("t", cT);
            if (bDebug) {
                log.error(cT);
            }

            //cPost = "d=" +cDomain +"&u=" +cUser;
            //sb = postPage( cServer +"/cp/ps/Main/Layout?d=" +cDomain +"&t=" +cT +"&l=it&s=" +getCook(cSessionCook,"s") +"&saveCredentials=null", cCook, cPost ).toString();
            if (getCook(cSessionCook, "ssoToken").length() > 0) {
                bLogin = true;
            } else {
                String cErr = getPar(getLocation(), "errorString");
                if (cErr.length() > 0) {
                    setLastErr(cErr);
                    log.error("Tiscali: login error (" + cErr + ")");
                }
            }
            log.error("Tiscali: login end");

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bLogin;
    }

    /**
     *
     * @return
     */
    public boolean list() {
        boolean bRet = false;
        try {
            if (bLogin) {
                log.error("Tiscali: list init");
                int nFi = 1;
                while (true) {
                    String cGet = "/cp/ps/Mail/commands/SyncFolder?d=" + p.getProperty("domain") + "&u=" + p.getProperty("userid") + "&t=" + p.getProperty("t") + "&l=it";
                    // 1 16 31
                    //String cGet = "/cp/ps/Mail/EmailList?search=&sh=&d=" +p.getProperty("domain") +"&fi=" +nFi +"&sd=Desc&an=" +p.getProperty("userid") +"&l=it&fp=INBOX&sc=&u=" +p.getProperty("userid") +"&t=" +getCook(cSessionCook,"ssoToken") +"&ss=";

                    if (bDebug) {
                        log.error(cServer + cGet);
                    }

                    String cPost = "accountName=" + p.getProperty("userid") + "&folderPath=&listPosition=1&sortColumn=&sortDirection=Desc&anotherSearch=false";
                    String sb = postPage(cServer + cGet, cSessionCook, cPost).toString();

                    if (bDebug) {
                        log.error(sb);
                    }

                    // cp:range-length = dimensione
                    // cp:list-size    = elementi
                    // cp:item         = elemento corrente

                    /*
                 <cp:item xmlns:cp="http://www.cp.net/cp" style="display:none" containerid="mailRowContainer" type="mailRow">
                 <cp:param name="pid">4c786d7db991b0e2ebb67e1742fe1b0f</cp:param>
                 <cp:param name="uid">2</cp:param>
                 <cp:param name="accountName">testplugin01</cp:param>
                 <cp:param name="folderLabel">Posta in arrivo</cp:param>
                 <cp:param name="folderDisplayStyle">display:none;</cp:param>
                 <cp:param name="folderPath">INBOX</cp:param>
                 <cp:param name="date">21/09/2010 1.04</cp:param>
                 <cp:param name="size">643 B</cp:param>
                 <cp:param name="cssClass">mailListItem</cp:param>
                 <cp:param name="subjectCss">mailListSubjectCol</cp:param>
                 <cp:param name="toCss">mailListFromCol</cp:param>
                 <cp:param name="fromCss">mailListFromCol</cp:param>
                 <cp:param name="dateCss">mailListDateCol</cp:param>
                 <cp:param name="folderCss">mailListFolderCol</cp:param>
                 <cp:param name="to">&lt;testplugin01@tiscali.it&gt;</cp:param>
                 <cp:param name="dateGroup">today</cp:param>
                 <cp:param name="subject">Avviso: la casella di posta Tiscali e` piena. </cp:param>
                 <cp:param name="from">Servizio di posta Tiscali</cp:param>
                 <cp:param name="fromAddress">postmaster@tiscali.it</cp:param>
                 <cp:param name="typeImg">mailList_blank</cp:param>
                 <cp:param name="typeAlt"></cp:param>
                 <cp:param name="attachImg">mailList_blank</cp:param>
                 <cp:param name="attachAlt"/>
                 <cp:param name="flaggedImg">mailList_flag_off</cp:param>
                 <cp:param name="flaggedAlt"/>
                 <cp:param name="priorityImg">mailList_priority_urgent</cp:param>
                 <cp:param name="priorityAlt"/>
                 <cp:param name="isDeleted">false</cp:param>
                 <cp:param name="verifiedSenderTitleMessage"></cp:param>
                 <cp:param name="verifiedSender"></cp:param>
                 <cp:param name="isVerificationEnabled">false</cp:param>
                 <cp:param name="isRecommendedEnabled">false</cp:param>
                 <cp:param name="isAdvancedDEMEnabled">false</cp:param>
                 <cp:param name="isBannerSyncEnabled">false</cp:param>
                 <cp:param name="verifiedOptions">0</cp:param>
                 </cp:item>
                     */
                    // Email estratte
                    int nEmail = 0;

                    // Estrazione
                    int nPos = 0;
                    while (nPos != -1) {
                        nPos = sb.indexOf("<cp:item xmlns:cp=\"http://www.cp.net/cp\" style=\"display:none\" containerid=\"mailRowContainer\" type=\"mailRow\">", nPos);
                        if (nPos == -1) {
                            break;
                        }
                        int nPosEnd = sb.indexOf("</cp:item>", nPos);
                        if (nPosEnd == -1) {
                            break;
                        }
                        String cEmail = sb.substring(nPos, nPosEnd);

                        int nLen = 1;
                        String cSiz = "";
                        try {                        //123456789012345 67890 1234567890
                            int posLenEnd = cEmail.indexOf("<cp:param name=\"size\">", nPos);
                            if (posLenEnd != -1) {
                                int posLenStart = sb.indexOf("</cp:param>", posLenEnd);
                                if (posLenStart != -1) {
                                    cSiz = sb.substring(posLenEnd + 21, posLenStart).trim();  // espresso in K
                                    nLen = new Double(Double.valueOf(cSiz).doubleValue() * 1024).intValue();
                                }
                            }
                        } catch (Throwable ex) {
                            log.error("tiscali: errore di calcolo size: [" + cSiz + "] segnalare all'autore di html2pop3");
                        }

                        if (addEmailInfo(cEmail, nLen)) {
                            nEmail++;
                        }
                        nPos = nPosEnd;
                    }
                    nFi += 15;
                    if (nEmail == 0) {
                        break;
                    }
                }
                log.error("Tiscali: list end");
                bRet = true;
            }
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
        StringBuffer oMail = null;
        try {
            log.error("Tiscali: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Tiscali: getmail ID (" + cMsgId + ")");

            // "/cp/ps/Mail/commands/LoadMessage?d=tiscali.it&u=testplugin01&t=d28124111d86&l=it&verifiedOptions=0&protoclass=mailOpenedMessageProtoStandard&attachmentsContainer=mailOpenedMessageAttachmentsContainer&an=testplugin01&fp=INBOX&pid=66606868bb68c461f2f24c274d90aca9&uid=3&alternateContainer=mailOpenedMessageContainer"
            //String cEmail = "/cp/ps/Mail/ViewMsgController?d=" +p.getProperty("domain") +"&sh=&listPositionNextForSearch=16&sc=&sd=Desc&u=" +p.getProperty("userid") +"&an=" +p.getProperty("userid") +"&t=" +getCook(cSessionCook,"ssoToken") +"&l=it&fpOrigin=INBOX&ss=&listPositionPrevForSearch=0&fp=INBOX&uid=" +cMsgId +"&sl=2&fi=1";
            String cEmail = "/cp/ps/Mail/ViewMsgControllerReal?d=" + p.getProperty("domain") + "&sh=&listPositionNextForSearch=16&sc=&sd=Desc&u=" + p.getProperty("userid") + "&an=" + p.getProperty("userid") + "&t=" + getCook(cSessionCook, "ssoToken") + "&l=it&fpOrigin=INBOX&ss=&listPositionPrevForSearch=0&fp=INBOX&uid=" + cMsgId + "&sl=2&fi=1";
            String sb = getPage(cServer + cEmail, cSessionCook).toString();
            //log.error( sb );
            if (sb.length() > 0
                    && sb.indexOf("Da:") != -1
                    && sb.indexOf("Inviato:") != -1
                    && sb.indexOf("A:") != -1
                    && sb.indexOf("Cc:") != -1
                    && sb.indexOf("Oggetto:") != -1) {

                // Prendo il body
                //String cBody = getPage( cServer +"/cp/ps/Mail/IMsgDetails?d=" +p.getProperty("domain") +"&u=" +p.getProperty("userid") +"&l=it", cSessionCook ).toString();
                //String cBody = getPage( cServer +"/cp/ps/Mail/MsgBody?d=" +p.getProperty("domain") +"&u=" +p.getProperty("userid") +"&l=it", cSessionCook ).toString();
                String cBody = "";
                int nBody = sb.indexOf("/cp/ps/Mail/MsgBody?");
                int nBodyEnd = sb.indexOf("\"", nBody);
                //log.info( ""+nLine +bAll );
                //log.info( cServer +sb.substring( nBody, nBodyEnd ));
                if (nBody != -1 && nBodyEnd != -1) {
                    cBody = getPage(cServer + sb.substring(nBody, nBodyEnd), cSessionCook).toString();
                }

                // Tolgo l'ultimo script
                int nEnd = cBody.lastIndexOf("<script>");
                if (nEnd != -1) {
                    cBody = cBody.substring(0, nEnd);
                }

                // Tolgo il primo script
                int nIniS = cBody.indexOf("<script");
                int nEndS = cBody.indexOf("/script>", nIniS + 1);
                if (nIniS != -1 && nEndS != -1) {
                    cBody = cBody.substring(0, nIniS) + cBody.substring(nEndS + 8);
                }

                if (cBody.length() > 0) {
                    String cBodyAttach = cBody;

                    //log.error( cBodyAttach );
                    // Serve per puntare in modo corretto le immagini dentro le email
                    // 13:20:39 martedi' 19 settembre 2006
                    // Al momento non risolve ancora il problema
                    cBodyAttach = string.replace(cBodyAttach, "src=\"/cp/ps/Mail/ViewAttachment", "src=\"" + cServer + "/cp/ps/Mail/ViewAttachment");

                    POP3Message pop3 = new POP3Message();
                    pop3.setCharset(POP3Message.ISO_8859_1);

                    //String cDA = getInfo( sb, "\t\t\t\t\t\tDa:" );
                    String cDA = getInfo(sb, "<label class=\"custom\">Da:");
                    cDA = getParQ(cDA);
                    pop3.setDa(cDA);

                    //pop3.setA(    getInfo( sb, "\t\t\t\t\t\tA:" ) );
                    pop3.setA(getInfo(sb, "<label class=\"custom\">A:"));
                    //pop3.setCC(   getInfo( sb, "\t\t\t\t\t\tCc:" ) );
                    pop3.setCc(getInfo(sb, "<label class=\"custom\">Cc:"));
                    pop3.setOggetto(getInfo(sb, "<label class=\"custom\">Oggetto:"));

                    //String cData = getInfo( sb, "\t\t\t\t\t\tInviato:" );
                    String cData = getInfo(sb, "<label class=\"custom\">Inviato:");
                    cData = getParQ(cData);
                    pop3.setData(formatDate(cData));

                    pop3.setBody(cBodyAttach);

                    String cChar = getContentTypeCharset();
                    if (cChar.length() != 0) {
                        pop3.setCharset(cChar);
                    }

                    pop3.addHTMLAttach("source.htm", (sb + "\r\n" + cBody).getBytes());

                    // TOP optimization
                    if (bAll || nLine > 0) {
                        int nAtt = sb.indexOf("javascript:onOpenAttachment(");
                        while (nAtt != -1) {
                            int nSla = sb.indexOf("\"", nAtt);
                            int nVir = sb.indexOf("\"", nSla + 1);
                            int nMag = sb.indexOf("</a>", nVir + 1);
                            int nMag2 = sb.lastIndexOf(">", nMag);
                            int nSpa = sb.lastIndexOf("(", nMag);
                            if (nSla != -1 && nVir != -1 && nMag != -1 && nMag2 != -1 && nSpa != -1 && nMag2 < nSpa) {
                                String cSubPage = sb.substring(nSla + 1, nVir);
                                cSubPage = "/cp/ps/Mail/ViewAttachment?d=" + p.getProperty("domain") + "&fp=INBOX&bid=&c=yes&u=" + p.getProperty("userid") + "&an=" + p.getProperty("userid") + "&t=" + getCook(cSessionCook, "ssoToken") + "&uid=" + cMsgId + "&l=it&disposition=attachment&ai=" + cSubPage + "&an=" + p.getProperty("userid") + "&fp=INBOX";
                                byte[] cFile = getPageBytes(cServer + cSubPage, cSessionCook);
                                pop3.addAttach(sb.substring(nMag2 + 1, nSpa).trim(), cFile);
                            }
                            nAtt = sb.indexOf("javascript:onOpenAttachment(", nAtt + 1);
                        }
                    }

                    oMail = new StringBuffer();
                    oMail.append(pop3.getMessage(nLine, bAll));
                }
            }

            log.error("Tiscali: getmail end");

        } catch (java.io.FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : oMail.toString());
    }

    private static boolean bDelete = true;

    /**
     *
     * @param b
     */
    static public void setDelete(boolean b) {
        bDelete = b;
    }

    /**
     *
     * @return
     */
    static public boolean getDelete() {
        return bDelete;
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.error("Tiscali: delmessage init");

            String cMsgId = getMessageID(nPos);

            log.error("Tiscali: delmessage ID " + cMsgId);

            // Post parameter
            String cPage = "/cp/ps/Mail/DeleteEmail?d=" + p.getProperty("domain") + "&fp=INBOX&fi=1&u=" + p.getProperty("userid") + "&an=" + p.getProperty("userid") + "&t=" + getCook(cSessionCook, "ssoToken") + "&uid=" + cMsgId + "&l=it";
            //String sb =
            getPage(cServer + cPage, cSessionCook).toString();

            bRet = true;

            log.error("Tiscali: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     */
    public void delMessageEnd() {
        try {
            log.error("Tiscali: delmessageEnd init");

            if (bDelete) {
                getPage(cServer + "/cp/ps/Mail/EmptyTrash?d=" + p.getProperty("domain") + "&u=" + p.getProperty("userid") + "&an=" + p.getProperty("userid") + "&t=" + getCook(cSessionCook, "ssoToken") + "&l=it&fpX=Trashcan", cSessionCook);
            }

            log.error("Tiscali: delmessageEnd end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
    }

    /**
     *
     * @return
     */
    public Vector getContact() {
        Vector oRet = new Vector();
        try {

            // Prendo i contatti
            //String cBook = "http://mail.google.com/mail/?view=cl&search=contacts&pnl=a&zx=1";
            String cBook = cServer + "/cp/ps/PSPab/viewContacts?d=" + p.getProperty("domain") + "&u=" + p.getProperty("userid") + "&t=" + getCook(cSessionCook, "ssoToken") + "&l=it&sortField=cpabLastName&sortDirection=asc&prevSortField=cpabLastName";
            String oMail = getPage(cBook, cSessionCook).toString();
            log.error("Tiscali: getContact");
            //log.error( oMail );

            int nStart = 0;
            while (true) {
                int nLabel = oMail.indexOf("entity.label=\"", nStart);
                int nLabel2 = oMail.indexOf("\";", nLabel);
                int nEmail = oMail.indexOf("entity.email=\"", nLabel2);
                int nEmail2 = oMail.indexOf("\";", nEmail);
                if (nLabel == -1 || nLabel2 == -1
                        || nEmail == -1 || nEmail2 == -1) {
                    break;
                }
                String[] oEle = {oMail.substring(nLabel + 14, nLabel2), oMail.substring(nEmail + 14, nEmail2)};
                oRet.addElement(oEle);
                nStart = nEmail2;
            }
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return oRet;
    }

    private String getInfo(String cMail, String cInfo) {
        String cRet = "";
        int nInfo = cMail.indexOf(cInfo);
        int nInfo2 = cMail.indexOf("<label", nInfo + cInfo.length());
        if (nInfo2 != -1) {
            nInfo2 = cMail.indexOf(">", nInfo2);
        }
        int nInfo3 = cMail.indexOf("</label>", nInfo2);
        if (nInfo != -1 && nInfo2 != -1 && nInfo3 != -1) {
            cRet = replace(cMail.substring(nInfo2 + 1, nInfo3), "&nbsp;", "").trim();
            cRet = replace(cRet, "&quot;", "\"");
            cRet = replace(cRet, "&lt;", "<");
            cRet = replace(cRet, "&gt;", ">");
            //cRet = replace( cRet, "[", "<" );
            //cRet = replace( cRet, "]", ">" );
        }
        return cRet.trim();
    }

    private String formatDate(String cDate) {
        String cRet = getCurDate();

        // 13/04/2006 11.00
        int nYear = 0;
        int nMonth = 0;
        int nDay = 0;
        int nH = 0;
        int nM = 0;
        int nS = 0;

        int nTok = 0;
        StringTokenizer st = new StringTokenizer(cDate, " /.");
        while (st.hasMoreTokens()) {
            String cTok = st.nextToken();
            nTok++;
            //log.error( "*"+cTok+"*" );
            if (nTok == 1) {
                nDay = Integer.parseInt(cTok);
            } else if (nTok == 2) {
                nMonth = Integer.parseInt(cTok) - 1;
            } else if (nTok == 3) {
                nYear = Integer.parseInt(cTok) - 1900;
            } else if (nTok == 4) {
                nH = Integer.parseInt(cTok);
            } else if (nTok == 5) {
                nM = Integer.parseInt(cTok);
            }
            //else if( nTok==6 ) nS     = Integer.parseInt(      cTok );
        }
        if (nTok > 4) {
            cRet = formatDate(nYear, nMonth, nDay, nH, nM, nS);
        }

        return cRet;
    }

    private String getParQ(String cData) {
        int nPar = cData.indexOf("[");
        int nPar2 = cData.indexOf("]", nPar + 1);
        if (nPar != -1 && nPar2 != -1) {
            cData = cData.substring(nPar + 1, nPar2);
        }
        return cData;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        plugintiscali tiscali = new plugintiscali();
        if (tiscali.login(args[0], args[1])) {
            /*
            tiscali.list();
            int nNum = tiscali.getMessageNum();
            int nSiz = tiscali.getMessageSize();
            log.info( "getMessageNum  :" +nNum );
            log.info( "getMessageSize :" +nSiz );
            for( int nPos=1; nPos<=nNum; nPos++ ){
                log.info( "getMessageID   (" +nPos +"):" +tiscali.getMessageID(nPos) );
                log.info( "getMessageSize (" +nPos +"):" +tiscali.getMessageSize(nPos) );
                log.info( "getMessage     (" +nPos +"):" +tiscali.getMessage(nPos) );
            }
             */
            log.info(tiscali.getContactXML());
        }
    }

}
