/*
 * Plugin per www.tin.it derivato da email.java
 *
 * Copyright 2004 Matteo Baccan  <matteo@baccan.it>
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
 * Title: Tin HTML2POP3 Description: Convertitore da HTML a POP3 per www.tin.it
 * Copyright: Copyright (c) 2003 Company:
 *
 * @author Giulio Pollini, Matteo Baccan
 * @version 2.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginVirgilio extends PluginTin implements POP3Plugin {

    private boolean bDebug = false;

    // Property per variabili hidden
    private final Map<String, String> prop;

    /**
     * Virgilio Plugin Costructor.
     */
    public PluginVirgilio() {
        prop = new HashMap<>();
    }

    /**
     *
     * @param cUserParam
     * @param cPwd
     * @return
     */
    public boolean login(String cUserParam, String cPwd) {
        bDebug = getDebug();

        boolean bRet = false;
        boolean bErr = false;

        String cFolder = "";
        int nQuestionMark = cUserParam.indexOf("?");
        if (nQuestionMark != -1) {
            cFolder = URLEncoder.encode(getPar(cUserParam.substring(nQuestionMark), "folder", "INBOX"));
            cUserParam = cUserParam.substring(0, nQuestionMark);
        } else {
            cFolder = "INBOX";
        }
        setFolder(cFolder);

        String cUser = cUserParam;

        // Pulizia informazioni di login
        String cDomain = cUser.substring(cUser.indexOf("@") + 1).toLowerCase();
        cUser = cUser.substring(0, cUser.indexOf("@")).toLowerCase();

        prop.put("userid", cUser);
        prop.put("password", cPwd);
        prop.put("domain", cDomain);

        for (int nRetry = 0; nRetry < 3; nRetry++) {
            bErr = false;

            // Login
            try {
                log.error("virgilio: login init (" + cDomain + "|" + nRetry + ")");

                // Mi faccio riconoscere dal sistema
                // Preparo I parametri
                String cPostNew = "usernameDisplay=" + cUser + "&"
                        + "password=" + cPwd + "&"
                        + "dominio=%40" + cDomain + "&"
                        //+ "imageField.x=35&"
                        //+ "imageField.y=15&"
                        + "login=" + cUser + "%40" + cDomain + "&"
                        + "pwd=" + cPwd + "&"
                        + "channel=Vmail&"
                        + "URL_OK=https%3A%2F%2Fauthsrs.alice.it%2Faap%2Faap_redir.jsp%3Fentry%3DVmail&"
                        + "URL_KO=https%3A%2F%2Fauthsrs.alice.it%2Faap%2Faap_redir_ko.jsp%3Fentry%3DVmail&"
                        + "servizio=mail&"
                        + "msisdn=" + cUser + "&"
                        + "username=" + cUser + "%40" + cDomain + "&"
                        + "user=" + cUser + "%40" + cDomain + "&"
                        + "a3afep=http%3A%2F%2Fmail.virgilio.it%2Fcommon%2Fiframe_mail%2Fpf%2FVlogin.html%3Fcode%3D470%26channel%3DVmail&"
                        + "DOMAIN=&"
                        + "PASS=" + cPwd + "&"
                        + "self=true&"
                        + "a3si=none&"
                        + "a3st=VCOMM&"
                        + "totop=true&"
                        + "nototopa3ep=true&"
                        + "a3aid=LVHPTOP&"
                        + "a3flag=0&"
                        + "a3ep=http%3A%2F%2Fwebmail.virgilio.it%2Fcp%2Fps%2FMain%2Flogin%2FSSOLogin&"
                        + "a3epvf=http%3A%2F%2Fwebmail.virgilio.it%2Fcp%2Fps%2FMain%2Flogin%2FSSOLogin&"
                        + "a3se=http%3A%2F%2Fmail.virgilio.it%2Fcommon%2Fiframe_mail%2Fpf%2FVlogin.html%3Fcode%3D470%26channel%3DVmail&"
                        + "a3dcep=http%3A%2F%2Fcommunicator.alice.it%2Fasp%2Fhomepage.asp%3Fs%3D005&"
                        + "a3l=" + cUser + "%40" + cDomain + "&"
                        + "a3p=" + cPwd + "&";

                // Post
                String cUrl2Go = "https://aaacsc.virgilio.it/piattaformaAAA/aapm/amI";

                // Vado al sito
                log.error("virgilio: POST:" + cUrl2Go);
                String sb = postPage(cUrl2Go, "", cPostNew).toString();

                // Ora prendo il cookie
                String cCook = getCookie();
                if (cCook.indexOf("PAAA_AUTHE=_;") != -1) {
                    setLastErr("Errore di autenticazione");
                    log.error("virgilio: PAAA_AUTHE empty");
                    log.error("virgilio: " + cCook);
                    log.error("virgilio: " + sb);
                    log.error("virgilio: login end");
                    return false;
                }

                int nUrl1 = 0;
                int nUrl2 = 0;

                sb = getPage("http://webmail.virgilio.it/cp/default.jsp", cCook).toString();

                sb = sb;

                nUrl1 = sb.indexOf("<iframe name=\"main\" id=\"main\" src=\"");
                nUrl2 = sb.indexOf("\" width=", nUrl1 + 1);
                String inboxUrl = sb.substring(nUrl1 + 35, nUrl2);
                nUrl1 = inboxUrl.indexOf("/", 10);
                setServer(inboxUrl.substring(0, nUrl1));
                prop.put("t", getPar("t", inboxUrl));
                // u, d, t

                /*
                cUrl2Go = "http://mailweb.virgilio.it/cp/ps/Main/login/SSOLogin";

                log.error("virgilio: SSO GET:" + cUrl2Go);
                if (bDebug) {
                    log.error("virgilio: GET:" + cCook);
                }
                sb = getPage(cUrl2Go, cCook).toString();  //getLocation();

                cCook += getCookie();
                if (bDebug) {
                    log.error("virgilio: login4C:" + cCook);
                }

                setServer("http://mailweb.virgilio.it");
                nUrl1 = sb.indexOf("'PreLogin?u=");
                nUrl2 = sb.indexOf("'", nUrl1 + 1);
                cUrl2Go = getServer() + "/cp/ps/Main/login/" + sb.substring(nUrl1 + 1, nUrl2);
                log.error("virgilio: MailFrame GET:" + cUrl2Go);
                if (nUrl1 == -1) {
                    log.error("virgilio: ERR: " + sb);
                }
                if (bDebug) {
                    log.error("virgilio: GET:" + cCook);
                }
                sb = getPage(cUrl2Go, cCook).toString();

                cCook += getCookie();
                nUrl1 = sb.indexOf("\"POSTLogin?");
                nUrl2 = sb.indexOf("\"", nUrl1 + 1);
                cUrl2Go = getServer() + "/cp/ps/Main/login/" + sb.substring(nUrl1 + 1, nUrl2);
                log.error("virgilio: MailFrame GET:" + cUrl2Go);
                if (nUrl1 == -1) {
                    log.error("virgilio: ERR: " + sb);
                }
                if (bDebug) {
                    log.error("virgilio: GET:" + cCook);
                }
                sb = postPage(cUrl2Go, cCook, "l=it&d=" + cDomain + "&u=" + cUser).toString();

                if (bDebug) {
                    log.error("virgilio: page:" + sb);
                }
                 */
                //int nINBOX = sb.indexOf("'/cp/ps/mail/SLcommands/SLEmailList?d=");
                //int nINBOX2 = sb.indexOf("'", nINBOX + 1);
                //if (nINBOX != -1 && nINBOX2 != -1 && nINBOX + 1 < nINBOX2) {
                //    String cPage = sb.substring(nINBOX + 1, nINBOX2);
                //    prop.put("t", getPar(cPage, "t"));
                prop.put("cookie", cCook);

                int nStart = 0;
                int nPage = 50;
                while (true) {
                    sb = postPage(getServer() + "/cp/ps/mail/SLcommands/SLEmailList?d=", cCook, "fp=" + getFolder() + "&limit=" + nPage + "&start=" + nStart).toString().trim();
                    if (bDebug) {
                        log.error("tin: email:" + sb);
                    }
                    JSONObject json = new JSONObject(sb);
                    JSONArray el = (JSONArray) json.get("emails");
                    int nEmail = 0;
                    for (int n = 0; n < el.length(); n++) {
                        JSONObject email = (JSONObject) el.get(n);
                        Integer size = (Integer) email.get("size");
                        if (addEmailInfo(email.toString(), size.intValue())) {
                            nEmail++;
                        }
                    }
                    if (nEmail == 0) {
                        break;
                    }
                    nStart += nPage;
                }
                bRet = true;
                //}

            } catch (Throwable ex) {
                bErr = true;
                log.error("Error", ex);
            }

            // Se non ho errori esco, altrimenti faccio 3 tentativi
            if (!bErr) {
                break;
            } else {
                // Pausa in caso di errore di login e sendo tentativo
                try {
                    if (nRetry < 2) {
                        Thread.sleep(3000);
                    }
                } catch (Throwable ex) {
                }
            }
        }

        log.error("virgilio: login end");

        return bRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginTin tin = new PluginTin();
        if (tin.login(args[0], args[1])) {
            int nNum = tin.getMessageNum();
            int nSiz = tin.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + tin.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + tin.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + tin.getMessage(nPos));
                break;
            }
        }
    }

}
