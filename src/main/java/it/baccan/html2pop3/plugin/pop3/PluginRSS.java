/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        RSS HTML2POP3
 * Description:  Convertitore da RSS a POP3
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.io.*;
import java.util.*;

import it.baccan.html2pop3.utils.message.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginRSS extends POP3Base implements POP3Plugin {

    private static final Properties config = new Properties();
    private static String cCacheFile = "";
    private static final Properties cache = new Properties();
    private final Properties p = new Properties();
    private final Properties pXML = new Properties();
    private String cUser = "";

    /**
     *
     * @param cPath
     * @param cConfig
     */
    public static void setConfig(String cPath, String cConfig) {
        try {
            try (FileInputStream fis = new FileInputStream(cPath + cConfig)) {
                config.load(fis);
            }

        } catch (FileNotFoundException fnf) {

            log.info("Non riesco a leggere il file " + cPath + cConfig);

        } catch (IOException e) {
            log.error("Error", e);
        }

        cCacheFile = cPath + cConfig + "-cache.cache";
        try {
            try (FileInputStream fis = new FileInputStream(cCacheFile)) {
                cache.load(fis);
            }
        } catch (FileNotFoundException fnf) {
            // Empty cache
        } catch (IOException e) {
            log.error("Error", e);
        }
    }

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
            log.error("Rss: login init");

            this.cUser = cUser;
            cPwd = config.getProperty(cPwd, cPwd);

            List<String> aRss = new ArrayList<>(10);
            if (cPwd.equalsIgnoreCase("all")) {
                Enumeration keysEnum = config.keys();
                while (keysEnum.hasMoreElements()) {
                    aRss.add(config.getProperty((String) keysEnum.nextElement()));
                }
            } else {
                aRss.add(cPwd);
            }

            for (int nRss = 0; nRss < aRss.size(); nRss++) {
                String cUrl = (String) aRss.get(nRss);

                log.error("Rss: " + cUrl);

                try {
                    java.util.zip.CRC32 crcCache = new java.util.zip.CRC32();
                    crcCache.update((cUser + cUrl).getBytes(), 0, (cUser + cUrl).length());
                    String cCrcCache = "" + crcCache.getValue();
                    String cCache = cache.getProperty(cCrcCache, "");

                    String sb = getPage(cUrl).toString();
                    String cCRCList = "";

                    String cEnc = "ISO-8859-1";
                    int nEncI = sb.indexOf("encoding=\"");
                    int nEncE = sb.indexOf("\"", nEncI + 10);
                    if (nEncI != -1 && nEncE != -1) {
                        cEnc = sb.substring(nEncI + 10, nEncE);
                    }
                    log.error("Rss: Encoding (" + cEnc + ")");

                    int nPos = 0;
                    while (true) {
                        int nIni = sb.indexOf("<item>", nPos);
                        if (nIni == -1) {
                            nIni = sb.indexOf("<item ", nPos);
                        }
                        if (nIni == -1) {
                            break;
                        }
                        int nEnd = sb.indexOf("</item>", nIni);
                        if (nEnd == -1) {
                            break;
                        }

                        String cRss = sb.substring(nIni, nEnd + 7);

                        //cRss  = replace( cRss, "&amp;", "&" );
                        String cTit = toHTML(getSubStr(cRss, "title"), cEnc);
                        String cLink = getSubStr(cRss, "link");
                        String cDesc = toHTML(getSubStr(cRss, "description"), cEnc);
                        String cCont = toHTML(getSubStr(cRss, "content:encoded"), cEnc);

                        String cEmail = "";
                        cEmail += "<a href=\"" + cLink + "\">(*)</a><b> " + cTit + " </b>\r\n";
                        if (cDesc.length() > 0) {
                            cEmail += "<br>" + cDesc + "\r\n";
                        }
                        cEmail += "<br>URL: " + cLink + "\r\n";
                        if (cCont.length() > 0) {
                            cEmail += "<br> " + cCont + "\r\n";
                        }

                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        if (cLink.length() > 0) {
                            crc.update(cLink.getBytes(), 0, cLink.length());
                        } else {
                            crc.update(cTit.getBytes(), 0, cTit.length());
                        }
                        String cCrc = "" + crc.getValue();

                        cCRCList += "-" + cCrc + "-";

                        // Se non l'ho letto nella sessione precedente
                        if (!cCache.contains("-" + cCrc + "-")) {
                            p.put(cCrc, cEmail);
                            pXML.put(cCrc, cRss);
                            pXML.put(cCrc + "enc", cEnc);
                            addEmailInfo(cCrc, cEmail.length());
                        }

                        nPos = nEnd;
                    }

                    cache.put(cCrcCache, cCRCList);
                } catch (Throwable ex) {
                    log.error("Error", ex);
                }
            }
            bRet = true;

            log.error("Rss: login end");

            try {
                cache.store(new FileOutputStream(cCacheFile), null);
            } catch (FileNotFoundException e) {
                log.error("FileNotFoundException" ,e);
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
    @Override
    public String getMessage(int nPos, int nLine, boolean bAll) {
        StringBuffer oMail = new StringBuffer();
        try {
            log.error("Rss: getmail init");

            String cMsgId = getMessageID(nPos);

            log.error("Rss: getmail ID (" + cMsgId + ")");

            String cEnc = pXML.getProperty(cMsgId + "enc");
            POP3Message pop3 = new POP3Message();
            pop3.setCharset(cEnc);
            pop3.setDa("HTML2POP3 RSS");
            pop3.setA(cUser);
            pop3.setOggetto("=?" + cEnc + "?B?" + Base64.getEncoder().encodeToString(toHTML(getSubStr(pXML.getProperty(cMsgId), "title"), cEnc).getBytes()) + "?=");
            String cData = getSubStr(pXML.getProperty(cMsgId), "pubDate");
            pop3.setData(cData.length() == 0 ? getCurDate() : cData);
            pop3.setBody(p.getProperty(cMsgId));
            pop3.addHTMLAttach("source.xml", pXML.getProperty(cMsgId).getBytes());
            //pop3.addHTMLAttach( "source.htm", getPage( getSubStr( pXML.getProperty( cMsgId ), "link" ) ).toString().getBytes() );

            oMail.append(pop3.getMessage(nLine, bAll));

            log.error("Rss: getmail end");
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
        boolean bRet = true;
        try {
            log.error("Rss: delete not implemented");
        } catch (Throwable ex) {
            log.error("Error", ex);
        }
        return bRet;
    }

    private String toHTML(String cRss, String cEnc) {
        if (cRss.startsWith("<![CDATA[") && cRss.endsWith("]]>")) {
            cRss = cRss.substring(9, cRss.length() - 3);
        }

        // &amp; -> & XML conversion
        cRss = replace(cRss, "&amp;", "&");

        if (cEnc.equalsIgnoreCase("ISO-8859-1")) {
            cRss = replace(cRss, "" + 0xe0, "&agrave;");
            cRss = replace(cRss, "" + 0xc0, "&Agrave;");
            cRss = replace(cRss, "" + 0xec, "&igrave;");
            cRss = replace(cRss, "" + 0xcc, "&Igrave;");
            cRss = replace(cRss, "" + 0xe8, "&egrave;");
            cRss = replace(cRss, "" + 0xc8, "&Egrave;");
            cRss = replace(cRss, "" + 0xf2, "&ograve;");
            cRss = replace(cRss, "" + 0xd2, "&Ograve;");
            cRss = replace(cRss, "" + 0xf9, "&ugrave;");
            cRss = replace(cRss, "" + 0xd9, "&Ugrave;");
        }
        // Serve per far funzionare i tag HTML
        cRss = replace(cRss, "&gt;", ">");
        cRss = replace(cRss, "&lt;", "<");
        cRss = replace(cRss, "&quot;", "\"");
        return cRss;
    }

    private String getSubStr(String cRss, String cItem) {
        String cRet = "";
        int nIni = cRss.indexOf("<" + cItem + ">");
        int nIniEnd = -1;

        if (nIni == -1) {
            nIni = cRss.indexOf("<" + cItem + " ");
            if (nIni != -1) {
                nIniEnd = cRss.indexOf(">", nIni);
                if (nIniEnd == -1) {
                    nIni = -1;
                }
            }
        } else {
            nIniEnd = nIni + cItem.length() + 2;
        }

        if (nIni != -1) {
            int nEnd = cRss.indexOf("</" + cItem + ">", nIniEnd);
            if (nEnd != -1) {
                cRet = cRss.substring(nIniEnd, nEnd);
            }
        }
        return cRet;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginRSS infinito = new PluginRSS();
        if (infinito.login(args[0], args[1])) {
            int nNum = infinito.getMessageNum();
            int nSiz = infinito.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + infinito.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + infinito.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + infinito.getMessage(nPos));
            }
        }
    }

}
