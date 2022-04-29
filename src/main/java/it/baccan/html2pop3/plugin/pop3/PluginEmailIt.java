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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginEmailIt extends POP3Base implements POP3Plugin {

    /**
     * Url di base di Email.it.
     */
    static final String EMAILIT_DOMAIN = "https://webmail.email.it/";

    /**
     * Server di riferimento
     */
    @Getter
    @Setter
    private String server = "";

    @Getter
    @Setter
    private String folder = "INBOX";

    private String crumb = "";
    private String sfi = "";

    static final String ZM_AUTH_TOKEN = "ZM_AUTH_TOKEN";
    private AtomicReference<String> auth = new AtomicReference<>("");

    // Property per variabili hidden
    private final Map<String, String> prop;

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
    public final boolean login(String cUserParam, String cPwd) {
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
                HttpResponse<String> domainResponse = getUnirest().get("https://www.email.it/mail.php")
                        .asString();

                String body = domainResponse.getBody();
                int szIni = body.indexOf("sz: '");
                int szEnd = body.indexOf("'", szIni + 5);
                String sz = body.substring(szIni + 5, szEnd);

                HttpResponse<JsonNode> stringResponse = getUnirest().post("https://www.email.it/mail_jxp_.php")
                        .field("rc", "")
                        .field("sz", sz)
                        .basicAuth(cUserParam, cPwd)
                        .asJson();

                String authPageUrl = stringResponse.getBody().getObject().get("url").toString();

                // Cookie ----------
                getUnirest().config().reset().followRedirects(false);
                HttpResponse<String> authPage = getUnirest().get(authPageUrl).asString();

                // Prendo auth
                authPage.getHeaders().get("Set-Cookie").forEach(cookie -> {
                    if (cookie.startsWith(ZM_AUTH_TOKEN)) {
                        int endCokie = cookie.indexOf(";");
                        auth.set(cookie.substring(ZM_AUTH_TOKEN.length() + 1, endCokie));
                    }
                });

                // Prendo csrf
                AtomicReference<String> session = new AtomicReference<>("");
                authPage.getHeaders().get("Set-Cookie").forEach(cookie -> {
                    String cookieLabel = "ZM_LOGIN_CSRF";
                    if (cookie.startsWith(cookieLabel)) {
                        int endCokie = cookie.indexOf(";");
                        session.set(cookie.substring(cookieLabel.length() + 1, endCokie));
                    }
                });
                getUnirest().config().reset().followRedirects(true);
                // Cookie ----------

                // Rifaccio la get seguendo i redirect
                authPage = getUnirest().get(authPageUrl).asString();

                // Passo alla home in html
                //HttpResponse<String> homepage = getUnirest().get("https://irin.email.it/?client=standard").asString();
                HttpResponse<String> homepage = getUnirest().get("https://webmail.email.it/h/search?mesg=welcome&init=true&auth=qp").asString();
                // Creo il DOM
                String sb = homepage.getBody();

                Document strongDoc = Jsoup.parse(sb);
                addEmails(strongDoc);

                crumb = strongDoc.getElementsByAttributeValue("name", "crumb").get(0).val();
                sfi = strongDoc.getElementsByAttributeValue("title", "Inbox").get(0).parent().attr("href");
                sfi = sfi.substring(sfi.indexOf("sfi=") + 4);

                /*
                <a id="NEXT_PAGE" href="/h/search?si=0&amp;so=10&amp;sc=38611&amp;sfi=4&amp;st=message">
                    <img src="/img/zimbra/ImgRightArrow.png" title="vai alla pagina successiva" alt="vai alla pagina successiva" border="0">
                </a>
                 */
                while (true) {
                    Element nextPage = strongDoc.getElementById("NEXT_PAGE");
                    if (nextPage == null) {
                        break;
                    }
                    String href = nextPage.attr("href");
                    if (!href.contains("&so")) {
                        break;
                    }

                    String so = href.substring(href.indexOf("&so="));
                    so = so.substring(0, so.indexOf("&", 1));

                    HttpResponse<String> page = getUnirest().get(EMAILIT_DOMAIN + "h/search?sfi=" + sfi + so).asString();
                    strongDoc = Jsoup.parse(page.getBody());
                    if (!addEmails(strongDoc)) {
                        break;
                    }
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
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
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
    @Override
    public String getMessage(int nPos, int nLine, boolean bAll) {
        StringBuilder oMail = new StringBuilder();
        try {
            log.debug("email.it: getmail init");

            String cMsgId = getMessageID(nPos);

            log.debug("email.it: getmail ID (" + cMsgId + ")");

            String cSessionCook = ZM_AUTH_TOKEN + "=" + auth.get();

            oMail.append(getPage(EMAILIT_DOMAIN + "service/home/~/?auth=co&view=text&id=" + cMsgId, cSessionCook, nLine, bAll));

            if (!getContentType().contains("text/plain")) {
                oMail = null;
            }

            log.debug("email.it: getmail end");

        } catch (FileNotFoundException fnf) {
            oMail = null;
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return oMail == null ? null : LineFormat.format(oMail.toString());
    }

    /**
     *
     * @param nPos
     * @return
     */
    @Override
    public final boolean delMessage(int nPos) {
        boolean bRet = false;
        try {
            log.info("email.it: delmessage init");

            String cMsgId = getMessageID(nPos);

            log.info("email.it: delmessage ID " + cMsgId);

            getUnirest().post(EMAILIT_DOMAIN + "h/search")
                    .queryString("si", "0")
                    .queryString("so", "0")
                    .queryString("sc", "38434")
                    .queryString("sfi", sfi)
                    .queryString("st", "message")
                    .field("actionDelete", "Elimina")
                    .field("dragTargetFolder", "")
                    .field("dragMsgId", "")
                    .field("folderId", "")
                    .field("actionOp", "")
                    .field("contextFolderId", "4")
                    .field("actionSort", "dateAsc")
                    .field("id", cMsgId)
                    .field("dragTargetFolder", "")
                    .field("dragMsgId", "")
                    .field("folderId", "")
                    .field("actionOp", "")
                    .field("contextFolderId", "4")
                    .field("doMessageAction", "1")
                    .field("crumb", crumb)
                    .field("selectedRow", "0")
                    .asString();

            bRet = true;

            log.info("email.it: delmessage end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     */
    @Override
    public final void delMessageEnd() {
        log.info("email.it: delmessageEnd init");
        log.info("email.it: delmessageEnd end");
    }

    private boolean addEmails(final Document strongDoc) {
        AtomicBoolean ret = new AtomicBoolean(false);
        Elements trs = strongDoc.getElementsByClass("ZhRow");
        trs.addAll(strongDoc.getElementsByClass("ZhRowOdd"));
        trs.forEach(tr -> {
            Elements as = tr.getElementsByTag("a");
            as.forEach(a -> {
                String href = a.attr("href");
                String token = "&id=";
                if (href.contains(token)) {
                    String id = href.substring(href.indexOf(token) + token.length());
                    if (addEmailInfo(id, 1000)) {
                        log.info("email.it: add email [{}]", id);
                        ret.set(true);
                    }
                }
            });
        });
        return ret.get();
    }

}
