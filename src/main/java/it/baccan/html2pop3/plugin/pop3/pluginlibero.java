/*
 * Libero plugin
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
 * Title: Libero HTML2POP3 Description: Convertitore da HTML a POP3 per libero.it Copyright: Copyright (c) 2003 Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.Timer;
import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.utils.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class pluginlibero extends POP3Base implements POP3Plugin {

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

    /**
     *
     * @param n
     */
    public pluginlibero(int n) {
        nMail = n;
    }

    // Proprietà di sessione
    private Properties p;

    // Elenco di tutte le sessioni attive
    private static final Properties sessions = new Properties();

    /**
     *
     * @param cUserParam
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUserParam, String cPwd) {
        boolean bRet = false;

        if ("1.6".equalsIgnoreCase(System.getProperty("java.specification.version"))) {
            log.info("Libero: versione 1.6 non compatibile con Libero.it");
            //new MsgBox("JAVA 1.6", "Stai usando JAVA 1.6.\nPer poter controllare la posta di LIBERO devi usare almeno JAVA 1.7.0_21", false);
        } else {

            // Provo a prendere la sessione dalla cache
            String key = "" + nMail + ":" + cUserParam + ":" + cPwd;
            p = (Properties) sessions.get(key);
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
                bRet = subLogin(cUserParam, cPwd);
            }

            // Se è valida, la memorizzo/sovrascrivo
            if (bRet) {
                sessions.put(key, p);
            }
        }
        // Restituisco il valore della login
        return bRet;
    }

    private boolean subLogin(String cUserParam, String cPwd) {
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

        String cFolder = "";
        String cRead = "";
        int nQuestionMark = cUserParam.indexOf("?");
        if (nQuestionMark != -1) {
            cFolder = URLEncoder.encode(getPar(cUserParam.substring(nQuestionMark), "folder", "inbox"));
            cRead = URLEncoder.encode(getPar(cUserParam.substring(nQuestionMark), "read", (getRead() ? "true" : "false")));
            cUserParam = cUserParam.substring(0, nQuestionMark);
        } else {
            cFolder = "inbox";
            cRead = (getRead() ? "true" : "false");
        }

        String cUser = cUserParam;
        if (!cUser.contains("@")) {
            cUser += "@" + cChoice;
        }

        // Login
        //System.setProperty("socksProxyHost", "127.0.0.1");
        //System.setProperty("socksProxyPort", "9050");
        try {
            log.info("Libero: primo login per prendere i cookie");
            //String randomHome = "http://mailbeta.libero.it/cp/WindMailPS.jsp?rndPrx=" + new Random().nextDouble(); //0.3217822669592484";
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
            String ret_url = "";

            while (true) {
                log.info("Libero: login reale");
                String cPostString = "SERVICE_ID=beta_email"
                        + "&RET_URL=" + URLEncoder.encode(randomHome)
                        + "&LOGINID=" + URLEncoder.encode(cUser)
                        + "&PASSWORD=" + URLEncoder.encode(cPwd)
                        + "&CAPTCHA_ID=" + URLEncoder.encode(cCaptchaID)
                        + "&CAPTCHA_INP=" + URLEncoder.encode(cStringCaptcha);

                cLoginPage = postPage("https://login.libero.it/logincheck.php", cSessionCook, cPostString, cLoginUrl).toString();
                cAllCookie = getCookie();
                // Vedo dove mi rimanda
                ret_url = getLocation();

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
                if (ret_url.length() == 0) {
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
            String url_member = getPar(ret_url, "url_member");
            if (url_member.length() > 0) {
                // Se c'e' la pagina di stazionamento, la salto andando direttamente alla destinazione
                log.info("Libero: salto pagina di stazionamento (" + ret_url + ")");
                ret_url = URLDecoder.decode(url_member);
            }

            // Se vengo rediretto da un'altra parte, non ho fatto il login
            if (!getPar(randomHome, "rndPrx").equalsIgnoreCase(getPar(ret_url, "rndPrx"))) {
                // Altro errore
                if (cMes.length() > 0) {
                    log.info("Libero: errore di login (" + cMes + ")");
                    setLastErr(cMes);
                    // Non riesco a capirlo
                } else {
                    log.info("Libero: errore di login");
                    log.info("Page: " + cLoginPage);
                    log.info("randomHome: " + randomHome);
                    log.info("ret_url: " + ret_url);
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
                String cPage = getPage(ret_url, cLoginCookie).toString();

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
                    log.info("Ret url:" + ret_url);
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
        } catch (Throwable ex) {
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
    static public void setRead(boolean b) {
        bRead = b;
    }

    /**
     *
     * @return
     */
    static public boolean getRead() {
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
        //BufferedImage img = ImageIO.read(new URL(cCaptcha));
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgb));
        ImageIcon icon = new ImageIcon(img);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);

        JLabel l = new JLabel("Scrivi il CAPTCHA");
        frame.add(l);

        JTextField function = new JTextField(8);
        function.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        frame.add(function);

        JButton button = new JButton("Conferma");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
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

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        pluginlibero libero = new pluginlibero(MAIL_LIBERO);
        if (libero.login(args[0], args[1])) {
            /*
             libero.list();
             int nNum = libero.getMessageNum();
             int nSiz = libero.getMessageSize();
             log.info( "getMessageNum  :" +nNum );
             log.info( "getMessageSize :" +nSiz );
             for( int nPos=1; nPos<=nNum; nPos++ ){
             log.info( "getMessageID   (" +nPos +"):" +libero.getMessageID(nPos) );
             log.info( "getMessageSize (" +nPos +"):" +libero.getMessageSize(nPos) );
             log.info( "getMessage     (" +nPos +"):" +libero.getMessage(nPos) );
             }
             */
            log.info(libero.getContactXML());
        }
    }
}
