/*
 * Copyright (c) 2021 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title: Email.it HTML2POP3 Description: Convertitore da HTML a POP3 per www.email.it
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.utils.LineFormat;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kong.unirest.HttpResponse;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginEmailIt extends POP3Base implements POP3Plugin {

    static final String EMAILIT_DOMAIN = "https://irin.email.it/";

    // Server di riferimento
    @Getter
    @Setter
    private String server = "";

    @Getter
    @Setter
    private String folder = "INBOX";

    static final String ZM_AUTH_TOKEN = "ZM_AUTH_TOKEN";
    AtomicReference<String> auth = new AtomicReference<>("");

    // Property per variabili hidden
    private final Map<String, String> prop;

    private static boolean delete = true;

    /**
     * TIN Plugin Costructor.
     */
    public PluginEmailIt() {
        super();
        prop = new HashMap<>();
    }

    /**
     *
     * @param cUserParam
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUserParam, String cPwd) {
        boolean bRet = false;
        boolean bErr;

        String cFolder = "INBOX";
        int nQuestionMark = cUserParam.indexOf("?");
        if (nQuestionMark != -1) {
            cFolder = getPar(cUserParam.substring(nQuestionMark), "folder", "INBOX");
            cUserParam = cUserParam.substring(0, nQuestionMark);
        }
        setFolder(cFolder);

        prop.put("userid", cUserParam);
        prop.put("password", cPwd);

        for (int nRetry = 0; nRetry < 3; nRetry++) {
            bErr = false;

            // Login
            try {
                log.trace("email.it: login user ({}|{})", cUserParam, cPwd);

                // Carico i cookie di dominio in Unirest
                HttpResponse<String> domainResponse = getUnirest().get(EMAILIT_DOMAIN).asString();

                // Prendo csrf
                AtomicReference<String> session = new AtomicReference<>("");
                domainResponse.getHeaders().get("Set-Cookie").forEach(cookie -> {
                    String cookieLabel = "ZM_LOGIN_CSRF";
                    if (cookie.startsWith(cookieLabel)) {
                        int endCokie = cookie.indexOf(";");
                        session.set(cookie.substring(cookieLabel.length() + 1, endCokie));
                    }
                });

                HttpResponse<String> stringResponse = getUnirest().post(EMAILIT_DOMAIN)
                        .field("loginOp", "login")
                        .field("login_csrf", session.get())
                        .field("username", cUserParam)
                        .field("password", cPwd)
                        .field("client", "standard")
                        //.header("Content-Type", "application/x-www-form-urlencoded")
                        .asString();

                // Prendo auth                
                stringResponse.getHeaders().get("Set-Cookie").forEach(cookie -> {
                    if (cookie.startsWith(ZM_AUTH_TOKEN)) {
                        int endCokie = cookie.indexOf(";");
                        auth.set(cookie.substring(ZM_AUTH_TOKEN.length() + 1, endCokie));
                    }
                });

                // Prendo la redirect
                // FIXME migliorare il controllo
                String homepageUrl = stringResponse.getHeaders().get("Location").get(0);

                HttpResponse<String> homepage = getUnirest().get(homepageUrl).asString();

                // Creo il DOM
                String sb = homepage.getBody();

                Document strongDoc = Jsoup.parse(sb);
                Elements trs = strongDoc.getElementsByClass("ZhRow");
                trs.forEach(tr -> {
                    Elements as = tr.getElementsByTag("a");
                    as.forEach(a -> {
                        String href = a.attr("href");
                        String token = "&id=";
                        if (href.contains(token)) {
                            String id = href.substring(href.indexOf(token) + token.length());
                            addEmailInfo(id, 1000);
                        }
                    });
                });

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

        log.info("email.it: login end");

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

            String cSessionCook = ZM_AUTH_TOKEN +"="+ auth.get();

            oMail = getPage("https://irin.email.it/service/home/~/?auth=co&view=text&id=" + cMsgId, cSessionCook, nLine, bAll);

            if (getContentType().indexOf("text/plain") == -1) {
                oMail = null;
            }

            log.error("Infinito: getmail end");

        } catch (FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : LineFormat.format(oMail.toString()));
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

            String cEmailJSON = getMessageID(nPos);

            JSONObject jEmail = new JSONObject(cEmailJSON);

            String cMsgId = jEmail.getString("pid");

            log.trace("tin: email:" + cEmailJSON);
            log.info("tin: delmessage ID " + cMsgId);

            HttpResponse<String> response = getUnirest().post(getServer() + "/cp/ps/mail/SLcommands/SLDeleteMessage?l=it")
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

}
