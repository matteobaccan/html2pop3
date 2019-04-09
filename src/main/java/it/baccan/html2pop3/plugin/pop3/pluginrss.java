/*
 * RSS plugin
 *
 * Copyright 2004 Matteo Baccan
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
public class pluginrss extends pop3base implements pop3plugin {

    /**
     *
     */
    public pluginrss() {
    }

    private static Properties config = new Properties();
    private static String cCacheFile = "";
    private static Properties cache = new Properties();

    /**
     *
     * @param cPath
     * @param cConfig
     */
    public static void setConfig(String cPath, String cConfig) {
        try {
            FileInputStream fis = new FileInputStream(cPath + cConfig);
            config.load(fis);
            fis.close();

        } catch (java.io.FileNotFoundException fnf) {

            log.info("Non riesco a leggere il file " + cPath + cConfig);

        } catch (IOException e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }

        cCacheFile = cPath + cConfig + "-cache.cache";
        try {
            FileInputStream fis = new FileInputStream(cCacheFile);
            cache.load(fis);
            fis.close();
        } catch (java.io.FileNotFoundException fnf) {
            // Empty cache
        } catch (IOException e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

    private Properties p = new Properties();
    private Properties pXML = new Properties();
    private String cUser = "";

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        boolean bRet = false;
        try {
            log.error("Rss: login init");

            this.cUser = cUser;
            cPwd = config.getProperty(cPwd, cPwd);

            Vector aRss = new Vector();
            if (cPwd.equalsIgnoreCase("all")) {
                Enumeration keysEnum = config.keys();
                while (keysEnum.hasMoreElements()) {
                    aRss.addElement(config.getProperty((String) keysEnum.nextElement()));
                }
            } else {
                aRss.addElement(cPwd);
            }

            for (int nRss = 0; nRss < aRss.size(); nRss++) {
                String cUrl = (String) aRss.elementAt(nRss);

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
                        if (cCache.indexOf("-" + cCrc + "-") == -1) {
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
                cache.save(new FileOutputStream(cCacheFile), null);
            } catch (Throwable e) {
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
            pop3.setOggetto("=?" + cEnc + "?B?" + new String(it.baccan.html2pop3.utils.Base64.encode(toHTML(getSubStr(pXML.getProperty(cMsgId), "title"), cEnc).getBytes())) + "?=");
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
        pluginrss infinito = new pluginrss();
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
