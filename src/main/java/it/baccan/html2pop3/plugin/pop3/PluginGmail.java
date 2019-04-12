/*
 * Gmail.com plugin
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
 * Title:        Hotmail HTML2POP3
 * Description:  Convertitore da HTML a POP3 per Gmail.com
 * Copyright:    Copyright (c) 2006
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.util.*;
import java.net.*;

import it.baccan.html2pop3.utils.*;
import it.baccan.html2pop3.plugin.smtp.*;
import jregex.Matcher;
import jregex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginGmail extends POP3Base implements POP3Plugin, SMTPPlugin {

    private boolean bDebug = false;

    private String cGmailCookie = "";
    private String cGlobal8 = "";
    private String cFolder = "inbox";
    private String cServer = "http://mail.google.com/mail";
    private boolean bLogin = false;

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        bDebug = getDebug();
        bLogin = false;

        try {
            log.error("Gmail: login init");

            String postReturn = null;
            String postData = null;

            String cCookie = "";
            postReturn = getPage("https://accounts.google.com/ServiceLogin?service=mail").toString();
            cCookie = getCookie();
            String cGALX = getCook(cCookie, "GALX");
            if (cGALX.length() == 0) {
                setLastErr("Errore di autenticazione - manca GALX");
                log.error("Gmail: autenticazione - GALX empty");
                log.error("Gmail: autenticazione - " + getCookie());
                log.error("Gmail: autenticazione - " + postReturn);
                log.error("Gmail: autenticazione - login end");
                return false;
            }
            if (bDebug) {
                log.error("GALX: " + cGALX);
            }
            //if( bDebug ) log.error( "P: "+postReturn );
            if (bDebug) {
                log.error("L: " + getLocation());
            }
            if (bDebug) {
                log.error("C: " + cCookie);
            }
            log.info("Gmail: ServiceLogin");

            postData = "continue=https%3A%2F%2Fmail.google.com%2Fmail%3Fui%3Dhtml%26zy%3Dl"
                    + "&service=mail&signIn=Sign%20in&rmShown=1&rm=false&ltmplcache=2&ltmpl=default&scc=1&ss=1&PersistentCookie=yes"
                    + "&GALX=" + cGALX
                    + "&Email=" + URLEncoder.encode(cUser)
                    + "&Passwd=" + URLEncoder.encode(cPwd);
            String cLoc = "https://accounts.google.com/ServiceLoginAuth";
            postReturn = postPage(cLoc, cCookie, postData).toString();
            cCookie += getCookie();
            if (postReturn.indexOf("Username and password do not match") != -1) {
                setLastErr("Errore di autenticazione - Login o password sbagliata");
                log.error("Gmail: autenticazione - Login o password sbagliata");
                log.error("Gmail: autenticazione - " + getCookie());
                log.error("Gmail: autenticazione - " + postReturn);
                log.error("Gmail: autenticazione - login end");
                return false;
            }
            if (bDebug) {
                log.error("P1: " + postReturn);
            }
            if (bDebug) {
                log.error("L1: " + getLocation());
            }
            if (bDebug) {
                log.error("C1: " + cCookie);
            }
            log.info("Gmail: ServiceLoginAuth");

            String cRef = "";
            cRef = cLoc;
            cLoc = "";
            while ((cLoc = getLocation()).length() > 0) {
                postReturn = getPage(cLoc, cCookie, 0, true, cRef).toString();
                cCookie += getCookie();
                if (bDebug) {
                    log.error("P2: " + postReturn);
                }
                if (bDebug) {
                    log.error("C2: " + cCookie);
                }
                if (bDebug) {
                    log.error("L2: " + getLocation());
                }
                log.info("Gmail: ServiceLoginAuth forward");
                cLoc = getLocation();
                cRef = cLoc;
            }

            /*
            cRef = cLoc;
            int nRef1 = postReturn.indexOf("&#39;");
            int nRef2 = postReturn.indexOf("&#39;", nRef1+1);
            cLoc = postReturn.substring( nRef1+5, nRef2 );
            cLoc = string.replace( cLoc, "&amp;", "&" );
            postReturn = getPage( cLoc, cCookie, 0, true, cRef ).toString();
            cCookie += getCookie();
            log.info( "Gmail: ServiceLoginAuth html redirect");

            cRef = cLoc;
            while( (cLoc=getLocation()).length()>0 ){
               postReturn = getPage( cLoc, cCookie, 0, true, cRef ).toString();
               cCookie += getCookie();
               if( bDebug ) log.error( "P3: "+postReturn );
               if( bDebug ) log.error( "C3: "+cCookie );
               if( bDebug ) log.error( "L3: "+getLocation() );
               log.info( "Gmail: ServiceLoginAuth forward");
               cLoc = getLocation();
               cRef = cLoc;
            }
             */
            String cGM = getCook(cCookie, "GMAIL_AT");
            String cGX = getCook(cCookie, "GX");
            String cSID = getCook(cCookie, "SID");
            if (cSID.length() > 0) {
                cCookie = "GMAIL_AT=" + cGM + ";"
                        + " GX=" + cGX + ";"
                        + " SID=" + cSID + ";";

                if (bDebug) {
                    log.error("C: " + cCookie);
                }

                cGmailCookie = cCookie;

                if (bDebug) {
                    log.error("Master Cookie: " + cGmailCookie);
                }

                bLogin = true;

            } else {
                log.error("GMail: wrong login");
            }

            log.error("Gmail: login end");
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
                log.error("GMail: list init");
                StringBuffer oMail = new StringBuffer();
                oMail.append(getPage(cServer + "/?search=" + cFolder + "&view=tl&start=0&init=1&zx=0", cGmailCookie));
                //if( bDebug ) log.error( "GMAIL_AT: " +getCook(getCookie(),"GMAIL_AT") );
                //if( cGmailCookie.indexOf("GMAIL_AT")==-1 ){
                //String cGMAIL_AT = getCook(getCookie(),"GMAIL_AT");
                //cGmailCookie += " GMAIL_AT=" +cGMAIL_AT +";";
                //}
                //if( bDebug ) log.error( "Master Cookie: " +cGmailCookie );
                if (bDebug) {
                    log.error(getLocation());
                }

                // DEBUG
                if (bDebug) {
                    log.error(oMail.toString());
                }

                Pattern p = new Pattern(",\\[\"(\\w*)\",\"(\\w*)\",\"(\\w*)\",(\\d*),(\\d*),\\[\"\\^(\\w*)\",\"\\^(\\w*)\"");
                Matcher m = p.matcher(oMail.toString());
                while (m.find()) {
                    // DEBUG
                    if (bDebug) {
                        log.error(m.group(1));
                    }
                    if (bDebug) {
                        log.error(m.group(2)); //
                    }
                    if (bDebug) {
                        log.error(m.group(3)); //
                    }
                    if (bDebug) {
                        log.error(m.group(4));
                    }
                    if (bDebug) {
                        log.error(m.group(5));
                    }
                    if (bDebug) {
                        log.error(m.group(6));
                    }

                    // Prendo I valori che mi servono
                    String cId = m.group(1);
                    int nLen = 1;
                    addEmailInfo(cId, nLen);
                }

                p = new Pattern("var GLOBALS=\\[,,\"(\\d*)\",\"([\\w|\\.]*)\",\"(\\w|\\.)*\",\"(\\d)*\",\"([\\w|\\.|!]*)\",\"([\\w|/]*)\\\",(\\d*),\"(\\w*)\",");
                m = p.matcher(oMail.toString());
                if (m.find()) {
                    // DEBUG
                    if (bDebug) {
                        log.error(m.group(1));
                    }
                    if (bDebug) {
                        log.error(m.group(2)); //
                    }
                    if (bDebug) {
                        log.error(m.group(3)); //
                    }
                    if (bDebug) {
                        log.error(m.group(4));
                    }
                    if (bDebug) {
                        log.error(m.group(5));
                    }
                    if (bDebug) {
                        log.error(m.group(6));
                    }
                    if (bDebug) {
                        log.error(m.group(7));
                    }
                    if (bDebug) {
                        log.error(m.group(8));
                    }

                    // Prendo il valore che mi serve
                    //cGlobal3 = m.group(3);
                    cGlobal8 = m.group(8);
                }
                log.error("GMail: list end");
                bRet = true;
            }
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     * @return
     */
    @Override
    public ArrayList<String[]> getContact() {
        ArrayList<String[]> oRet = new ArrayList<>();
        try {

            // Prendo i contatti
            String cBook = cServer + "/contacts/data/export?exportType=ALL&groupToExport=&out=OUTLOOK_CSV";

            StringBuffer oMail = new StringBuffer();
            oMail.append(getPage(cBook, cGmailCookie));

            //log.error( "Gmail: getContact " +cBook            );
            //log.error( "Gmail: getContact " +cGmailCookie     );
            //log.error( "Gmail: getContact " +oMail.toString() );
            int n = 0;
            Pattern p = new Pattern("([^,\r\n]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),(.*)");
            Matcher m = p.matcher(oMail.toString());
            while (m.find()) {
                String[] oEle = {m.group(6), m.group(14)};
                if (n > 0) {
                    oRet.add(oEle);
                }
                n++;
            }
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return oRet;
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
            log.error("Gmail: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Gmail: getmail ID (" + cMsgId + ")");

            String cLoc = cServer + "/u/0/?ui=2&ik=" + cGlobal8 + "&view=om&th=" + cMsgId;

            String postReturn = getPage(cLoc, cGmailCookie, nLine, bAll).toString();

            oMail.append(postReturn);

            log.error("Gmail: getmail end");
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
            log.error("Gmail: delmessage");

            String cMsgId = getMessageID(nPos);

            log.error("Gmail: delmessage " + cMsgId);

            // act = tr    -> mette in trash    -> parametro t=
            // act = rc_^i -> mette in archive  -> parametro t=
            // act = dm    -> delete message    -> parametro m=
            //String cDele = "http://mail.google.com/mail?search=" +cFolder +"&view=tl&start=0";
            //String cPost = "&t=" +cMsgId +"&act=rc_^i&at="+cGMAIL_AT;
            //String cDele = cServer +"/?search=" +cFolder +"&qt=&view=up&act=dm&at=" +getCook(cGmailCookie,"GMAIL_AT") +"&m=" +cMsgId;
            //log.error( "Gmail: delmessage " +cDele );
            //String cPage = getPage( cDele, cGmailCookie ).toString();
            String cDele = cServer + "/u/0/?ui=2&ik=" + cGlobal8 + "&rid=mail%3A%23.72c.125.0&at=" + getCook(cGmailCookie, "GMAIL_AT") + "&view=up&act=dm&_reqid=40837562&pcd=1&mb=0&rt=j&search=" + cFolder;
            String cPage = postPage(cDele, cGmailCookie, "m=" + cMsgId).toString();

            bRet = (cPage.indexOf("The message has been moved to the Trash") != -1);

            log.error("Gmail: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    // Send message
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
            log.error("Gmail: sendMessage");

            String cTo = "";
            for (int nPos = 0; nPos < aTo.size(); nPos++) {
                cTo += (nPos > 0 ? ";" : "") + ((String) aTo.elementAt(nPos)).trim();
            }

            //log.info( cMsg );
            // Separo l'header dal corpo
            String cHead = "";
            String cBody = "";
            int nBody = cMsg.indexOf("" + (char) 13 + (char) 10 + (char) 13 + (char) 10);
            if (nBody != -1) {
                cHead = cMsg.substring(0, nBody + 2);
                cBody = cMsg.substring(nBody + 4);
            }

            //log.error( "HEAD: (" +cHead +")" );
            //log.error( "BODY: (" +cBody +")" );
            // Estraggo i tag
            String cSub = getTag(cHead, "Subject:"); //"prova";
            //String cBod = cBody.replaceAll(""+(char)13,"").replaceAll(""+(char)10,"");
            String cBod = cBody.replace("" + (char) 13 + (char) 10, "<br>");
            //String cBod = cBody;

            //log.error( "SUB: (" +cSub +")" );
            //log.error( "BOD: (" +cBod +")" );
            // Questi li vediamo poi
            String cCC = "";
            String cBCC = "";
            //String cStd = "1/15/2007";
            //String cStt = "5:00pm";
            //String cEnd = "1/15/2007";
            //String cEnt = "6:00pm";

            String postData = "";
            postData += "view=sm";    // Send Message
            postData += "&draft=";    // empty = no draft
            postData += "&rm=";       // empty = new message ID
            postData += "&th=";       // empty = new thread ID
            postData += "&at=" + getCook(cGmailCookie, "GMAIL_AT");
            //postData += "&wid=376";
            //postData += "&jsid=pn7tmgrm8i4m";
            //postData += "&ov=tl";
            //postData += "&isevent=false";
            //postData += "&startdatetime=";
            //postData += "&enddatetime=";
            postData += "&from=" + cFrom;
            postData += "&to=" + cTo;
            postData += "&cc=" + cCC;       // Get from email
            postData += "&bcc=" + cBCC;     // Get from email
            postData += "&subject=" + cSub; // Get from email
            //postData += "&eventtitle=";
            //postData += "&where=";
            //postData += "&stdate=" +cStd;  // Get from email 1/15/2007
            //postData += "&sttime=" +cStt;  // Get from email 5:00pm
            //postData += "&enddate=" +cEnd; // Get from email 1/15/2007
            //postData += "&endtime=" +cEnt; // Get from email 6:00pm
            postData += "&ishtml=1";       // Allways
            //postData += "&cmid=1";         // Allways
            postData += "&msgbody=" + cBod; // Get from email - Ciao<br><br>

            //log.error( "RET: " +postData );
            // Indaga cmid=120 e ik=e4caa34b2b a cosa serve
            //String postReturn = postPage( "http://mail.google.com/mail/?ik=e4caa34b2b&cmid=160&newatt=0&rematt=0", cGmailCookie, postData, "", "", "multipart/form-data" ).toString();
            String cRef = cServer + "/?view=cv&search=" + cFolder + "&th=&zx=1";
            String postReturn = postPage(cServer + "/?search=inbox&qt=&cmid=&newatt=0&rematt=0", cGmailCookie, postData, cRef, "", "multipart/form-data").toString();

            //log.info( "Post:" +postReturn );
            //##da migliorare
            bRet = (postReturn.indexOf("\"sr\",\"\",1,\"") != -1);
            if (!bRet) {
                log.error("RET: " + postReturn);
            }

            log.error("Gmail: sendMessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    //
    private String getTag(String cHead, String cTag) {
        String cRet = "";
        int nPos = cHead.indexOf(cTag);
        if (nPos != -1) {
            int nPosEnd = cHead.indexOf("" + (char) 13 + (char) 10, nPos);
            if (nPosEnd != -1) {
                cRet = cHead.substring(nPos + cTag.length(), nPosEnd).trim();
            }
        }
        return cRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginGmail gmail = new PluginGmail();
        if (gmail.login(args[0], args[1])) {
            /*
            gmail.list();
            int nNum = gmail.getMessageNum();
            int nSiz = gmail.getMessageSize();
            log.info( "getMessageNum  :" +nNum );
            log.info( "getMessageSize :" +nSiz );
            for( int nPos=1; nPos<=nNum; nPos++ ){
                log.info( "getMessageID   (" +nPos +"):" +gmail.getMessageID(nPos) );
                log.info( "getMessageSize (" +nPos +"):" +gmail.getMessageSize(nPos) );
                log.info( "getMessage     (" +nPos +"):" +gmail.getMessage(nPos) );
            }
             */

            log.info(gmail.getContactXML());

            //Vector aTo = new Vector();
            //aTo.addElement( args[2] );
            //gmail.sendMessage( args[0], aTo, args[3] );
        }
    }

}
