/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title: Libero HTML2POP3.
 * Convertitore da HTML a POP3 per libero.it.
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.Timer;
import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.utils.CharsetCoding;
import it.baccan.html2pop3.utils.HTMLTool;
import it.baccan.html2pop3.utils.LineFormat;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Random;
import java.util.ArrayList;
import java.util.TimerTask;
import kong.unirest.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginLibero extends POP3Base implements POP3Plugin {

    /**
     *
     */
    public static final int MAIL_LIBERO = 1;

    /**
     *
     */
    public static final int MAIL_INWIND = 2;

    /**
     *
     */
    public static final int MAIL_IOL = 3;

    /**
     *
     */
    public static final int MAIL_BLU = 4;

    /**
     *
     */
    public static final int MAIL_GIALLO = 5;
    // Server di riferimento
    private int nMail = 0;

    // Proprietà di sessione
    private Properties p;

    // Elenco di tutte le sessioni attive
    private static final Properties SESSIONS = new Properties();

    /**
     *
     * @param n
     */
    public PluginLibero(final int n) {
        super();
        nMail = n;
    }

    /**
     *
     * @param cUserParam
     * @param cPwd
     * @return true for successfull login.
     */
    @Override
    public final boolean login(final String cUserParam, final String cPwd) {
        boolean bRet = false;

        if ("1.6".equalsIgnoreCase(System.getProperty("java.specification.version"))) {
            log.info("Libero: versione 1.6 non compatibile con Libero.it");
        } else {

            // Provo a prendere la sessione dalla cache
            String key = "" + nMail + ":" + cUserParam + ":" + cPwd;
            p = (Properties) SESSIONS.get(key);
            if (p != null) {
                // Verifico la validità della sessione
                //p = null;
                try {
                    String cPage = getPage(p.getProperty("homepage"), p.getProperty("cookie")).toString();
                    bRet = (cPage.contains("/cp/ps/Main/loadingInside"));
                } catch (Throwable t) {
                    // p.put("cookie","")
                }
                if (bRet) {
                    log.info("Libero: utilizzo sessione in cache");
                }
            }

            // Se la sessione non è valida
            if (!bRet) {
                try {
                    bRet = subLogin(cUserParam, cPwd);
                } catch (UnsupportedEncodingException ex) {
                    log.error("UnsupportedEncodingException subLogin", ex);
                }
            }

            // Se è valida, la memorizzo/sovrascrivo
            if (bRet) {
                SESSIONS.put(key, p);
            }
        }
        // Restituisco il valore della login
        return bRet;
    }

    private boolean subLogin(final String cUserParam, final String cPwd) throws UnsupportedEncodingException {
        {
            HttpResponse<String> response = getUnirest().get("https://evnt.iol.it/v2?ck=wwwtest.libero.it%2Chp%2C%2C%2C%2Chp%2Cheader%2Cmenu_autenticazione%2Clink_mail%2Clogin%2Chttps%253A%252F%252Flogin.libero.it%252F%2Ca%2C%2C1192%2C%2C6874&nc=1556297681257")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://www.libero.it/")
                    .header("connection", "keep-alive")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://login.libero.it/")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://www.libero.it/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js")
                    .header("host", "ajax.googleapis.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().post("https://csm.fr.eu.criteo.net/gev?entry=c~Gum.FirefoxSyncframe.CookieRead.uid~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.Lwid.Origin.3~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.IdCpy.Origin.3~1")
                    .header("host", "csm.fr.eu.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=www.libero.it")
                    .header("connection", "keep-alive")
                    .header("content-length", "0")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/js/placeholders.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/common/tech_includes/lib/policy_cookieCMP.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/common/tech_includes/lib/cmp.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it//mail/login/2018/libero/img/logo-quifinanza.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/iplug/js/lib/iol/analytics/data/login-libero-it/tracking_login-libero-it.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/iplug/js/lib/iol/analytics/engine/IOL.Analytics.Tracking.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://onetag.mgr.consensu.org/cmp.js")
                    .header("cookie", "euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0")
                    .header("host", "onetag.mgr.consensu.org")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.login.step1%2C1%2C1536x864%2C24%2C1%2C1556297682780%2Chttps%3A%2F%2Fwww.libero.it%2F%2C1536x438%2C0&pu=https%3A%2F%2Flogin.libero.it%2F&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=login&cg6=step1&cg7=libero.web.messaging.smart.login.step1&cp1=www.libero.it&cp2=https%3A%2F%2Fwww.libero.it%2F&cp4=no-refresh&cp7=utf-8&cp9=1.1.6&cp10=20181029105520&cp11=Libero%20Mail%20-%20login&cp12=web&cp24=webmail&cp25=https%3A&cp26=login.libero.it&cp103=https%3A%2F%2Flogin.libero.it%2F")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/img/logo-libero.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&amp%3Bcg=0&amp%3Bsi=http%3A%2F%2Flogin.libero.it%2F&seq=1556297682748")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/conf/PB842EDC3-BDDA-4494-9CDE-8B0150370A55.js#name=nlsnInstance&ns=NOLBUNDLE")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://login.libero.it/images/libero_favicon.ico")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://vendorlist.consensu.org/vendorlist.json")
                    .header("host", "vendorlist.consensu.org")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("origin", "https://login.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.login.step1%2C1%2C1536x864%2C24%2C1%2C1556297683397%2Chttps%3A%2F%2Fwww.libero.it%2F%2C1536x438%2C0&pu=https%3A%2F%2Flogin.libero.it%2F&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=login&cg6=step1&cg7=libero.web.messaging.smart.login.step1&cp1=www.libero.it&cp2=https%3A%2F%2Fwww.libero.it%2F&cp4=no-refresh&cp7=utf-8&cp9=1.1.6&cp10=20181029105520&cp11=Libero%20Mail%20-%20login&cp12=web&cp24=webmail&cp25=https%3A&cp26=login.libero.it&cp103=https%3A%2F%2Flogin.libero.it%2F&ct=adblock&ck1=4")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://onetag-sys.com/cmp-get-google-consent/")
                    .header("cookie", "OTP=B-jAmtUWll_Kixcgwb4FMdzGYr82SSKRxHpf9uJKn9Y")
                    .header("host", "onetag-sys.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("origin", "https://login.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/js/2/nlsSDK600.bundle.min.js")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://onetag-sys.com/cmp-set-google-consent/?value=1")
                    .header("cookie", "OTP=B-jAmtUWll_Kixcgwb4FMdzGYr82SSKRxHpf9uJKn9Y")
                    .header("host", "onetag-sys.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=session&c9=devid%2C&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&sessionId=giOHcDTAKwiu9lGUJK9TrFJYZZPwv1556297684&c16=sdkv%2Cbj.6.0.0&uoo=&retry=0")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://www.facebook.com/brandlift.php?sessionId=giOHcDTAKwiu9lGUJK9TrFJYZZPwv1556297684&media_type=dcr&advertiser_id=NA")
                    .header("cookie", "fr=0dDemFh5wtGtdEP1X.AWWi0SL9HGSt6QpvcrWSJM1i7Bs.BZOI1M.xF.Fy_.0.0.Bcwjg9.AWWDfbZO; datr=Qf84WUHiGH57v4p2MHKn156n; sb=SP84WQPoYSy4yO8WQzuGj7j0; c_user=543107502; xs=75%3A1nWt-QrpXlsmfg%3A2%3A1496907593%3A12204%3A10277; wd=1536x710; dpr=1.25; spin=r.1000642222_b.trunk_t.1556216200_s.1_v.2_; act=1556234058605%2F29; presence=EDvF3EtimeF1556234079EuserFA2543107502A2EstateFDt3F_5b_5dEutc3F1556216575666G556234079718CEchFDp_5f543107502F2CC")
                    .header("host", "www.facebook.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=giOHcDTAKwiu9lGUJK9TrFJYZZPwv1556297684&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562976841608610&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297682756&c3=st%2Cc&c64=starttm%2C1556297685&adid=1556297682756&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297686&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Flogin.libero.it%2F&c66=mediaurl%2C&c62=sendTime%2C1556297686&rnd=226165")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().post("https://login.libero.it/logincheck.php")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("content-length", "136")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("SERVICE_ID=webmail&RET_URL=https%3A%2F%2Fmail.libero.it%2Fappsuite%2Fapi%2Flogin%3Faction%3DliberoLogin&LOGINID=testplugin01%40libero.it")
                    .asString();
            response = getUnirest().get("https://login.libero.it/key.phtml")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_LOG_TMP_CK=V1-9f39aac4223a1d1c57f49e124c329a79-1477aa8487f6bb0809f290c05e91c3e37fd5a8404fec018409a888de1f400a0f497f48c00e89405590c7938a42d5ade97947b84f72964e360c18fe476865968109ed4c8b5d150d311a7b776603756ecee8bde1110b9b7cc8ed2d1180e0edae358d6d32379de842082bc7578ed510a10d99fdad19292865797c8a9dfc9dd770dc27720da2fb10001fa781b73baa1fec22-84d7fca4c3013b64798792e98148571b2c1cce99")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js")
                    .header("host", "ajax.googleapis.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/js/placeholders.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/common/tech_includes/lib/policy_cookieCMP.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/common/tech_includes/lib/cmp.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it//mail/login/2018/libero/img/logo-motorlife.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/iplug/js/lib/iol/analytics/data/login-libero-it/tracking_login-libero-it.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/iplug/js/lib/iol/analytics/engine/IOL.Analytics.Tracking.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://onetag.mgr.consensu.org/cmp.js")
                    .header("cookie", "euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0")
                    .header("host", "onetag.mgr.consensu.org")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.login.step2%2C1%2C1536x864%2C24%2C1%2C1556297693645%2Chttps%3A%2F%2Flogin.libero.it%2F%2C1536x438%2C0&pu=https%3A%2F%2Flogin.libero.it%2Fkey.phtml&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=login&cg6=step2&cg7=libero.web.messaging.smart.login.step2&cp1=login.libero.it&cp2=https%3A%2F%2Flogin.libero.it%2F&cp4=no-refresh&cp7=utf-8&cp9=1.1.6&cp10=20181029105520&cp11=Libero%20Mail%20-%20login&cp12=web&cp24=webmail&cp25=https%3A&cp26=login.libero.it&cp103=https%3A%2F%2Flogin.libero.it%2Fkey.phtml")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/img/logo-libero.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/img/ico-off.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://vendorlist.consensu.org/vendorlist.json")
                    .header("host", "vendorlist.consensu.org")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("origin", "https://login.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://onetag-sys.com/cmp-get-google-consent/")
                    .header("cookie", "OTP=B-jAmtUWll_Kixcgwb4FMdzGYr82SSKRxHpf9uJKn9Y")
                    .header("host", "onetag-sys.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("origin", "https://login.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.login.step2%2C1%2C1536x864%2C24%2C1%2C1556297694386%2Chttps%3A%2F%2Flogin.libero.it%2F%2C1536x438%2C0&pu=https%3A%2F%2Flogin.libero.it%2Fkey.phtml&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=login&cg6=step2&cg7=libero.web.messaging.smart.login.step2&cp1=login.libero.it&cp2=https%3A%2F%2Flogin.libero.it%2F&cp4=no-refresh&cp7=utf-8&cp9=1.1.6&cp10=20181029105520&cp11=Libero%20Mail%20-%20login&cp12=web&cp24=webmail&cp25=https%3A&cp26=login.libero.it&cp103=https%3A%2F%2Flogin.libero.it%2Fkey.phtml&ct=adblock&ck1=4")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://onetag-sys.com/cmp-set-google-consent/?value=1")
                    .header("cookie", "OTP=B-jAmtUWll_Kixcgwb4FMdzGYr82SSKRxHpf9uJKn9Y")
                    .header("host", "onetag-sys.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&amp%3Bcg=0&amp%3Bsi=http%3A%2F%2Flogin.libero.it%2F&seq=1556297693617")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/conf/PB842EDC3-BDDA-4494-9CDE-8B0150370A55.js#name=nlsnInstance&ns=NOLBUNDLE")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://login.libero.it/images/libero_favicon.ico")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_LOG_TMP_CK=V1-9f39aac4223a1d1c57f49e124c329a79-1477aa8487f6bb0809f290c05e91c3e37fd5a8404fec018409a888de1f400a0f497f48c00e89405590c7938a42d5ade97947b84f72964e360c18fe476865968109ed4c8b5d150d311a7b776603756ecee8bde1110b9b7cc8ed2d1180e0edae358d6d32379de842082bc7578ed510a10d99fdad19292865797c8a9dfc9dd770dc27720da2fb10001fa781b73baa1fec22-84d7fca4c3013b64798792e98148571b2c1cce99")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/js/2/nlsSDK600.bundle.min.js")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=session&c9=devid%2C&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&sessionId=OyyVuZJj67uq8bWvLIMngnWimmR0F1556297695&c16=sdkv%2Cbj.6.0.0&uoo=&retry=0")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://www.facebook.com/brandlift.php?sessionId=OyyVuZJj67uq8bWvLIMngnWimmR0F1556297695&media_type=dcr&advertiser_id=NA")
                    .header("cookie", "fr=0dDemFh5wtGtdEP1X.AWWi0SL9HGSt6QpvcrWSJM1i7Bs.BZOI1M.xF.Fy_.0.0.Bcwjg9.AWWDfbZO; datr=Qf84WUHiGH57v4p2MHKn156n; sb=SP84WQPoYSy4yO8WQzuGj7j0; c_user=543107502; xs=75%3A1nWt-QrpXlsmfg%3A2%3A1496907593%3A12204%3A10277; wd=1536x710; dpr=1.25; spin=r.1000642222_b.trunk_t.1556216200_s.1_v.2_; act=1556234058605%2F29; presence=EDvF3EtimeF1556234079EuserFA2543107502A2EstateFDt3F_5b_5dEutc3F1556216575666G556234079718CEchFDp_5f543107502F2CC")
                    .header("host", "www.facebook.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=OyyVuZJj67uq8bWvLIMngnWimmR0F1556297695&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562976953038912&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297693627&c3=st%2Cc&c64=starttm%2C1556297696&adid=1556297693627&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297696&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Flogin.libero.it%2Fkey.phtml&c66=mediaurl%2C&c62=sendTime%2C1556297696&rnd=624315")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/login/2018/libero/img/ico-on.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://ssl-i.plug.it/mail/login/2018/libero/css/style.css?v=1")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().post("https://login.libero.it/keycheck.php")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; cookies_enabled=yes; _gid=GA1.2.1033993157.1556297362; mail_arr=V1-717159257b66d4384f1629c487b07929-40812f384d61eef08bedac0da6f9e8c4aa03899ee6b0cb0b781df5e4fc5b1029aab615a1bac79cc8b8ec8386026f9232add912b526746f2ceaea3b2d9a643b49-ac8accd6e98913f064c2f1b91637afd634aa66ee; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297524; LIB_ADV_ECK=1556297524%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_LOG_TMP_CK=V1-9f39aac4223a1d1c57f49e124c329a79-1477aa8487f6bb0809f290c05e91c3e37fd5a8404fec018409a888de1f400a0f497f48c00e89405590c7938a42d5ade97947b84f72964e360c18fe476865968109ed4c8b5d150d311a7b776603756ecee8bde1110b9b7cc8ed2d1180e0edae358d6d32379de842082bc7578ed510a10d99fdad19292865797c8a9dfc9dd770dc27720da2fb10001fa781b73baa1fec22-84d7fca4c3013b64798792e98148571b2c1cce99")
                    .header("host", "login.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("content-length", "584")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("fullFingerprint%5Buseragent%5D=Mozilla%2F5.0%2B(Windows%2BNT%2B10.0%3B%2BWOW64%3B%2Brv%3A66.0)%2BGecko%2F20100101%2BFirefox%2F66.0&fullFingerprint%5Blanguage%5D=&fullFingerprint%5Bcolor%5D=24&fullFingerprint%5Bscreen%5D=864x1536&fullFingerprint%5Btimezone%5D=-120&fullFingerprint%5Bsessionstorage%5D=true&fullFingerprint%5Blocalstorage%5D=true&fullFingerprint%5Bcpu%5D=undefined&fullFingerprint%5Bplatform%5D=Win32&fullFingerprint%5Bdonottrack%5D=unspecified&fullFingerprint%5Bplugin%5D=&fullFingerprint%5Bcanvas%5D=&hashFingerprint=2794266614&LOGINID=" + cUserParam + "&PASSWORD=" + cPwd)
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/login?action=liberoLogin")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/#session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://login.libero.it/key.phtml")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/login/login.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/boot.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/precore.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/default/favicon.ico")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/login?action=rampup&rampup=true&rampUpFor=open-xchange-appsuite&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/system?action=whoami&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/jslob?action=list&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "13")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("[\"io.ox.iol\"]")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/jslob?action=list&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "16")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("[\"io.ox/portal\"]")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/notifications/config.json?_=1556297708985")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://cdn.onesignal.com/sdks/OneSignalSDK.js?_=1556297708987")
                    .header("cookie", "__cfduid=da4c30cbd730ac8d32d610562af854bad1527000823")
                    .header("host", "cdn.onesignal.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/notifications/config.json?25938295=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/libero/config/textlinkrc.json")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/io.ox.iol/box/config_libero.json?1556297711454=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().post("https://mail.libero.it/appsuite/api/login")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "53")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("action=store&session=4f90bb3a824e45a9a56d83930388b894")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/oauth/services?action=all&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/oauth/accounts?action=all&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://cdn.onesignal.com/sdks/OneSignalPageSDKES6.js?v=150703")
                    .header("cookie", "__cfduid=da4c30cbd730ac8d32d610562af854bad1527000823")
                    .header("host", "cdn.onesignal.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/multiple?session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "81")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("[{\"action\":\"all\",\"module\":\"fileservice\"},{\"action\":\"all\",\"module\":\"fileaccount\"}]")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/3rd.party/font-awesome/fonts/fontawesome-webfont.woff2?v=4.4.0")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/font-woff2;q=1.0,application/font-woff;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "identity")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/libero/icon180.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/libero/favicon.ico")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/img/ico_diamond.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/img/ico_libero.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/default/logo-small.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/img/logo_libero.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/fallback-image-contact.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/3rd.party/open-sans-fontface/fonts/Light/OpenSans-Light.woff")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/font-woff2;q=1.0,application/font-woff;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "identity")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/user?action=get&timezone=utc&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/default/sounds/bell.mp3")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,application/ogg;q=0.7,video/*;q=0.6,*/*;q=0.5")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("range", "bytes=0-")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail?action=all&folder=default0%2FINBOX&columns=610%2C600%2C601%2C611&unseen=true&deleted=false&sort=610&order=desc&limit=100&timezone=utc&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/folders?action=list&all=0&altNames=true&parent=default0%2FINBOX&timezone=UTC&tree=0&session=4f90bb3a824e45a9a56d83930388b894&columns=1%2C2%2C3%2C4%2C5%2C6%2C20%2C23%2C300%2C301%2C302%2C304%2C305%2C306%2C307%2C308%2C309%2C310%2C311%2C312%2C313%2C314%2C315%2C316%2C317%2C318%2C3010%2C3020%2C3030%2C3050")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/libero/footer/footer.html")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/forecast.html")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail?action=get&timezone=utc&embedded=true&id=1&unseen=true&view=html&max_size=102400&folder=default0%2FINBOX&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail?action=all&folder=default0%2FINBOX&columns=102%2C600%2C601%2C602%2C603%2C604%2C605%2C606%2C607%2C608%2C610%2C611%2C614%2C652%2C656&sort=610&order=desc&timezone=utc&limit=0%2C50&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/forecast.html")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/local_slider.html")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/mail/crt/l_crt_wgtbox2.html")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ssl-i.plug.it/mail/js/underscore-min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ssl-i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/analytics/engine/IOL.Analytics.Tracking.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/t/l/comscore_engine.js_?v=1.6.0")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/widget.js?20171106=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/cms/img109/hp/01/102/2019/4/1555495611-IMG_LIBEROMAIL.JPG")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/cms/img109/hp/01/102/2017/5/WIDGET_A_MAILPLUS.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/cms/img109/hp/01/102/2017/5/app_liberomail.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/cms/img109/hp/01/102/2017/12/WIDGET_mailpec.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/widget.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/widget.js?20171106=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/bootstrap.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-ui-small.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/widgetLocalSlider.min.js?v2=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://code.jquery.com/ui/1.9.0/themes/smoothness/jquery-ui.css")
                    .header("cookie", "__utma=160157602.1206876524.1518017879.1518017879.1518017879.1; __cfduid=d6479a6cf5dbd223f1740bdbada3442661550588977")
                    .header("host", "code.jquery.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://static.criteo.net/js/ld/publishertag.js")
                    .header("host", "static.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_wgtbox2.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/img/tutorial_arrow.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/apps/themes/libero/img/add_widget.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/multiple?session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "76")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("[{\"action\":\"mail\",\"module\":\"quota\"},{\"action\":\"filestore\",\"module\":\"quota\"}]")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/bootstrap.min.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/widget.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?bxmp=webmail.libero.it%2C%2Cmail.libero.it%2C%252Fappsuite%252F%2Cadblock-detect-stats%2Cadblock-detected%2C0%2C&nc=1556297716442")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/prev.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/next.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/webmail30/cms/contentCMS/wu3/l/hp/com_widget/lancio.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/oggisulibero/oggisulibero.json?m=55&h=18&d=5")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/3bmeteo/json/meteo/58/58091.js?callback=meteo_prev_cb&_=1556297716787")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/hplibero/v1_3/font/icolibero/icolibero.woff2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/font-woff2;q=1.0,application/font-woff;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "identity")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("origin", "https://plug.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/arrow_weather.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/oroscopo30/oroscopo30.xml?_=1556297716788")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/xml, text/xml, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/common_local/feedhp/058/hp_news/058091.json")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/3bmeteo/json/meteo/58/58091.js?callback=meteo_prev_cb&_=1556297716758")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/oroscopo30/oroscopo30.xml?_=1556297716759")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/xml, text/xml, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/dilei/8cc27e22e004ac8a0e471c47bf61b0c6/maria-de-filippi-gabriele-costanzo-instagram-1217.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/dilei/c402eac2026cfec580f3e234a3e25c04/scrub.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/d7903ba829b583576df81e199ec94167/5036649783001_6023894784001_6023795443001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/f035a70223b000db0f72f0de5d8c02d1/5036649783001_5987404228001_5982448672001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/notizie.virgilio.it/cms/2019/04/siri-salvini.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/supereva/cms/2019/04/bilancia.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/motorlife/1399982a39ad69146f59d5f5578af3c7/renault-city-k-ze.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/sport.virgilio.it/cms/2019/04/emiliano-sala_1129292.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/a23a7277b11555aba672d0cd8a44753e/5036649783001_5622274516001_5622265545001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/buonissimo.org/cms/2019/04/cotololette-alla-messinese.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_slider.html?rnd=765815")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/images/n_canali/notizie/ansa/ORIGINALE/7de/7de8114229af382f60a4ae294fa74118.jpg?w=98&h=74&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/common_local/feedhp/058/hp_eventi/058091.json")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/images//local/tnet/eventin/b/3/tnet_3160d2f0469641bca8adbe5e9a01fb33_d.jpg?w=98&h=122&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local%20content.html?c=Roma&i=058091&h=roma")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://onesignal.com/api/v1/sync/9cbc3e84-6abd-4171-8ee0-0dd4cfde53af/web?callback=__jp0")
                    .header("cookie", "__cfduid=da4c30cbd730ac8d32d610562af854bad1527000823")
                    .header("host", "onesignal.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/local/sskin_seat/img/meteo/PNG_90x90_blue/2_1_1.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/local/sskin_seat/img/meteo/PNG_90x90_blue/2_0_0.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/local/sskin_seat/img/meteo/PNG_90x90_blue/2_1_1.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/local/sskin_seat/img/meteo/PNG_90x90_blue/2_0_0.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/forecast.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/techsec/techsec.json?rnd=863253")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/syncframe?topUrl=mail.libero.it#%7B%22optout%22:%7B%22value%22:false,%22origin%22:0%7D,%22uid%22:%7B%22value%22:%22573cebf8-09d1-4911-97fd-ff1ec53b9da2%22,%22origin%22:3%7D,%22idfs%22:%7B%22origin%22:0%7D,%22sid%22:%7B%22origin%22:0%7D,%22origin%22:%22publishertag%22,%22version%22:65,%22lwid%22:%7B%22value%22:%2229e2e77c-ef47-4bc4-b7fc-828804c6b486%22,%22origin%22:3%7D,%22tld%22:%22plug.it%22,%22topUrl%22:%22mail.libero.it%22%7D")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_wgtbox2.html")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/local_slider.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://code.jquery.com/ui/1.9.0/themes/smoothness/images/ui-bg_flat_75_ffffff_40x100.png")
                    .header("cookie", "__utma=160157602.1206876524.1518017879.1518017879.1518017879.1; __cfduid=d6479a6cf5dbd223f1740bdbada3442661550588977")
                    .header("host", "code.jquery.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://code.jquery.com/ui/1.9.0/themes/smoothness/jquery-ui.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/prev.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/next.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/general_feed/wgt_video/wgt_video.json?m=55&h=18&d=5")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/tecnologia/cms/2017/11/password-pc-smartphone.jpg?w=465&h=325&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_tec.html?rnd=317860")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/mail/tracking/groupm.json?_=1556297708990")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&cg=0&si=http%3A%2F%2Fposta.libero.it%2Fcore_apps_changeapp_action.io_ox_portal&seq=1556297719295")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/conf/PB842EDC3-BDDA-4494-9CDE-8B0150370A55.js#name=nlsnInstance&ns=NOLBUNDLE")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/sid/pixel?origin=publishertag&domain=plug.it&topUrl=mail.libero.it&idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2&lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/d7903ba829b583576df81e199ec94167/5036649783001_6023894784001_6023795443001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/a23a7277b11555aba672d0cd8a44753e/5036649783001_5622274516001_5622265545001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/libero/images/video/9de68fb303b4dc1f55af1da404ef6387/5036649783001_6026725991001_6026720438001-vs.jpg?w=328&h=229&a=c")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/portal/feed_video.html?rnd=82743")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/img/ico-video.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/css/widget_libero.css")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.home%2C1%2C1536x864%2C24%2C1%2C1556297719871%2Chttps%3A%2F%2Fmail.libero.it%2Fappsuite%2F%2C1536x438%2C0&pu=http%3A%2F%2Fposta.libero.it%2Fcore_apps_changeapp_action.io_ox_portal&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=home&cg7=libero.web.messaging.smart.home&cp1=mail.libero.it&cp2=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F&cp4=no-refresh&cp7=utf-8&cp8=3.0&cp9=1.1.6&cp10=20170403090019&cp11=&cp12=web&cp25=http%3A&cp26=posta.libero.it&cp103=http%3A%2F%2Fposta.libero.it%2Fcore_apps_changeapp_action.io_ox_portal")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?ck=webmail.libero.it%2C%2Cmail.libero.it%2C%252Fappsuite%252F%2Cadblock-dialog%2C%2Cmodaldialog%2C%2Cclosepopup%2C%2C%2C%2C%2C0%2C%2C0&nc=1556297719952")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/js/2/nlsSDK600.bundle.min.js")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://ag.gbc.criteo.com/idsd?cpp=uS8ATXxkU3VpOVpubW9NY2d5UTdJNUdEME9UQkJmMjkycFRYd3RmOG9FSkZGQUFaWjJ0bEJ2cFZLZis4T1plVkZ3U0x4ZW9IZWp2a1BhdHRjZHdiK2xmcjZRWm54VFdDWW4xS2ZYUktRd0dLMzlEZjRnZkdFV2VnWktWelJJVTdqUHpuS1lQY3l1M2Jienh6UEYyZktTbC9heEM0dnlUOThjNXlKWlA4SDhkNFpxUFNGM0I1UGFRNG1BRndtdEd0RkoxdjVUZXM3ZG9oTVM0WWVtOXdsRzVhQXptVEVUNFFCVTNyUm8wVElKc2pyYXFzPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ag.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?bxmp=webmail.libero.it%2C%2Cmail.libero.it%2C%252Fappsuite%252F%2Cadblock-dialog%2Cadblock-dialog%2C0%2C&nc=1556297720233")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://gem.gbc.criteo.com/sid?cpp=WgI9wXxkU3VpOVpubW9NY2d5UTdJNUdEME9UQkJmMjkycFRYd3RmOG9FSkZGQUFaWjJ0bEJ2cFZLZis4T1plVkZ3U0x4ZW9IZWp2a1BhdHRjZHdiK2xmcjZRWm54VFdDWW4xS2ZYUktRd0dLMzlEZjRnZkdFV2VnWktWelJJVTdqUHpuS1lQY3l1M2Jienh6UEYyZktTbC9heEM0dnlUOThjNXlKWlA4SDhkNFpxUFFXalo3Szl0eXBmZDgvaFROQ3F6elZqK2pqdkg4cGZocUlFMnFPQnpxWUhaaWtFMTlqYzBKS3ErNWkyS1BXZkhzPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gem.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "cdn-gl.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=session&c9=devid%2C&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&c16=sdkv%2Cbj.6.0.0&uoo=&retry=0")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://www.facebook.com/brandlift.php?sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&media_type=dcr&advertiser_id=NA")
                    .header("cookie", "fr=0dDemFh5wtGtdEP1X.AWWi0SL9HGSt6QpvcrWSJM1i7Bs.BZOI1M.xF.Fy_.0.0.Bcwjg9.AWWDfbZO; datr=Qf84WUHiGH57v4p2MHKn156n; sb=SP84WQPoYSy4yO8WQzuGj7j0; c_user=543107502; xs=75%3A1nWt-QrpXlsmfg%3A2%3A1496907593%3A12204%3A10277; wd=1536x710; dpr=1.25; spin=r.1000642222_b.trunk_t.1556216200_s.1_v.2_; act=1556234058605%2F29; presence=EDvF3EtimeF1556234079EuserFA2543107502A2EstateFDt3F_5b_5dEutc3F1556216575666G556234079718CEchFDp_5f543107502F2CC")
                    .header("host", "www.facebook.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://cdn-gl.imrworldwide.com/novms/html/ls.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://onesignal.com/webPushAnalytics")
                    .header("cookie", "__cfduid=da4c30cbd730ac8d32d610562af854bad1527000823")
                    .header("host", "onesignal.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562977204205187&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297719297&c3=st%2Cc&c64=starttm%2C1556297721&adid=1556297719297&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297721&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F%23&c66=mediaurl%2C&c62=sendTime%2C1556297721&rnd=762799")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?ck=webmail.libero.it%2CHOME%2Cmail.libero.it%2C%252Fappsuite%252F%2Cwgt-inbox%2C%2Cwidget%2Cinbox%2Cwgt-inbox-postainarrivo-leggi%2C%2C%2C%2C%2C0%2C%2C0&nc=1556297723101")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/jslob?action=set&id=io.ox%2Fmail&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "2393")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("{\"removeDeletedPermanently\":false,\"contactCollectOnMailTransport\":true,\"contactCollectOnMailAccess\":false,\"useFixedWidthFont\":false,\"appendVcard\":false,\"sendDispositionNotification\":true,\"appendMailTextOnReply\":true,\"forwardMessageAs\":\"Inline\",\"messageFormat\":\"alternative\",\"lineWrapAfter\":80,\"defaultSendAddress\":\"testplugin01@libero.it\",\"autoSaveDraftsAfter\":\"3_minutes\",\"allowHtmlMessages\":true,\"allowHtmlImages\":true,\"displayEmoticons\":true,\"isColorQuoted\":true,\"defaultSignature\":false,\"defaultReplyForwardSignature\":false,\"mobileSignatureType\":\"none\",\"threadSupport\":false,\"sort\":\"thread\",\"order\":\"desc\",\"unread\":false,\"simpleLineBreaks\":true,\"notificationSoundName\":\"bell\",\"playSound\":true,\"folderview\":{\"visible\":{\"large\":true}},\"viewOptions\":{\"default0/INBOX\":{\"sort\":610,\"order\":\"desc\",\"thread\":false}},\"showContactPictures\":false,\"showExactDates\":false,\"showCompactLayout\":false,\"alwaysShowSize\":false,\"enforcesecureconnection\":false,\"contactCollectFolder\":31,\"defaultseparator\":\"/\",\"saveNoCopyInSentFolder\":false,\"compose\":{\"shareAttachments\":{\"threshold\":26214400,\"enabled\":true,\"forceAutoDelete\":true,\"name\":\"JumboMAIL\",\"requiredExpiration\":true}},\"namespace\":\"\",\"addresses\":[\"testplugin01@libero.it\"],\"defaultFolder\":{\"drafts\":\"default0/draft\",\"spam\":\"default0/Spam\",\"inbox\":\"default0/INBOX\",\"sent\":\"default0/outbox\",\"trash\":\"default0/trash\"},\"folder\":{\"drafts\":\"default0/draft\",\"spam\":\"default0/Spam\",\"inbox\":\"default0/INBOX\",\"sent\":\"default0/outbox\",\"trash\":\"default0/trash\"},\"showReplyTo\":{\"configurable\":false},\"categories\":{\"enabled\":true,\"forced\":false,\"initialized\":\"notyetstarted\",\"list\":[{\"id\":\"general\",\"name\":\"PRINCIPALE\",\"active\":true,\"permissions\":[\"train\"]},{\"id\":\"offerte\",\"name\":\"OFFERTE\",\"description\":\"Offerte, promozioni ed altre mail marketing.\",\"active\":true,\"permissions\":[\"disable\",\"train\"]},{\"id\":\"social\",\"name\":\"SOCIAL\",\"description\":\"Messaggi provenienti da social network e siti di condivisione multimediale\",\"active\":true,\"permissions\":[\"disable\",\"train\"]},{\"id\":\"utenze\",\"name\":\"UTENZE\",\"description\":\"Messaggi provenienti da banche, societÃ \\ttelefoniche, di energia e gas, etc.\",\"active\":false,\"permissions\":[\"disable\",\"train\"]}]},\"defaultaddress\":\"testplugin01@libero.it\",\"separators\":{\"0\":\"/\"},\"archive\":{\"days\":90},\"listview\":{\"secondaryPageSize\":25,\"primaryPageSize\":25},\"showCheckboxes\":true,\"layout\":\"list\",\"features\":{\"notifyOnSent\":true}}")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/textlink-cms/index.html?25938295=&p=f")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/folders?action=list&all=0&altNames=true&parent=default0&timezone=UTC&tree=0&session=4f90bb3a824e45a9a56d83930388b894&columns=1%2C2%2C3%2C4%2C5%2C6%2C20%2C23%2C300%2C301%2C302%2C304%2C305%2C306%2C307%2C308%2C309%2C310%2C311%2C312%2C313%2C314%2C315%2C316%2C317%2C318%2C3010%2C3020%2C3030%2C3050")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/folders?action=get&altNames=true&id=default0&timezone=UTC&tree=0&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/css/widget_libero.css")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/css,*/*;q=0.1")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/js/jquery-1.12.3.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/iplug/js/lib/iol/evnt/iol_evnt.min.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/mail/textlink-cms/js/textlink.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().post("https://csm.nl.eu.criteo.net/gev?entry=c~Gum.FirefoxSyncframe.CookieRead.uid~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.Lwid.Origin.3~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.IdCpy.Origin.3~1")
                    .header("host", "csm.nl.eu.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("content-length", "0")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail/categories?action=unread&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://plug.libero.it/webmail30/textlink/prod/libero_free.json?55185=")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D")
                    .header("host", "plug.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail?action=all&folder=default0%2FINBOX&columns=102%2C600%2C601%2C602%2C603%2C604%2C605%2C606%2C607%2C608%2C610%2C611%2C614%2C652%2C656%2CX-Open-Xchange-Share-URL%2CDKIM-Signature%2Cx-fbsn&sort=610&order=desc&categoryid=general&timezone=utc&limit=0%2C25&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://wips.plug.it/cips/mail/cms/2018/10/libero-mailplus.png")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "wips.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?bxmp=webmail.libero.it%2CPOSTA%2C%2C%2Cposta-layer-premium-plus-09%2Cposta%3Blayer%3Bpremium-plus-09%2C0%2C&nc=1556297725181")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://plug.libero.it/mail/textlink-cms/index.html?p=f&25938295")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://static.criteo.net/js/ld/publishertag.js")
                    .header("host", "static.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/mail?action=search&columns=600%2C601%2C603%2C607%2C610%2C611%2Cx-fbsn%2CAuthentication-Results%2CDKIM-Signature&folder=default0%2FINBOX&limit=100&order=desc&session=4f90bb3a824e45a9a56d83930388b894&sort=610&timestamp=2116800000000")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "108")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("{\"filter\":[\"and\",[\"=\",{\"field\":\"user_flags\"},\"$dem_inbox\"],[\">\",{\"field\":\"received_date\"},\"1555692925414\"]]}")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/syncframe?topUrl=mail.libero.it#%7B%22optout%22:%7B%22value%22:false,%22origin%22:0%7D,%22uid%22:%7B%22value%22:%22573cebf8-09d1-4911-97fd-ff1ec53b9da2%22,%22origin%22:3%7D,%22idfs%22:%7B%22origin%22:0%7D,%22sid%22:%7B%22origin%22:0%7D,%22origin%22:%22publishertag%22,%22version%22:65,%22lwid%22:%7B%22value%22:%2229e2e77c-ef47-4bc4-b7fc-828804c6b486%22,%22origin%22:3%7D,%22tld%22:%22plug.it%22,%22topUrl%22:%22mail.libero.it%22%7D")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/sid/pixel?origin=publishertag&domain=plug.it&topUrl=mail.libero.it&idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2&lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.posta_elenco.arrivata%2C1%2C1536x864%2C24%2C1%2C1556297725980%2Chttps%3A%2F%2Fmail.libero.it%2Fappsuite%2F%2C1536x438%2C0&pu=http%3A%2F%2Fposta.libero.it%2Fmail_list_load_collection.list.primary.inbox.general&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=posta_elenco&cg6=arrivata&cg7=libero.web.messaging.smart.posta_elenco.arrivata&cp1=mail.libero.it&cp2=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F&cp4=no-refresh&cp7=utf-8&cp8=3.0&cp9=1.1.6&cp10=20170403090019&cp11=&cp12=web&cp25=http%3A&cp26=posta.libero.it&cp103=http%3A%2F%2Fposta.libero.it%2Fmail_list_load_collection.list.primary.inbox.general")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ag.gbc.criteo.com/idsd?cpp=hQ3rFnw4UUdEb2RyT2JsR3NHU1JkeU85aFlmdE1SYjdOM2tvcnlTcGk2Q2tTVDIya3JNcUpLemJUS0x6SmI1aThuVmFnd3c5WnErazVCWmdDZWFRSjExaDgxcy9PaEQyTThIQ2pBeDQwYlZhdlFldjVZQ25EaW8zN1lDMFZjRGJmL04rMHlYR3pDMUI1N09teHJjM21lejJIbXNyL2xjVGJjbU8vU1VCQVdJZldRNlV0OHFhTWRpYTJWNEF2MXFpMVRwdEUrRmRZMGxYdnhxMVZWUmVoQWRNOHRXenk3VjV3anVoNWhhdXhLYzZINTlrPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ag.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://gem.gbc.criteo.com/sid?cpp=yujomHw4UUdEb2RyT2JsR3NHU1JkeU85aFlmdE1SYjdOM2tvcnlTcGk2Q2tTVDIya3JNcUpLemJUS0x6SmI1aThuVmFnd3c5WnErazVCWmdDZWFRSjExaDgxcy9PaEQyTThIQ2pBeDQwYlZhdlFldjVZQ25EaW8zN1lDMFZjRGJmL04rMHlYR3pDMUI1N09teHJjM21lejJIbXNyL2xjVGJjbU8vU1VCQVdJZldRNldzaTdEbi8xeEhPRjdMVFBCS2F3cE8zTS9aOFcxQ0NHWHlIZFdLMXZoaWZBVWsreUh2dElxUUkxN25Zb1YzbUtnPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gem.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/snippet?action=all&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&cg=0&si=http%3A%2F%2Fposta.libero.it%2Fmail_list_load_collection.list.primary.inbox.general&seq=1556297727938")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562977204205187&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297719297&c3=st%2Cc&c64=starttm%2C1556297722&adid=1556297719297&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297727&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F%23&c66=mediaurl%2C&c62=sendTime%2C1556297727&rnd=778434")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/jslob?action=set&id=io.ox%2Fmail&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "2393")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("{\"removeDeletedPermanently\":false,\"contactCollectOnMailTransport\":true,\"contactCollectOnMailAccess\":false,\"useFixedWidthFont\":false,\"appendVcard\":false,\"sendDispositionNotification\":true,\"appendMailTextOnReply\":true,\"forwardMessageAs\":\"Inline\",\"messageFormat\":\"alternative\",\"lineWrapAfter\":80,\"defaultSendAddress\":\"testplugin01@libero.it\",\"autoSaveDraftsAfter\":\"3_minutes\",\"allowHtmlMessages\":true,\"allowHtmlImages\":true,\"displayEmoticons\":true,\"isColorQuoted\":true,\"defaultSignature\":false,\"defaultReplyForwardSignature\":false,\"mobileSignatureType\":\"none\",\"threadSupport\":false,\"sort\":\"thread\",\"order\":\"desc\",\"unread\":false,\"simpleLineBreaks\":true,\"notificationSoundName\":\"bell\",\"playSound\":true,\"folderview\":{\"visible\":{\"large\":true}},\"viewOptions\":{\"default0/INBOX\":{\"sort\":610,\"order\":\"desc\",\"thread\":false}},\"showContactPictures\":false,\"showExactDates\":false,\"showCompactLayout\":false,\"alwaysShowSize\":false,\"enforcesecureconnection\":false,\"contactCollectFolder\":31,\"defaultseparator\":\"/\",\"saveNoCopyInSentFolder\":false,\"compose\":{\"shareAttachments\":{\"threshold\":26214400,\"enabled\":true,\"forceAutoDelete\":true,\"name\":\"JumboMAIL\",\"requiredExpiration\":true}},\"namespace\":\"\",\"addresses\":[\"testplugin01@libero.it\"],\"defaultFolder\":{\"drafts\":\"default0/draft\",\"spam\":\"default0/Spam\",\"inbox\":\"default0/INBOX\",\"sent\":\"default0/outbox\",\"trash\":\"default0/trash\"},\"folder\":{\"drafts\":\"default0/draft\",\"spam\":\"default0/Spam\",\"inbox\":\"default0/INBOX\",\"sent\":\"default0/outbox\",\"trash\":\"default0/trash\"},\"showReplyTo\":{\"configurable\":false},\"categories\":{\"enabled\":true,\"forced\":false,\"initialized\":\"notyetstarted\",\"list\":[{\"id\":\"general\",\"name\":\"PRINCIPALE\",\"active\":true,\"permissions\":[\"train\"]},{\"id\":\"offerte\",\"name\":\"OFFERTE\",\"description\":\"Offerte, promozioni ed altre mail marketing.\",\"active\":true,\"permissions\":[\"disable\",\"train\"]},{\"id\":\"social\",\"name\":\"SOCIAL\",\"description\":\"Messaggi provenienti da social network e siti di condivisione multimediale\",\"active\":true,\"permissions\":[\"disable\",\"train\"]},{\"id\":\"utenze\",\"name\":\"UTENZE\",\"description\":\"Messaggi provenienti da banche, societÃ \\ttelefoniche, di energia e gas, etc.\",\"active\":false,\"permissions\":[\"disable\",\"train\"]}]},\"defaultaddress\":\"testplugin01@libero.it\",\"separators\":{\"0\":\"/\"},\"archive\":{\"days\":90},\"listview\":{\"secondaryPageSize\":25,\"primaryPageSize\":25},\"showCheckboxes\":true,\"layout\":\"list\",\"features\":{\"notifyOnSent\":true}}")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/apps/load/7.8.3-45.20190402.125555,io.ox.iol/mail/categories/picker.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/plain, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().put("https://mail.libero.it/appsuite/api/multiple?session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "text/javascript; charset=UTF-8")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length", "156")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .body("[{\"action\":\"update\",\"id\":\"1\",\"folder\":\"default0/INBOX\",\"timestamp\":2116800000000,\"module\":\"mail\",\"data\":{\"flags\":32,\"value\":true,\"collect_addresses\":true}}]")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/v=7.8.3-45.20190402.125555/apps/themes/libero/fallback-image-contact.png")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/top.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/spazio.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_account.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_allegati.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_organizza.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_drive.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_mobile.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_app_gratis.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_appstore.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/ico_googleplay.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("http://i.plug.it/premium/dem/benvenuto3.0_libero/logo_liberomail.gif")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("cookie", "cto_lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "i.plug.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/halo/contact/picture?email=accounts-noreply%40libero.it&width=40&height=40&scaleType=cover&uniq=1556297709008&user=2&context=24863433&sequence=1")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/apps/load/7.8.3-45.20190402.125555,io.ox/mail/inplace-reply.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/plain, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail/categories?action=unread&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.posta_messaggio.arrivata%2C1%2C1536x864%2C24%2C1%2C1556297730238%2Chttps%3A%2F%2Fmail.libero.it%2Fappsuite%2F%2C1536x438%2C0&pu=http%3A%2F%2Fposta.libero.it%2Fmail_list_showitem_click.list.primary.inbox.general&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=posta_messaggio&cg6=arrivata&cg7=libero.web.messaging.smart.posta_messaggio.arrivata&cp1=mail.libero.it&cp2=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F&cp4=no-refresh&cp7=utf-8&cp8=3.0&cp9=1.1.6&cp10=20170403090019&cp11=&cp12=web&cp25=http%3A&cp26=posta.libero.it&cp103=http%3A%2F%2Fposta.libero.it%2Fmail_list_showitem_click.list.primary.inbox.general")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://static.criteo.net/js/ld/publishertag.js")
                    .header("host", "static.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().post("https://csm.nl.eu.criteo.net/gev?entry=c~Gum.FirefoxSyncframe.CookieRead.uid~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.Lwid.Origin.3~1&entry=c~Gum.FirefoxSyncframe.FragmentData.publishertag.IdCpy.Origin.3~1")
                    .header("host", "csm.nl.eu.criteo.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("content-length", "0")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/syncframe?topUrl=mail.libero.it#%7B%22optout%22:%7B%22value%22:false,%22origin%22:0%7D,%22uid%22:%7B%22value%22:%22573cebf8-09d1-4911-97fd-ff1ec53b9da2%22,%22origin%22:3%7D,%22idfs%22:%7B%22origin%22:0%7D,%22sid%22:%7B%22origin%22:0%7D,%22origin%22:%22publishertag%22,%22version%22:65,%22lwid%22:%7B%22value%22:%2229e2e77c-ef47-4bc4-b7fc-828804c6b486%22,%22origin%22:3%7D,%22tld%22:%22plug.it%22,%22topUrl%22:%22mail.libero.it%22%7D")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://i.plug.it/mail/crt/l_crt_leftcol.html")
                    .header("connection", "keep-alive")
                    .header("upgrade-insecure-requests", "1")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://gum.criteo.com/sid/pixel?origin=publishertag&domain=plug.it&topUrl=mail.libero.it&idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2&lwid=29e2e77c-ef47-4bc4-b7fc-828804c6b486")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gum.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://ag.gbc.criteo.com/idsd?cpp=QdMadnxrUFliZmx0RGxieGI0Zmc1cTduWTBUK0kxbGk1cXVjMW12c3ZCeDc4cEg3Rm8rNkVCbFlUYm9tVmlkQmxlVHY4WFh4S2FwUFJZbnNoZ2NmMmtHbllPdGU4NkRyKzg4T2NvQUpxdTFmZzcwUTMzRHMrTlBITjloRlBwenU0Ynh2azVBNXBVazlPU1RJUnNkWU5ia3RPUWk3Z3hML3h4ZGZyOWVqc0xXRmdiNExuM0d3VVRNS08xa1NmNHVxRFFLRmwzU2pURUNjTXdLbDBMU0N4L09pNG5kdHlrc09mejExeGJpTU5wT0RKQ3BNPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "ag.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://gem.gbc.criteo.com/sid?cpp=uU5T0nxrUFliZmx0RGxieGI0Zmc1cTduWTBUK0kxbGk1cXVjMW12c3ZCeDc4cEg3Rm8rNkVCbFlUYm9tVmlkQmxlVHY4WFh4S2FwUFJZbnNoZ2NmMmtHbllPdGU4NkRyKzg4T2NvQUpxdTFmZzcwUTMzRHMrTlBITjloRlBwenU0Ynh2azVBNXBVazlPU1RJUnNkWU5ia3RPUWk3Z3hML3h4ZGZyOWVqc0xXRmdiNElDVThFZW1LbUhGY2NXcmRhV2FVY1RLZWhTSFRWRHVWSTBHNjhlVzJzaXR0OFJpTzV1S21HbzRjVFpTWTJockxNPXw&cppv=2")
                    .header("cookie", "uid=573cebf8-09d1-4911-97fd-ff1ec53b9da2")
                    .header("host", "gem.gbc.criteo.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://gum.criteo.com/syncframe?topUrl=mail.libero.it")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/folders?action=get&altNames=true&id=default0%2FINBOX&timezone=UTC&tree=0&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&cg=0&si=http%3A%2F%2Fposta.libero.it%2Fmail_list_showitem_click.list.primary.inbox.general&seq=1556297732248")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562977204205187&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297719297&c3=st%2Cc&c64=starttm%2C1556297728&adid=1556297719297&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297732&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F%23&c66=mediaurl%2C&c62=sendTime%2C1556297732&rnd=52138")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://evnt.iol.it/v2?ck=webmail.libero.it%2CPOSTA%2Cmail.libero.it%2C%252Fappsuite%252F%2C%2C%2CLeggimail%2Ctoolbar_read%2CVisualizzailsorgente%2C%2C%2C%2C%2C0%2C%2C0&nc=1556297745538")
                    .header("cookie", "evntuid=XLdClQoCDU4AADiyqLkAAAgX")
                    .header("host", "evnt.iol.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/apps/load/7.8.3-45.20190402.125555,io.ox/mail/actions/source.js")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "text/plain, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://mail.libero.it/appsuite/api/mail?action=get&timezone=utc&embedded=false&id=1&src=true&folder=default0%2FINBOX&view=html&max_size=102400&session=4f90bb3a824e45a9a56d83930388b894")
                    .header("cookie", "_ga=GA1.2.1024765156.1550229286; euconsent=BOcAkp3OfHABYAKAQBITCO-AAAAml7_______9______9uz_Ov_v_f__33e8__9v_l_7_-___u_-33d4-_1vf99yfm1-7ftr3tp_87ues2_Xur__59__3z3_9phPrsk89ry0; policy_cookie=11111111_11111; pc_liberoOff=0; LIB_ADV_G=cede4f7de2ac9ce0c8b64b1b42f68ac0%7C%7C9%7C; HPh=56; cto_lwid=f7bc525a-ea4f-4993-a9d2-b245a99aed57; cto_idcpy=573cebf8-09d1-4911-97fd-ff1ec53b9da2; _fbp=fb.1.1555515377448.1558909778; _gid=GA1.2.1033993157.1556297362; LIB_ADV_CK=6-1-55-9-0-P; LIB_ADV_UCK=1%7C0eb46cead280b431045209da81d54ae8%7C1556297707; LIB_ADV_ECK=1556297707%7C6-1-55-9-0; LIB_ADV_P=8951636433f0822da3a97e2ddafb637b0ba5ae81.f46673d0d61eac5c059bf0a28a04833ef4943321f891d4a46e5843d3f8924590; WEBMAIL20=WM30; WMBROWSER=OK; url.key=5349743327557235534974332755723553497433275572355349743327557235; language=it_IT; metrics-userhash-v2=0760fee8075a78c3376aa851210f8b7d; as=%7B%22pid%22%3A%22b0ab9add636851ff4aa3a7df629c6d3a%22%2C%22audienceId%22%3A%5B%2293078%22%5D%7D; LIB_SSO_CK=ZmM0ODhhMjc0ZDk2ZDBlNTAyZTdkOWJlMjA5ZTVlNTauxOO%252FEyYhypohCUU3dyO64zVHDZ%252BLedNXHa0qdtB7dw%253D%253D; LIB_NAME_CK=ZGVs; LIB_TK_CK=MWM1YjhhYWU4YWUxNTc0NTY4MGQ0ZGZkMjE5OGVkZTOmcFACAO66P5Pw%252BXQRyRt6j%252BnMW9R8DH3cGdnRzlkvKYYQktvtHhn0E%252B242zVTomU%253D; JSESSIONID=6389804326632244094.APP090@SH17; open-xchange-secret-0FRkqJDqXPOKS94YlifSw=ee13388a29bb49c1b844bc930260c1bc; open-xchange-public-session-DowOGKzrhC469zIZKWivA=9144092b0eb040eca22c3999170ce7da; open-xchange-session-0FRkqJDqXPOKS94YlifSw=4f90bb3a824e45a9a56d83930388b894; ADB_FC_CK=; ADB_LT_ST_CK=; keep_openfld_-1322399934=%7B%22mail%22%3Atrue%7D")
                    .header("host", "mail.libero.it")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "application/json, text/javascript, */*; q=0.01")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://italiaonline01.wt-eu02.net/215973748390194/wt?p=433%2Clibero.web.messaging.smart.posta_popup.leggi_sorgente%2C1%2C1536x864%2C24%2C1%2C1556297746169%2Chttps%3A%2F%2Fmail.libero.it%2Fappsuite%2F%2C1536x482%2C0&pu=http%3A%2F%2Fposta.libero.it%2Fmail_message_source_action&la=it&tz=2&cg1=libero&cg2=web&cg3=messaging&cg4=smart&cg5=posta_popup&cg6=leggi_sorgente&cg7=libero.web.messaging.smart.posta_popup.leggi_sorgente&cp1=mail.libero.it&cp2=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F&cp4=no-refresh&cp7=utf-8&cp8=3.0&cp9=1.1.6&cp10=20170403090019&cp11=&cp12=web&cp25=http%3A&cp26=posta.libero.it&cp103=http%3A%2F%2Fposta.libero.it%2Fmail_message_source_action")
                    .header("cookie", "wteid_215973748390194=4154569976200480029; wtsid_215973748390194=1; wt_nbg_Q3=!KM9yUVn87aIs96mVeua4q0vdtk2OGPLiQdbg7xU86bim0wYo8m4usL8gmAWfRZYgouhZ16WGryz3hg==")
                    .header("host", "italiaonline01.wt-eu02.net")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/mail/t/t_libero.html?seq=1556297713689")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/m?ci=libero-it&cg=0&si=http%3A%2F%2Fposta.libero.it%2Fmail_message_source_action&seq=1556297748162")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=V&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=view&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C0&crs=&lat=&lon=&c29=plid%2C15562977204205187&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297719297&c3=st%2Cc&c64=starttm%2C1556297733&adid=1556297719297&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297747&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=0&si=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F%23&c66=mediaurl%2C&c62=sendTime%2C1556297747&rnd=804007")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();
            response = getUnirest().get("https://secure-it.imrworldwide.com/cgi-bin/gn?prd=dcr&ci=it-605193&ch=it-605193_c10_LiberoMessaging_BRW_S&asn=LiberoMessaging_BRW&sessionId=ipEFWdtmtnfG4LxKMkUC7GO2s7MkN1556297720&prv=1&c6=vc%2Cc10&ca=NA&c13=asid%2CPB842EDC3-BDDA-4494-9CDE-8B0150370A55&c32=segA%2CNA&c33=segB%2CNA&c34=segC%2CNA&c15=apn%2C&sup=0&segment2=&segment1=&forward=1&plugv=&playerv=&ad=0&cr=D&c9=devid%2C&enc=true&c1=nuid%2C7b22e941-a38b-4de3-b26c-f3e3e4c13f51&at=timer&rt=text&c16=sdkv%2Cbj.6.0.0&c27=cln%2C43&crs=&lat=&lon=&c29=plid%2C15562977204205187&c30=bldv%2C6.0.0.333&st=dcr&c7=osgrp%2C&c8=devgrp%2C&c10=plt%2C&c40=adbid%2C&c14=osver%2CNA&c26=dmap%2C1&dd=&hrd=&wkd=&c35=adrsid%2C&c36=cref1%2C&c37=cref2%2C&c11=agg%2C1&c12=apv%2C&c51=adl%2C0&c52=noad%2C0&devtypid=&pc=NA&c53=fef%2Cn&c54=oad%2C&c55=cref3%2C&c57=adldf%2C2&ai=1556297719297&c3=st%2Cc&c64=starttm%2C1556297721&adid=1556297719297&c58=isLive%2Cfalse&c59=sesid%2C&c61=createtm%2C1556297764&c63=pipMode%2C&uoo=&c68=bndlid%2C&nodeTM=&logTM=&c73=phtype%2C&c74=dvcnm%2C&c76=adbsnid%2C&c44=progen%2C&davty=1&si=https%3A%2F%2Fmail.libero.it%2Fappsuite%2F%23&c66=mediaurl%2C&c62=sendTime%2C1556297764&rnd=593537")
                    .header("cookie", "CAMPAIGN=61789-1.52425-1.53896-6.63101-1; IMRID=4f452039-6c6f-451b-a815-90bdcb40f24d")
                    .header("host", "secure-it.imrworldwide.com")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0")
                    .header("accept", "image/webp,*/*")
                    .header("accept-language", "it-IT,it;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("accept-encoding", "gzip, deflate, br")
                    .header("referer", "https://mail.libero.it/appsuite/")
                    .header("connection", "keep-alive")
                    .header("pragma", "no-cache")
                    .header("cache-control", "no-cache")
                    .header("te", "Trailers")
                    .asString();

        }
        //boolean bDebug = getDebug();
        p = null;
        // Uso il server in base alla chiamata
        String cChoice = "";
        if (nMail == MAIL_LIBERO) {
            cChoice = "libero.it";
        } else if (nMail == MAIL_INWIND) {
            cChoice = "inwind.it";
        } else if (nMail == MAIL_IOL) {
            cChoice = "iol.it";
        } else if (nMail == MAIL_BLU) {
            cChoice = "blu.it";
        } else if (nMail == MAIL_GIALLO) {
            cChoice = "giallo.it";
        }

        String cFolder;
        String cRead;
        String cUser = cUserParam;

        int nQuestionMark = cUser.indexOf("?");
        if (nQuestionMark != -1) {
            cFolder = URLEncoder.encode(getPar(cUser.substring(nQuestionMark), "folder", "inbox"), CharsetCoding.UTF_8);
            cRead = URLEncoder.encode(getPar(cUser.substring(nQuestionMark), "read", (getRead() ? "true" : "false")), CharsetCoding.UTF_8);
            cUser = cUser.substring(0, nQuestionMark);
        } else {
            cFolder = "inbox";
            cRead = (getRead() ? "true" : "false");
        }

        if (!cUser.contains("@")) {
            cUser += "@" + cChoice;
        }

        // Login
        //System.setProperty("socksProxyHost", "127.0.0.1");
        //System.setProperty("socksProxyPort", "9050");
        try {

            HttpResponse<String> homepage = getUnirest().get("https://www.libero.it").asString();
            logHeaders(homepage);

            // Get cookie session
            HttpResponse<String> homepageResponse = getUnirest().get("https://login.libero.it").asString();
            logHeaders(homepageResponse);

            // Login
            HttpResponse<String> loginResponse = getUnirest().post("https://login.libero.it/logincheck.php")
                    .field("SERVICE_ID", "webmail")
                    .field("RET_URL", "https://mail.libero.it/appsuite/api/login?action=liberoLogin")
                    .field("LOGINID", cUser)
                    .asString();

            // Post
            String sbLogin = loginResponse.getBody();
            logHeaders(loginResponse);
            HttpResponse<String> key = getUnirest().get(getLocation(loginResponse)).asString();
            logHeaders(key);

            // Login
            HttpResponse<String> stringResponse = getUnirest().post("https://login.libero.it/keycheck.php")
                    .field("fullFingerprint[useragent]", "Mozilla/5.0+(Windows+NT+10.0;+WOW64;+rv:66.0)+Gecko/20100101+Firefox/66.0")
                    .field("fullFingerprint[language]", "")
                    .field("fullFingerprint[color]", "24")
                    .field("fullFingerprint[screen]", "864x1536")
                    .field("fullFingerprint[timezone]", "-120")
                    .field("fullFingerprint[sessionstorage]", "true")
                    .field("fullFingerprint[localstorage]", "true")
                    .field("fullFingerprint[cpu]", "undefined")
                    .field("fullFingerprint[platform]", "Win32")
                    .field("fullFingerprint[donottrack]", "unspecified")
                    .field("fullFingerprint[plugin]", "")
                    .field("fullFingerprint[canvas]", "")
                    .field("hashFingerprint", "2794266614")
                    .field("LOGINID", cUser)
                    .field("PASSWORD", cPwd)
                    .asString();

            // Post
            String sb = stringResponse.getBody();
            logHeaders(stringResponse);

            // Send to https://mail.libero.it/appsuite/api/login?action=liberoLogin
            HttpResponse<String> liberoLogin = getUnirest().get(getLocation(stringResponse)).asString();
            logHeaders(liberoLogin);

            liberoLogin = getUnirest().get("https://mailbasic.libero.it/appsuite/api/login?action=liberoLogin&lightui=true").asString();
            logHeaders(liberoLogin);

            log.info("Libero: primo login per prendere i cookie");
            String randomHome = "http://webmail.libero.it/cp/default.jsp?rndPrx=" + new Random().nextDouble();
            String cLoginUrl = "https://login.libero.it/?service_id=beta_email&ret_url=" + randomHome;
            getPage("https://login.libero.it/");

            // Ora prendo i cookie che mi servono per la sessione
            String cAllCookie = getCookie();
            String cSessionCook = getFullCook(cAllCookie, "Libero");
            cSessionCook += getFullCook(cAllCookie, "cookies_enabled");
            cSessionCook += getFullCook(cAllCookie, "LIB_ADV_G");

            // Con i cookie giusti, faccio il login
            String cStringCaptcha = "";
            String cCaptchaID = "";
            String cMes = "";
            String cLoginPage = "";
            String retUrl = "";

            while (true) {
                log.info("Libero: login reale");
                String cPostString = "SERVICE_ID=beta_email"
                        + "&RET_URL=" + URLEncoder.encode(randomHome, CharsetCoding.UTF_8)
                        + "&LOGINID=" + URLEncoder.encode(cUser, CharsetCoding.UTF_8)
                        + "&PASSWORD=" + URLEncoder.encode(cPwd, CharsetCoding.UTF_8)
                        + "&CAPTCHA_ID=" + URLEncoder.encode(cCaptchaID, CharsetCoding.UTF_8)
                        + "&CAPTCHA_INP=" + URLEncoder.encode(cStringCaptcha, CharsetCoding.UTF_8);

                cLoginPage = postPage("https://login.libero.it/logincheck.php", cSessionCook, cPostString, cLoginUrl).toString();
                cAllCookie = getCookie();
                // Vedo dove mi rimanda
                retUrl = getLocation();

                // Cerca im mess d'errore
                cMes = "";
                int nMesIni = cLoginPage.indexOf("<div id=\"box_err_mess\"");
                if (nMesIni != -1) {
                    nMesIni = cLoginPage.indexOf(">", nMesIni);
                    int nMesEnd = cLoginPage.indexOf("<br>", nMesIni);
                    if (nMesIni != -1 && nMesEnd != -1) {
                        cMes = cLoginPage.substring(nMesIni + 1, nMesEnd).trim();
                    }
                }

                // Cerco Url del CAPTCHA
                String cCaptcha = "";
                int nCapIni = cLoginPage.indexOf("coolcaptcha.php");
                int nCapEnd = cLoginPage.indexOf("\"", nCapIni);
                if (nCapIni != -1 && nCapEnd != -1) {
                    cCaptcha = "https://login.libero.it/" + cLoginPage.substring(nCapIni, nCapEnd);
                    // Non posso più, ora lo randomizzano
                    //cCaptcha = "https://login.libero.it/coolcaptcha.php?captcha_id=" +new Random().nextDouble();
                }

                // Cerco l'ID del CAPTCHA
                cStringCaptcha = "";
                cCaptchaID = getPar(cCaptcha, "captcha_id");

                boolean lCaptcha = false;
                if (retUrl.length() == 0) {
                    // CAPTCHA
                    if (cMes.equalsIgnoreCase("Inserisci le lettere visualizzate nell'immagine.")
                            || cMes.equalsIgnoreCase("Le lettere inserite non corrispondono a quelle visualizzate.")) {
                        cStringCaptcha = getCaptcha(cMes, cCaptcha, cSessionCook);
                        lCaptcha = (cStringCaptcha.length() > 0);
                    }
                }
                if (!lCaptcha) {
                    break;
                }
            }

            // Verifico se c'e' una pagina di stazionamento (p.e. opzionipassword : https://selfcare.libero.it/opzionipassword?url_member=http%3A%2F%2Fwebmail.libero.it%2Fcp%2Fdefault.jsp%3FrndPrx%3D0.7356633663827326)
            String urlMember = getPar(retUrl, "url_member");
            if (urlMember.length() > 0) {
                // Se c'e' la pagina di stazionamento, la salto andando direttamente alla destinazione
                log.info("Libero: salto pagina di stazionamento (" + retUrl + ")");
                retUrl = URLDecoder.decode(urlMember, CharsetCoding.UTF_8);
            }

            // Se vengo rediretto da un'altra parte, non ho fatto il login
            if (!getPar(randomHome, "rndPrx").equalsIgnoreCase(getPar(retUrl, "rndPrx"))) {
                // Altro errore
                if (cMes.length() > 0) {
                    log.info("Libero: errore di login (" + cMes + ")");
                    setLastErr(cMes);
                    // Non riesco a capirlo
                } else {
                    log.info("Libero: errore di login");
                    log.info("Page: " + cLoginPage);
                    log.info("randomHome: " + randomHome);
                    log.info("ret_url: " + retUrl);
                }
            } else {
                // Aggiungi i cookie dati dalla login
                cAllCookie = getCookie();
                cSessionCook += getFullCook(cAllCookie, "LIB_LOG_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_ADV_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_SSO_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_SSO_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_TK_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_MISC_CK");
                cSessionCook += getFullCook(cAllCookie, "LIB_ADV_D");

                // Prendo la location, se c'e' il warning della richiesta segreta lo faccio
                log.info("Libero: entro nella webmail");
                String cLoginCookie = "";
                cLoginCookie += getFullCook(cSessionCook, "Libero");
                cLoginCookie += getFullCook(cSessionCook, "LIB_ADV_G");
                cLoginCookie += getFullCook(cSessionCook, "LIB_ADV_CK");
                cLoginCookie += getFullCook(cSessionCook, "LIB_SSO_CK");
                cLoginCookie += getFullCook(cSessionCook, "LIB_NAME_CK");
                cLoginCookie += getFullCook(cSessionCook, "LIB_TK_CK");
                cLoginCookie += getFullCook(cSessionCook, "LIB_ADV_D");

                // Entro in webmail
                String cPage = getPage(retUrl, cLoginCookie).toString();

                int nFrame1 = cPage.indexOf("id=\"main\" src=\"");
                int nFrame2 = cPage.indexOf("\"", nFrame1 + 15);
                if (nFrame1 != -1 && nFrame2 != -1) {
                    String cGoPage = cPage.substring(nFrame1 + 15, nFrame2);

                    cAllCookie = getCookie();
                    String cWebmailCookie = "";
                    cWebmailCookie += getFullCook(cLoginCookie, "Libero");
                    cWebmailCookie += getFullCook(cLoginCookie, "LIB_ADV_G");
                    cWebmailCookie += getFullCook(cLoginCookie, "LIB_ADV_CK");
                    cWebmailCookie += getFullCook(cLoginCookie, "LIB_SSO_CK");
                    cWebmailCookie += getFullCook(cLoginCookie, "LIB_NAME_CK");
                    cWebmailCookie += getFullCook(cLoginCookie, "LIB_ADV_D");
                    cWebmailCookie += getFullCook(cAllCookie, "s");
                    cWebmailCookie += getFullCook(cAllCookie, "JSESSIONID");

                    cPage = getPage(cGoPage, cWebmailCookie).toString();

                    int nServer = cGoPage.indexOf("/", 7);

                    p = new Properties();
                    p.put("server", cGoPage.substring(0, nServer));
                    p.put("u", getPar(cGoPage, "u"));
                    p.put("t", getPar(cGoPage, "t"));
                    p.put("cookie", cWebmailCookie);
                    p.put("d", cChoice);
                    p.put("folder", cFolder);
                    p.put("read", cRead);
                    p.put("homepage", cGoPage);
                } else {
                    log.info("Libero: errore di login");
                    log.info("Ret url:" + retUrl);
                    log.info("Location: " + getLocation());
                    log.info("Page: " + cPage);
                }
            }

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return (p != null);
    }

    /**
     *
     * @return
     */
    public boolean list() {
        boolean bRet = false;
        try {
            resetEmailInfo(); // Elimina eventuali code di email dovute ai retry
            if (p != null) {
                log.error("Libero: list");
                String cFrame = "/cp/ps/Mail/commands/SyncFolder?d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&t=" + p.getProperty("t");

                String cPost = "accountName=DefaultMailAccount&folderPath=" + p.getProperty("folder") + "&listPosition=1";
                String sb = postPage(p.getProperty("server") + cFrame, p.getProperty("cookie"), cPost).toString();
                int nEmail = estractEmail(sb);
                log.error("Libero: trovate (" + nEmail + ") email");

                while (mailIsUnderStorageLimit()) {
                    int nCurIni = sb.indexOf("currentPage:");
                    int nCurEnd = sb.indexOf(",", nCurIni);
                    int nLastIni = sb.indexOf("lastPage:");
                    int nLastEnd = sb.indexOf(",", nLastIni);
                    int nSizeIni = sb.indexOf("numberToShow:");
                    int nSizeEnd = sb.indexOf(",", nSizeIni);
                    if (nCurIni != -1 && nCurEnd != -1 && nLastIni != -1 && nLastEnd != -1 && nSizeIni != -1 && nSizeEnd != -1) {
                        long nCurPage = Long.parseLong(sb.substring(nCurIni + 12, nCurEnd).trim());
                        long nLastPage = Long.parseLong(sb.substring(nLastIni + 9, nLastEnd).trim());
                        long nPageSize = Long.parseLong(sb.substring(nSizeIni + 13, nSizeEnd).trim());
                        if (nCurPage < nLastPage) {
                            nCurPage++;
                            cPost = "accountName=DefaultMailAccount&folderPath=" + p.getProperty("folder") + "&listPosition=" + ((nPageSize * nCurPage) + 1);
                            sb = postPage(p.getProperty("server") + cFrame, p.getProperty("cookie"), cPost).toString();
                            nEmail = estractEmail(sb);
                            log.error("Libero: trovate (" + nEmail + ") email");
                        } else {
                            break;
                        }
                    } else {
                        log.error("Libero: errore di paginazione (" + sb + ")");
                        break;
                    }
                }

                bRet = true;
            }
        } catch (SocketException se) {
            // Se ho una socket exception rimango in loop e provo il retry
            setLastErr("Errore di comunicazione. Riprovare fra qualche minuto.");
            log.info("Libero: SocketException " + se.getMessage());
        } catch (Exception ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    /**
     *
     * @param nPos
     * @return
     */
    public String getMessageID(int nPos) {
        String cRet = super.getMessageID(nPos);
        int nPosPipe = cRet.indexOf("|");
        return cRet.substring(nPosPipe + 1);
    }

    /**
     *
     * @param nPos
     * @return
     */
    public String getMessageIDTag(int nPos) {
        String cRet = super.getMessageID(nPos);
        int nPosPipe = cRet.indexOf("|");
        return cRet.substring(0, nPosPipe);
    }

    private static boolean bRead = false;

    /**
     *
     * @param b
     */
    public static void setRead(boolean b) {
        bRead = b;
    }

    /**
     *
     * @return
     */
    public static boolean getRead() {
        return bRead;
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
            log.error("Libero: getmail init");

            String cMsgId = getMessageID(nPos);
            String cMsgTag = getMessageIDTag(nPos);

            log.error("Libero: getmail ID (" + cMsgId + ")");

            String cFrame = "/cp/ps/Main/Downloader/" + cMsgId + ".eml?uid=" + cMsgId + "&d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&ai=-1&t=" + p.getProperty("t") + "&c=yes&an=DefaultMailAccount&disposition=attachment&fp=" + p.getProperty("folder") + "&dhid=mailDownloader";
            oMail = getPage(p.getProperty("server") + cFrame, p.getProperty("cookie"), nLine, bAll, "");

            if (!getContentType().equalsIgnoreCase("message/rfc822")) {
                oMail = null;
            } else {
                if ("true".equalsIgnoreCase(p.getProperty("read"))) {
                    log.error("Libero: getmail read flag init");
                    cFrame = "/cp/ps/Mail/commands/LoadMessage?d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&t=" + p.getProperty("t") + "&an=DefaultMailAccount&fp=" + p.getProperty("folder") + "&pid=" + cMsgTag + "&uid=" + cMsgId;
                    getPage(p.getProperty("server") + cFrame, p.getProperty("cookie"), nLine, bAll, "");
                    log.error("Libero: getmail read flag end");
                }
            }

            log.error("Libero: getmail end");
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error", ex);
        }
        return (oMail == null ? null : LineFormat.format(oMail.toString()));
    }

    /**
     *
     * @param SO
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     * @throws Exception
     */
    public boolean streamMessage(OutputStream SO, int nPos, int nLine, boolean bAll) throws Exception {
        boolean bRet = false;
        boolean bEx = false;
        try {
            log.error("Libero: getmailstream init");

            String cMsgId = getMessageID(nPos);
            String cMsgTag = getMessageIDTag(nPos);

            log.error("Libero: getmailstream estrazione " + cMsgId);

            String cFrame = "/cp/ps/Main/Downloader/" + cMsgId + ".eml?uid=" + cMsgId + "&d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&ai=-1&t=" + p.getProperty("t") + "&c=yes&an=DefaultMailAccount&disposition=attachment&fp=" + p.getProperty("folder") + "&dhid=mailDownloader";

            URLConnection con = streamPageTop(p.getProperty("server") + cFrame, p.getProperty("cookie"));

            if (getContentType().equalsIgnoreCase("message/rfc822")) {
                bRet = true;
                HTMLTool html = new HTMLTool();
                if (bAll) { // ALL
                    html.putData(SO, "+OK " + getMessageSize(nPos) + " bytes\r\n");
                } else { // TOP
                    html.putData(SO, "+OK top of message follows\r\n");
                }
                streamPageBody(con, SO, nLine, bAll);
                html.putData(SO, ".\r\n");

                if ("true".equalsIgnoreCase(p.getProperty("read"))) {
                    log.error("Libero: getmailstream read flag init");
                    cFrame = "/cp/ps/Mail/commands/LoadMessage?d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&t=" + p.getProperty("t") + "&an=DefaultMailAccount&fp=" + p.getProperty("folder") + "&pid=" + cMsgTag + "&uid=" + cMsgId;
                    getPage(p.getProperty("server") + cFrame, p.getProperty("cookie"), nLine, bAll, "");
                    log.error("Libero: getmailstream read flag end");
                }

            }
            log.error("Libero: getmailstream end");
        } catch (Throwable ex) {
            bRet = false;
            log.error("Error", ex);
            bEx = true;
        }

        if (bEx) {
            throw new Exception("Libero: Errore durante la lettura della mail. Riprovare il download");
        }
        return bRet;
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
            log.error("Libero: delmessage");

            String cMsgId = getMessageIDTag(nPos);

            log.error("Libero: delmessage " + cMsgId);

            // Preparo I parametri
            String cPage = "/cp/ps/Mail/commands/DeleteMessage?d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&t=" + p.getProperty("t") + "&lsrt=10875";
            String cPost = "selection=" + cMsgId;

            postPage(p.getProperty("server") + cPage, p.getProperty("cookie"), cPost);

            bRet = true;

            log.error("Libero: delmessage end");
        } catch (Throwable ex) {
            bRet = false;
            log.error("Error", ex);
        }
        return bRet;
    }

    @Override
    public void delMessagesFromTrash() throws DeleteMessageException {
        try {
            log.error("Libero: DelMessageFromTrash ini");

            // Preparo I parametri
            String cPage = "/cp/ps/Mail/commands/EmptyFolder?d=" + p.getProperty("d") + "&u=" + p.getProperty("u") + "&t=" + p.getProperty("t") + "&lsrt=11638&an=DefaultMailAccount&fp=trash&recursive=true";

            getPage(p.getProperty("server") + cPage, p.getProperty("cookie"));

            log.error("Libero: DelMessageFromTrash end");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
    }

    private int estractEmail(String sb) {
        int nNumMail = 0;
        // Cerco la lista dei messaggi
        int nPos = 0;
        while (nPos != -1) {
            // ID dell'email
            String cEmailID = "onclick=\"MailList.onClick(event,this)\" id=\"";
            nPos = sb.indexOf(cEmailID, nPos);
            if (nPos == -1) {
                break;
            }
            int nPosEnd = sb.indexOf("\"", nPos + cEmailID.length());
            if (nPosEnd == -1) {
                break;
            }
            String cMail = sb.substring(nPos + cEmailID.length(), nPosEnd);

            cEmailID = " uid=\"";
            nPos = sb.indexOf(cEmailID, nPosEnd);
            if (nPos == -1) {
                break;
            }
            nPosEnd = sb.indexOf("\"", nPos + cEmailID.length());
            if (nPosEnd == -1) {
                break;
            }
            cMail += "|" + sb.substring(nPos + cEmailID.length(), nPosEnd);

            // Size non più calcolabile
            int nLen = 1;
            /*
             String cSiz = "";
             try {                    //012345678901234567890123456789
             int nSiz = sb.indexOf("<td class=\"last\">", nPosEnd);
             if (nSiz != -1) {
             int nSizEnd = sb.indexOf("\r", nSiz);
             if (nSizEnd != -1) {
             cSiz = sb.substring(nSiz + 17, nSizEnd);
             if (cSiz.endsWith("MB")) {
             nLen = Double.valueOf(replace(cSiz, "MB", "").trim()).intValue() * 1024 * 1024;
             } else if (cSiz.endsWith("KB")) {
             nLen = Double.valueOf(replace(cSiz, "KB", "").trim()).intValue() * 1024;
             } else if (cSiz.endsWith("B")) {
             nLen = Double.valueOf(replace(cSiz, "B", "").trim()).intValue();
             }
             }
             }
             } catch (Throwable ex) {
             log.error("Libero: errore di calcolo size: [" + cSiz + "] segnalare all'autore di html2pop3");
             }
             */

            // Lascio andare in errore in caso di non trovato, cosi qualcuno me lo segnala
            if (addEmailInfo(cMail, nLen)) {
                nNumMail++;
            }

            nPos = nPosEnd;
        }
        return nNumMail;
    }

    private String getCaptcha(String cMsg, String cCaptcha, String cSessionCook) throws Throwable {
        log.error("Libero: getCaptcha ini");
        byte[] imgb = getPageBytes(cCaptcha, cSessionCook);

        final JDialog frame = new JDialog(null, Dialog.ModalityType.APPLICATION_MODAL);
        frame.setLayout(new FlowLayout());
        frame.setSize(400, 200);

        JLabel e = new JLabel(cMsg);
        frame.add(e);

        // Non va via proxy
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgb));
        ImageIcon icon = new ImageIcon(img);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);

        JLabel l = new JLabel("Scrivi il CAPTCHA");
        frame.add(l);

        JTextField function = new JTextField(8);
        function.addActionListener((ActionEvent e1) -> {
            frame.dispose();
        });
        frame.add(function);

        JButton button = new JButton("Conferma");
        button.addActionListener((ActionEvent e1) -> {
            frame.dispose();
        });
        frame.add(button);

        frame.setLocationRelativeTo(null);

        // Dopo 1 minuto chiudo il frame, in caso di mancato input
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                //log.error("Libero: getCaptcha autoClose");
                frame.dispose();
            }
        }, 1 * 60 * 1000);

        frame.setVisible(true);
        log.error("Libero: getCaptcha end");

        return function.getText();
    }

    /**
     *
     * @return
     */
    @Override
    public ArrayList<String[]> getContact() {
        ArrayList<String[]> oRet = new ArrayList<>();
        /*
         try {
         // Preparo I parametri
         String cPost = "ID=" + cID + "&Act_AB_Export=1&HELP_ID=addressbook&RecipNb=0&DIRECT=1&Act_Role=0&Facility=0&AB_Export_Type=Export_Csv&AB_PATTERN=&exp=";

         String sb = postPage(cServer, cSessionCook, cPost).toString();
         int nUrl = sb.indexOf("\"/xam_exch/");
         int nUrl2 = sb.indexOf("\"", nUrl + 1);
         if (nUrl != -1 && nUrl2 != -1) {
         String cUrl = sb.substring(nUrl + 1, nUrl2);
         String cPage = getPage(cServer + cUrl).toString();
         //log.error( cPage );

         BufferedReader br = new BufferedReader(new StringReader(cPage));
         String cLine = "";
         int nLine = 0;
         while ((cLine = br.readLine()) != null) {
         nLine++;
         if (nLine > 1) {
         cLine = replace(cLine, ",", ", ");

         String[] oEle = {"", ""};
         int nTok = 0;
         StringTokenizer st = new StringTokenizer(cLine, ",");
         while (st.hasMoreTokens()) {
         String cTok = st.nextToken().trim();
         nTok++;
         //log.error( "*"+cTok+"*" );
         if (nTok == 5) {
         oEle[0] = cTok;
         } else if (nTok == 6) {
         oEle[1] = cTok;
         }
         }
         oRet.addElement(oEle);

         }
         }
         }

         } catch (Throwable ex) {
         log.error("Error",ex);
         }
         //*/
        return oRet;
    }

}
