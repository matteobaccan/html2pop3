/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
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

import it.baccan.html2pop3.utils.message.POP3Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import kong.unirest.Header;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.unbescape.html.HtmlEscape;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginTin extends POP3Base implements POP3Plugin {

    // Server di riferimento
    @Getter @Setter private String server = "";

    @Getter @Setter private String folder = "INBOX";

    // Property per variabili hidden
    private final Map<String, String> prop;

    private boolean bDebug = false;

    private UnirestInstance unirest = null;

    /**
     *
     */
    public PluginTin() {
        prop = new HashMap<>();
        unirest = Unirest.spawnInstance();
    }

    /**
     *
     * @param cUserParam
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUserParam, String cPwd) {
        bDebug = getDebug();

        boolean bRet = false;
        boolean bErr = false;

        String cFolder = "INBOX";
        int nQuestionMark = cUserParam.indexOf("?");
        if (nQuestionMark != -1) {
            cFolder = getPar(cUserParam.substring(nQuestionMark), "folder", "INBOX");
            cUserParam = cUserParam.substring(0, nQuestionMark);
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
                log.info("tin: login init ({}|{})", cDomain, nRetry);
                log.trace("tin: login user ({}|{})", cUser, cPwd);

                HttpResponse<String> stringResponse = unirest.post("https://aaacsc.alice.it/piattaformaAAA/aapm/amI")
                        .field("usernameDisplay", cUser)
                        .field("dominio", "@" + cDomain)
                        .field("password", cPwd)
                        .field("imageField", "")
                        .field("twoweeks", "false")
                        .field("login", cUser + "@" + cDomain)
                        .field("pwd", cPwd)
                        .field("channel", "mail_alice")
                        .field("URL_OK", "https://authsrs.alice.it/aap/aap_redir.jsp?entry=mail_alice")
                        .field("URL_KO", "https://mail.alice.it/boxlogin/errore.html")
                        .field("servizio", "mail")
                        .field("msisdn", cUser)
                        .field("username", cUser + "@" + cDomain)
                        .field("user", cUser + "@" + cDomain)
                        .field("a3afep", "https://mail.tim.it/boxlogin/errore.html")
                        .field("DOMAIN", cDomain)
                        .field("PASS", cPwd)
                        .field("self", "true")
                        .field("a3si", "none")
                        .field("a3st", "VCOMM")
                        .field("totop", "true")
                        .field("nototopa3ep", "true")
                        .field("a3aid", "lames")
                        .field("a3flag", "0")
                        .field("a3ep", "https://webmail.pc.tim.it/cp/ps/Main/login/SSOLogin")
                        .field("a3se", "https://mail.tim.it/boxlogin/errore.html")
                        .field("a3dcep", "http://communicator.alice.it/asp/homepage.asp?s=005")
                        .field("a3l", cUser + "@" + cDomain)
                        .field("a3p", cPwd)
                        .asString();

                // Post
                String sb = stringResponse.getBody();

                // Vado al sito
                AtomicBoolean foundSession = new AtomicBoolean(false);
                stringResponse.getHeaders().get("Set-Cookie").forEach(cookie -> {
                    if (cookie.contains("PAAA_AUTHE")) {
                        if (cookie.length() > 15) {
                            foundSession.set(true);
                        }
                    }
                });

                // Ora prendo il cookie
                if (!foundSession.get()) {
                    setLastErr("Errore di autenticazione");
                    log.error("tin: PAAA_AUTHE empty");
                    log.error("tin: " + sb);
                    log.error("tin: login end");
                    return false;
                }

                // Loop di rimbalzo
                while (true) {
                    int href1 = sb.indexOf(" href=\"");
                    int href2 = sb.indexOf("\"", href1 + 7);
                    if (href1 == -1 || href2 == -1) {
                        break;
                    }
                    String anchor = sb.substring(href1 + 7, href2);
                    String escaped = HtmlEscape.unescapeHtml(anchor);
                    log.info("tin: forward: [{}]", escaped);
                    HttpResponse<String> response = unirest.get(escaped).asString();
                    sb = response.getBody();
                }

                if (cDomain.equalsIgnoreCase("tim.it")) {
                    int href1 = sb.indexOf(" src=\"");
                    int href2 = sb.indexOf("\"", href1 + 6);
                    if (href1 == -1 || href2 == -1) {
                        log.error("SRC not found [{}]", sb);
                    }
                    String anchor = sb.substring(href1 + 6, href2);
                    setServer(anchor.substring(0, anchor.indexOf("/", 10)));
                    log.info("tin: LocalSSOLogin:" + anchor);
                    log.info("tin: server:" + getServer());
                    HttpResponse<String> response = unirest.get(anchor).asString();
                    sb = response.getBody();

                    String search = " src='";
                    href1 = sb.indexOf(search);
                    href2 = sb.indexOf("'", href1 + search.length());
                    if (href1 == -1 || href2 == -1) {
                        log.error("SRC not found [{}]", sb);
                    }
                    anchor = getServer() + sb.substring(href1 + search.length(), href2);
                    log.info("tin: PreLogin: [{}]", anchor);
                    response = unirest.get(anchor).asString();
                    sb = response.getBody();

                    href1 = sb.indexOf("&t=");
                    href2 = sb.indexOf("&", href1 + 3);
                    String t = sb.substring(href1 + 3, href2);
                    prop.put("t", t);

                } else {
                    int href1 = sb.indexOf(" src=\"");
                    int href2 = sb.indexOf("\"", href1 + 6);
                    if (href1 == -1 || href2 == -1) {
                        log.error("SRC not found [{}]", sb);
                    }
                    String anchor = sb.substring(href1 + 6, href2);
                    log.info("tin: LocalSSOLogin: [{}]", anchor);
                    HttpResponse<String> response = unirest.get(anchor).asString();
                    sb = response.getBody();
                    String search = "top.location=\"";
                    href1 = sb.indexOf(search);
                    href2 = sb.indexOf("\"", href1 + search.length());
                    if (href1 == -1 || href2 == -1) {
                        log.error("tin.it: SSOLogin error. [{}]", sb);
                    }
                    anchor = sb.substring(href1 + search.length(), href2).replace("http://", "https://");
                    log.info("tin: SSOLogin: [{}]", anchor);
                    response = unirest.get(anchor).asString();
                    sb = response.getBody();

                    setServer(anchor.substring(0, anchor.indexOf("/", 10)));
                    log.info("tin: server: [{}]", getServer());
                    href1 = sb.indexOf(" src='PreLogin");
                    href2 = sb.indexOf("'", href1 + 6);
                    anchor = getServer() + "/cp/ps/Main/login/" + sb.substring(href1 + 6, href2);
                    log.info("tin: PRELogin: [{}]", anchor);
                    response = unirest.get(anchor).asString();
                    sb = response.getBody();

                    href1 = sb.indexOf("&t=");
                    href2 = sb.indexOf("&", href1 + 3);
                    String t = sb.substring(href1 + 3, href2);
                    prop.put("t", t);

                }

                String cUrl2Go = getServer() + "/cp/ps/mail/SLcommands/SLEmailList?d=" + cDomain + "&u=" + cUser + "&t=" + prop.get("t") + "&l=it";

                log.info("tin: MailFrame GET [{}]", cUrl2Go);

                int nStart = 0;
                int nPage = 50;
                while (true) {
                    HttpResponse<String> response = unirest.post(cUrl2Go)
                            .field("fp", getFolder())
                            .field("limit", "" + nPage)
                            .field("start", "" + nStart)
                            .asString();
                    sb = response.getBody();

                    log.trace("tin: SLEmailList: [{}]", sb);
                    JSONObject json = new JSONObject(sb);
                    JSONArray el = (JSONArray) json.get("emails");
                    int nEmail = 0;
                    for (int n = 0; n < el.length(); n++) {
                        JSONObject email = (JSONObject) el.get(n);
                        //String uid = (String)email.get("uid");
                        Integer size = (Integer) email.get("size");
                        if (addEmailInfo(email.toString(), size)) {
                            nEmail++;
                        }
                    }
                    if (nEmail == 0) {
                        break;
                    }
                    nStart += nPage;
                }
                bRet = true;

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
                } catch (InterruptedException ex) {
                    log.error("Pause error", ex);
                }
            }
        }

        log.info("tin: login end");

        return bRet;
    }

    private String cleanJSON(String cJSON) {
        return HtmlEscape.unescapeHtml(cJSON);
    }

    /**
     *
     * @param nPos
     * @return
     */
    @Override
    public String getMessageID(int nPos) {
        String cEmailJSON = super.getMessageID(nPos);
        JSONObject jEmail = new JSONObject(cEmailJSON);
        return jEmail.getString("uid");
    }

    /**
     *
     * @param nPos
     * @return
     */
    public String getMessageIDFull(int nPos) {
        return super.getMessageID(nPos);
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
        StringBuffer oMail = null;
        try {
            log.info("tin: getmail init");

            String cEmailJSON = getMessageIDFull(nPos);

            JSONObject jEmail = new JSONObject(cEmailJSON);

            String cMsgId = jEmail.getString("uid");

            if (bDebug) {
                log.debug("tin: email:" + jEmail);
            }
            log.info("tin: getmail ID [{}]", cMsgId);

            String sb = "";
            boolean bTop = !(bAll || nLine > 0);

            if (!bTop) {
                HttpResponse<String> response = unirest.post(getServer() + "/cp/ps/mail/SLcommands/SLEmailBody?l=it")
                        .field("bid", "")
                        .field("d", prop.get("domain"))
                        .field("folderpath", getFolder())
                        .field("t", prop.get("t"))
                        .field("u", prop.get("userid"))
                        .field("uid", cMsgId)
                        .asString();
                sb = response.getBody();
                
                List<Header> headers = response.getHeaders().all();
                headers.forEach(action -> {
                    log.debug("[{}]=[{}]", action.getName(), action.getValue());
                });
                if (bDebug) {
                    log.debug(sb);
                }
            }

            if (bTop || (!bTop && getMessage(sb).length() > 0)) {
                POP3Message pop3 = new POP3Message();
                pop3.setDa("\"" + cleanJSON(jEmail.getString("fromnameoraddress")) + "\" <" + cleanJSON(jEmail.getString("from")) + ">");
                pop3.setData(formatDate(jEmail.getString("date")));
                pop3.setA(cleanJSON(jEmail.getString("to")));
                pop3.setOggetto(cleanJSON(jEmail.getString("subject")));
                if (!bTop) {
                    pop3.setBody(getMessage(sb));
                }

                // TOP optimization
                if (!bTop) {
                    HttpResponse<String> response = unirest.post(getServer() + "/cp/ps/mail/SLcommands/SLEmailHeaders?l=it&d=" + prop.get("domain") + "&t=" + prop.get("t") + "&u=" + prop.get("userid"))
                            .field("folderpath", getFolder())
                            .field("uid", cMsgId)
                            .asString();
                    String sbHead = response.getBody();

                    JSONObject jHead = new JSONObject(sbHead);
                    JSONArray ja = (JSONArray) jHead.get("emailheaders");

                    if (ja != null) {
                        if (ja.length() > 0) {
                            JSONObject row = (JSONObject) ja.get(0);
                            pop3.setCc(cleanJSON(row.getString("cc")));
                            // Uso gli header per migliorare l'email ritonata
                            String headerClean = cleanJSON(row.getString("headers"));
                            StringTokenizer st = new StringTokenizer(headerClean, "\r\n");
                            while (st.hasMoreTokens()) {
                                String cTok = st.nextToken();
                                if( cTok.startsWith("Date:") ){
                                    String date = cTok.substring(5).trim();                                    
                                    pop3.setData(date);
                                }
                            }
                        }
                    }

                    if (bDebug) {
                        log.info(jHead.toString());
                    }

                    if (ja != null) {
                        if (ja.length() > 0) {
                            JSONObject row = (JSONObject) ja.get(0);
                            //String yy = getServer() +row.getString( "urlZipDownloader" );
                            //byte[] xx = getPage( yy, prop.get("cookie") );
                            //xx=xx; String y = new String(xx);
                            JSONArray jatt = (JSONArray) row.get("attachments");
                            if (jatt != null) {
                                int n = 0;
                                while (n < jatt.length()) {
                                    JSONObject jAttach = (JSONObject) jatt.get(n);
                                    String urlDownloader = getServer() + jAttach.getString("urlDownloader") + "&disposition=attachment";

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    unirest.post(urlDownloader)
                                            .field("u", prop.get("userid"))
                                            .field("d", prop.get("domain"))
                                            .field("t", prop.get("t"))
                                            .thenConsume(consumer -> {
                                                try {
                                                    baos.write(consumer.getContentAsBytes());
                                                } catch (IOException ex) {
                                                    log.error("Error reading attch", ex);
                                                }
                                            });

                                    byte[] cFile = baos.toByteArray();
                                    pop3.addAttach(cleanJSON(jAttach.getString("name")), cFile);
                                    n++;
                                }
                            }
                        }
                    }

                }
                oMail = new StringBuffer();
                oMail.append(pop3.getMessage(nLine, bAll));
            } else {
                log.error("tin: errore di lettura email");
                log.error("tin: BODY:" + sb);
            }

            log.info("tin: getmail end");
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
    @Override
    public boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.info("tin: delmessage init");

            String cEmailJSON = getMessageIDFull(nPos);

            JSONObject jEmail = new JSONObject(cEmailJSON);

            String cMsgId = jEmail.getString("pid");

            if (bDebug) {
                log.debug("tin: email:" + cEmailJSON);
            }
            log.info("tin: delmessage ID " + cMsgId);

            HttpResponse<String> response = unirest.post(getServer() + "/cp/ps/mail/SLcommands/SLDeleteMessage?l=it")
                    .field("d", prop.get("domain"))
                    .field("t", prop.get("t"))
                    .field("u", prop.get("userid"))
                    .field("selection", cMsgId)
                    .asString();
            String sb = response.getBody();

            JSONObject jDelete = new JSONObject(sb);
            bRet = jDelete.getBoolean("success");

            log.info("tin: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    private static boolean bDelete = true;

    /**
     *
     * @param b
     */
    public static void setDelete(boolean b) {
        bDelete = b;
    }

    /**
     *
     * @return
     */
    public static boolean getDelete() {
        return bDelete;
    }

    /**
     *
     */
    @Override
    public void delMessageEnd() {

        try {
            log.info("tin: delmessageEnd init");

            // 15:35:22 martedi' 26 giugno 2012
            // per ora non faccio la pulizia del cestino, tanto e' dopo 7 giorni
            //if( bDelete ) getPage( cServer +"/cp/ps/Mail/EmptyTrash?fp=Cestino&d=" +prop.get("domain") +"&an=" +prop.get("an") +"&u=" +prop.get("userid") +"&t=" +prop.get("t") +"&IAmInEmailList=true&style=&l=it&s=" +prop.get("s") );
            log.info("tin: delmessageEnd end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
    }

    private String getMessage(String cMail) {
        String cRet = "";
        //01234567890123 456789012345678901234 567890123456789
        String cMessageIni = "<body "; //onload=\"calcHeight();\" onresize=\"calcHeight();return false;\">";
        int nInfo = cMail.indexOf(cMessageIni);
        if (nInfo != -1) {
            nInfo = cMail.indexOf(">", nInfo);
        }
        int nInfo2 = cMail.indexOf("<!-- DO NOT REMOVE THIS USED TO CALC LENGHT OF PAGE -->", nInfo);
        if (nInfo != -1 && nInfo2 > nInfo) {
            cRet = cMail.substring(nInfo + 1, nInfo2);
        }
        int nInfo3 = cMail.indexOf("<script language=\"JavaScript\">", nInfo);
        if (nInfo3 != -1 && nInfo3 > nInfo && nInfo3 < nInfo2) {
            cRet = cMail.substring(nInfo + 1, nInfo3);
        }

        return cRet.trim();
    }

    //TODO:
    //From: xxxxxxxxxx <xxxxxxx.xxxxxx@gmail.com>
    private String formatDate(String cDate) {
        String cRet = getCurDate();

        // 26-giu-2012 9.31
        int nYear = 0;
        int nMonth = 0;
        int nDay = 0;
        int nH = 0;
        int nM = 0;
        int nS = 0;

        int nTok = 0;
        StringTokenizer st = new StringTokenizer(cDate, " /:.-");
        while (st.hasMoreTokens()) {
            String cTok = st.nextToken();
            nTok++;
            if (nTok == 1) {
                nDay = Integer.parseInt(cTok);
            } else if (nTok == 2) {
                nMonth = month2numIta(cTok);
            } else if (nTok == 3) {
                nYear = Integer.parseInt(cTok) - 1900;
            } else if (nTok == 4) {
                nH = Integer.parseInt(cTok);
            } else if (nTok == 5) {
                nM = Integer.parseInt(cTok);
            }
        }
        if (nTok > 4) {
            cRet = formatDate(nYear, nMonth, nDay, nH, nM, nS);
        }

        return cRet;
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
