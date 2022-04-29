/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        Libero HTML2POP3
 * Description:  Crea una mail POP3
 * Copyright:    Copyright (c) 2003
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.utils.message;

import it.baccan.html2pop3.utils.CharsetCoding;
import it.baccan.html2pop3.utils.ContentType;
import it.baccan.html2pop3.utils.LineFormat;
import it.baccan.html2pop3.utils.Version;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class POP3Message extends MasterMessage {

    /**
     *
     */
    public static final int TEXT_MESSAGE = 0;

    /**
     *
     */
    public static final int HTML_MESSAGE = 1;
    private int messageType = HTML_MESSAGE;

    @Getter
    @Setter
    private String da = "";
    @Getter
    @Setter
    private String a = "";
    @Getter
    @Setter
    private String cc = "";
    @Getter
    @Setter
    private String oggetto = "";
    @Getter
    @Setter
    private String data = "";
    @Getter
    @Setter
    private String notifica = "";

    @Getter
    @Setter
    private String body = "";

    private final List<byte[]> aAttach = new ArrayList<>(0);
    private final List<String> aName = new ArrayList<>(0);

    /**
     *
     */
    public static boolean addHTML = false;
    @Getter
    @Setter
    private String charset = CharsetCoding.UTF_8;

    private static boolean bRFC2047 = true;
    private final String cHEX = "0123456789abcdef";

    /**
     *
     * @param cName
     * @param cAttach
     */
    @Override
    public void addAttach(String cName, byte[] cAttach) {
        this.aName.add(cName);
        this.aAttach.add(cAttach);
    }

    /**
     *
     * @param cName
     * @param cAttach
     */
    public void addHTMLAttach(String cName, byte[] cAttach) {
        if (addHTML) {
            addAttach(cName, cAttach);
        }
    }

    /**
     *
     * @param b
     */
    public static void setAddHTML(boolean b) {
        addHTML = b;
    }

    /**
     *
     * @return
     */
    public static boolean getAddHTML() {
        return addHTML;
    }

    /**
     *
     * @return
     */
    @Override
    public String getMessage() {
        return getMessage(0, false);
    }

    /**
     * Sets if the message is in HTML or TEXT form the default is HTML.
     *
     * @param i the format of the message TEXT_MESSAGE/HTML_MESSAGE
     */
    public void setMessageType(int i) {
        messageType = i;
    }

    /**
     *
     * @param nLine
     * @param bAll
     * @return
     */
    @Override
    public String getMessage(int nLine, boolean bAll) {
        StringBuilder oMail = new StringBuilder();
        StringBuilder oMailBody = new StringBuilder();

        try {
            String cCharset = "";
            if (messageType == HTML_MESSAGE) {
                cCharset = "Content-Type: text/html; charset=\"" + getCharset() + "\"\r\n";
            } else {
                cCharset = "Content-Type: text/plain; charset=" + getCharset() + "\r\n";
            }

            if (da.length() > 0) {
                oMail.append("From: ").append(da).append("\r\n");
            }
            if (a.length() > 0) {
                oMail.append("To: ").append(a).append("\r\n");
            }
            if (cc.length() > 0) {
                oMail.append("Cc: ").append(cc).append("\r\n");
            }
            if (oggetto.length() > 0) {
                oMail.append("Subject: ").append(rfc2047(oggetto)).append("\r\n");
            }
            if (data.length() > 0) {
                oMail.append("Date: ").append(data).append("\r\n");
            }

            oMail.append("User-Agent: HTML2POP3 ").append(Version.getVersion()).append("\r\n");
            oMail.append("X-Mailer: HTML2POP3 ").append(Version.getVersion()).append("\r\n");

            // Non e' alternative ma mixed
            oMail.append("Content-Type: multipart/mixed;\r\n");
            oMail.append("  boundary=\"______BoundaryOfDocument______\";\r\n");
            oMail.append("  charset=\"").append(getCharset()).append("\"\r\n");
            oMail.append("Mime-Version: 1.0\r\n");
            if (data.length() > 0) {
                oMail.append("Received: by localhost with HTTP; ").append(data).append("\r\n");
            }
            if (notifica.length() > 0) {
                oMail.append("Return-Path: ").append(notifica).append("\r\n");   // (OutlookExpress)
                oMail.append("Return-Receipt-To: ").append(notifica).append("\r\n");   // (OfficeOutllok)
                oMail.append("Disposition-Notification-To: ").append(notifica).append("\r\n");   // (OutlookExpress) http://rfc.sunsite.dk/rfc/rfc3798.html
            }
            oMail.append("\r\n");
            oMail.append("--______BoundaryOfDocument______\r\n");

            oMail.append(cCharset);
            oMail.append("Content-Transfer-Encoding: quoted-printable\r\n");
            oMail.append("\r\n");
            /*
            if (messageType == HTML_MESSAGE) {
                oMailBody.append("<HTML>\r\n");
                oMailBody.append("<HEAD>\r\n");
                // SPACE before </title> prevente UTF-8 error in wrong email
                oMailBody.append("<TITLE>").append(oggetto).append(" </TITLE>\r\n");
                oMailBody.append("<META http-equiv=Content-Type content=\"text/html; charset=").append(getCharset()).append("\">\r\n");
                oMailBody.append("</HEAD>\r\n");
                oMailBody.append("<BODY bgcolor=#ffffff topmargin=10 marginheight=10 leftmargin=10 marginwidth=10>\r\n");
            }*/

            // Il body deve avere le seguenti modifiche
            // riga per riga .. se parte con . aggiugnerne uno
            //body = string.replace( body, "\n.", "\n.." );
            //if( body.startsWith(".") && !body.startsWith("..") )
            //body = "." +body;
            body = LineFormat.format(body);

            oMailBody.append(body);
            //if (messageType == HTML_MESSAGE) {
            //    oMailBody.append("</BODY>\r\n");
            //    oMailBody.append("</HTML>\r\n");
            //} else {
            //    oMailBody.append("\r\n");
            //}

            // Appendo il base64 del body
            StringBuilder newBody = new StringBuilder();
            splitAndAttach7bit(newBody, oMailBody);
            oMailBody = newBody;
            oMailBody.append("\r\n");

            for (int nAttach = 0; nAttach < aName.size(); nAttach++) {
                String filename = (String) aName.get(nAttach);

                oMailBody.append("--______BoundaryOfDocument______\r\n");
                oMailBody.append("Content-Type: ").append(ContentType.getInstance().getFromFilename(filename)).append(";\r\n");
                oMailBody.append("      name=\"").append(filename).append("\"\r\n");
                oMailBody.append("Content-Transfer-Encoding: base64\r\n");
                oMailBody.append("Content-Disposition: attachment;\r\n");
                oMailBody.append("      filename=\"").append(filename).append("\"\r\n");
                oMailBody.append("\r\n");

                splitAndAttachBinary(oMailBody, aAttach.get(nAttach));

                oMailBody.append("\r\n");
            }

            oMailBody.append("--______BoundaryOfDocument______--\r\n");
            oMailBody.append("\r\n");

            if (bAll) {
                oMail.append(oMailBody);
            } else {
                try (StringReader sr = new StringReader(oMailBody.toString())) {
                    BufferedReader br = new BufferedReader(sr);
                    String cLine = null;
                    while (nLine > 0 && (cLine = br.readLine()) != null) {
                        oMail.append(cLine);
                        oMail.append("\r\n");
                        nLine--;
                    }
                }
            }

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return oMail.toString();
    }

    /**
     *
     * @param b
     */
    public static void setrfc2047(boolean b) {
        bRFC2047 = b;
    }

    /**
     *
     * @return
     */
    public static boolean getrfc2047() {
        return bRFC2047;
    }

    /**
     *
     * @param cStr
     * @return
     */
    public String rfc2047(String cStr) {
        cStr = cStr.replace("&#39;", "'");

        String cRet = cStr;
        // Verifica se ci sono caratteri strani e serve una codifica secondo rfc2047
        boolean bEncode = false;
        String cEncode = "";
        for (int n = 0; n < cStr.length(); n++) {
            // Singolo carattere
            char c = cStr.charAt(n);
            // Se fuori dal range ... faccio l'encoding
            if (!((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == ' '
                    || c == '>'
                    || c == '<'
                    || c == '"'
                    || c == '\''
                    || c == '_'
                    || c == '-'
                    || c == '.'
                    || c == '@')) {
                bEncode = true;
                cEncode += "=" + cHEX.charAt((c & 240) >> 4) + cHEX.charAt(c & 15);
            } else {
                cEncode += "" + c;
            }

        }

        // Encoding
        if (bEncode && bRFC2047) {
            cRet = "=?" + getCharset() + "?" + "B" + "?" + Base64.getEncoder().encodeToString(cStr.getBytes(StandardCharsets.UTF_8)) + "?=";
        }
        return cRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        POP3Message pop3 = new POP3Message();
        pop3.setDa("cFrom");
        pop3.setA("cFrom");
        pop3.setCc("cFrom");
        pop3.setOggetto("objèòà");
        pop3.setData("");
        pop3.setBody("");
        log.info(pop3.getMessage());
    }

    private void splitAndAttachBinary(StringBuilder oMailBody, byte[] cAttach) {
        for (int nBlock = 0; nBlock < cAttach.length; nBlock += 60) {
            int nLen = 60;
            if (nBlock + nLen >= cAttach.length) {
                nLen = cAttach.length - nBlock;
            }
            byte[] buf = new byte[nLen];
            for (int n = 0; n < nLen; n++) {
                buf[n] = cAttach[nBlock + n];
            }
            oMailBody.append(Base64.getEncoder().encodeToString(buf)).append("\r\n");
        }
    }

    private void splitAndAttach7bit(StringBuilder oMailBody, StringBuilder cAttach) {
        // =3D
        int nBlock = 0;
        int line = 0;
        while (nBlock < cAttach.length()) {
            line++;
            char c = cAttach.charAt(nBlock);

            // Carattere = con forzatura
            if (c == 0x3d) {
                oMailBody.append("=3d");
                line += 2;
                // Caratteri usabili direttamente    
            } else if (((c >= 0x20 && c <= 0x7a) || c == 0x0d || c == 0x0a)) {
                oMailBody.append(c);
                // Caratteri unicode non standard    
            } else {
                String add = "&#" + (int) c + ";";
                oMailBody.append(add);
                line += add.length() - 1;
            }

            // Nuova riga
            if (c == '\n') {
                line = 0;
                // Nuova riga    
            } else if (line >= 73) {
                oMailBody.append("=\r\n");
                line = 0;
            }
            nBlock++;
        }
    }

}
