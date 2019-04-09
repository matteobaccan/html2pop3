/*
 * HTTP/NNTP generic plugin
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
 * Title:        HTML2POP3 NNTP2HTML
 * Description:  Convertitore da HTML a NNTP
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.nntp;

import java.net.*;
import java.io.*;
import java.util.*;

import it.baccan.html2pop3.utils.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class pluginnntp extends nntpbase implements nntpplugin {

    /**
     *
     */
    public pluginnntp() {
    }

    private static int nNumMsg = 25;
    private static String cConfigPath = "";
    private static String cConfig = "";

    /**
     *
     * @param cPath
     * @param cC
     */
    public static void setConfig(String cPath, String cC) {
        cConfigPath = cPath;
        cConfig = cC;

        // Se non ho la directory di LOG la creo
        try {
            java.io.File oFile = new java.io.File(cPath + "nntpcache");
            if (!oFile.exists()) {
                oFile.mkdir();
            }
        } catch (Throwable e) {
        }
    }

    /**
     *
     * @param SO
     * @return
     */
    public boolean streamList(OutputStream SO) {
        boolean bRet = false;
        //Properties config = new Properties();
        try {
            FileInputStream fis = new FileInputStream(cConfigPath + cConfig);
            BufferedInputStream bis = new BufferedInputStream(fis);
            InputStreamReader isr = new InputStreamReader(bis);
            BufferedReader br = new BufferedReader(isr);

            String cLine;
            while ((cLine = br.readLine()) != null) {
                if (!cLine.startsWith("#")) {
                    if (cLine.endsWith("\r\n")) {
                        cLine = cLine.substring(0, cLine.length() - 2);
                    }
                    htmlTool html = new htmlTool();
                    html.putData(SO, cLine + " 00001 00001 n\r\n");
                }
            }

            br.close();
            isr.close();
            bis.close();
            fis.close();
            bRet = true;
        } catch (java.io.FileNotFoundException fnf) {

            log.info("Non riesco a leggere il file " + cConfigPath + cConfig);

        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }

        //Vector aGroup = new Vector();
        //Enumeration keysEnum = config.keys();
        //while(keysEnum.hasMoreElements()){
        //aGroup.addElement( (String)keysEnum.nextElement() );
        //}
        //return aGroup;
        return bRet;
    }

    private Vector aOver = new Vector();
    private String cCurrentGroup = "";

    /**
     *
     * @param cGroup
     * @return
     */
    public long[] group(String cGroup) {
        long[] nRet = {0, 0};
        cCurrentGroup = cGroup;
        aOver = new Vector();

        log.error("nntp: group init");

        // Se non ho la directory di LOG la creo
        try {
            java.io.File oFile = new java.io.File(cConfigPath + "nntpcache" + File.separator + cCurrentGroup);
            if (!oFile.exists()) {
                oFile.mkdir();
            }
        } catch (Throwable e) {
        }

        try {
            String cPage = getPage("http://groups.google.it/groups?hl=it&lr=&ie=UTF-8&num=" + nNumMsg + "&group=" + cGroup).toString();
            Vector aSelm = getSelm(cPage, true);

            long nPosMin = 0;
            long nPosMax = 1;
            for (int nPos = 0; nPos < aSelm.size(); nPos++) {
                String cID = (String) aSelm.elementAt(nPos);
                String cArt = getArticle(cID);
                if (cArt != null) {
                    // NUM<tab>subject<tab>author<tab>date<tab>message-id<tab>references<tab>byte count<tab>line count:NUM
                    String cNID = lastNum;

                    // Calcolo minimo
                    long nNum = Double.valueOf(cNID).longValue();
                    if (nNum < nPosMin || nPosMin == 0) {
                        nPosMin = nNum;
                    }
                    // e il massimo
                    if (nNum > nPosMax) {
                        nPosMax = nNum;
                    }

                    String cSub = getRow(cArt, "Subject:");
                    String cAut = getRow(cArt, "From:");
                    String cDat = getRow(cArt, "Date:");
                    String cMID = getRow(cArt, "Message-ID:");
                    String cRef = getRow(cArt, "References:");
                    String cByt = "" + cArt.length();

                    byte[] b = cArt.getBytes();
                    int nRow = 0;
                    for (int nC = 0; nC < b.length; nC++) {
                        if (b[nC] == '\n') {
                            nRow++;
                        }
                    }
                    String cLin = "" + nRow;

                    aOver.addElement(cNID + "\t" + cSub + "\t" + cAut + "\t" + cDat + "\t" + cMID + "\t" + cRef + "\t" + cByt + "\t" + cLin + ":" + cNID);
                }
            }
            nRet[1] = nPosMax - 1;
            nRet[0] = nPosMin;

            // Sort
            boolean bSort;
            do {
                bSort = true;
                for (int nPos = 0; nPos < aOver.size() - 1; nPos++) {
                    Object a = aOver.elementAt(nPos);
                    Object b = aOver.elementAt(nPos + 1);
                    if (((String) a).compareTo((String) b) > 0) {
                        aOver.setElementAt(a, nPos + 1);
                        aOver.setElementAt(b, nPos);
                        bSort = false;
                    }
                }
            } while (!bSort);

        } catch (Throwable ex) {
            log.info("group", ex);
        }
        log.error("nntp: group end");
        return nRet;
    }

    private Vector getSelm(String cPage, boolean bTH) {
        Vector aSelm = new Vector();

        int nPos = 0;
        //String cRef = "";
        while (true) {
            int nIni = cPage.indexOf("href=/groups", nPos);
            if (nIni == -1) {
                break;
            }
            int nEnd = cPage.indexOf(">", nIni + 1);
            if (nEnd == -1) {
                break;
            }
            int nEnd2 = cPage.indexOf(" ", nIni + 1);
            if (nEnd2 != -1 && nEnd2 < nEnd) {
                nEnd = nEnd2;
            }

            String cUrl = cPage.substring(nIni + 5, nEnd).trim();

            String cID = getPar(cUrl, "selm");
            if (cID.length() > 0) {
                aSelm.addElement("<" + cID + ">");
                log.info("nntp: ID <" + cID + ">");
            } else if (bTH) {
                String cTh = getPar(cUrl, "threadm");
                if (cTh.length() > 0) {
                    log.info("nntp: Thread: " + cTh);
                    try {
                        String cSubPage = getPage("http://groups.google.it" + cUrl).toString();
                        cTh = getPar(cSubPage, "th");

                        if (cTh.length() > 0) {
                            cSubPage = getPage("http://groups.google.it/groups?dq=&hl=it&lr=&ie=UTF-8&th=" + cTh).toString();
                            Vector aSub = getSelm(cSubPage, false);
                            for (int nPosEle = 0; nPosEle < aSub.size(); nPosEle++) {
                                aSelm.addElement(aSub.elementAt(nPosEle));
                            }
                        }
                    } catch (Throwable ex) {
                        log.info("getSelm", ex);
                    }
                }
            }
            nPos = nEnd;
        }

        return aSelm;
    }

    static Object oLock = new Object();
    static Properties oLockObj = new Properties();
    private String lastNum = "";
    private Properties aArtNum = new Properties();

    private String getArticle(String cId) {

        String cFile = cConfigPath + "nntpcache" + File.separator + cCurrentGroup + File.separator + URLEncoder.encode(cId);
        String cCacheID = cConfigPath + "nntpcache" + File.separator + cCurrentGroup + File.separator + "id-cache.cache";

        // Sync global
        Object oCurrentGroup = null;
        synchronized (oLock) {
            oCurrentGroup = oLockObj.get(cCurrentGroup);
            if (oCurrentGroup == null) {
                oCurrentGroup = new Object();
                oLockObj.put(cCurrentGroup, oCurrentGroup);
            }
        }

        // Sync on group
        String cRet = null;
        synchronized (oCurrentGroup) {
            cRet = readCache(cFile);

            //cId = replace( cId, "%40", "@" );
            //cId = replace( cId, "%24", "$" );
            //cId = replace( cId, "%25", "%" );
            if (cRet == null) {
                try {
                    log.info("nntp: read article from web " + cId);
                    cRet = getPage("http://groups.google.it/groups?output=gplain&selm=" + cId).toString();
                    if (cRet.startsWith("<html>") || cRet.startsWith("L'ID messaggio")) {
                        log.info("nntp: error retrieving http://groups.google.it/groups?output=gplain&selm=" + cId + " " + cCurrentGroup);
                        cRet = null;
                    }

                    if (cRet != null) {
                        writeCache(cFile, cRet);
                    }
                } catch (Throwable ex) {
                    log.info("getArticle", ex);
                }
            }

            java.io.File oFile;
            // ID to num
            Properties cacheID = new Properties();
            oFile = new java.io.File(cCacheID);
            if (oFile.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(cCacheID);
                    cacheID.load(fis);
                    fis.close();
                } catch (Throwable e) {
                }
            }

            // Ora
            String cNum = cacheID.getProperty(cId);
            if (cNum == null) {
                // Creazione ID
                // cerco il max
                long nMax = 0;
                Enumeration keysEnum = cacheID.keys();
                while (keysEnum.hasMoreElements()) {
                    String cNID = cacheID.getProperty((String) keysEnum.nextElement());
                    long nNum = Double.valueOf(cNID).longValue();
                    if (nNum >= nMax) {
                        nMax = nNum;
                    }
                }

                nMax++;

                aArtNum.put("" + nMax, cId);
                lastNum = "" + nMax;
                cacheID.put(cId, "" + nMax);
                try {
                    FileOutputStream fos = new FileOutputStream(cCacheID);
                    cacheID.save(fos, null);
                    fos.close();
                } catch (Throwable e) {
                }
            } else {
                aArtNum.put(cNum, cId);
                lastNum = cNum;
            }
        }

        if (cRet != null) {
            log.info("nntp: request article " + cId + " - " + getRow(cRet, "Subject:"));
        }
        return cRet;
    }

    private String getRow(String cPage, String cId) {
        String cRet = "";
        int nPos = cPage.indexOf(cId);
        if (nPos != -1) {
            int nPos2 = cPage.indexOf('\r', nPos);
            if (nPos2 != -1) {
                cRet = cPage.substring(nPos + cId.length(), nPos2).trim();
            }
        }
        return cRet;
    }

    // <a href="/cgi-bin/ibwrn/artl=36617/it.comp.sicurezza.windows" target="article">Re: Programma per loggare le accensioni</a>  Lu  26. Aug
    // num       1
    // num-      from num to end
    // num-num2  from num to num2
    // NUM<tab>subject<tab>author<tab>date<tab>message-id<tab>references<tab>byte count<tab>line count:NUM
    //"NUM<tab>subject<tab><tab><tab>message-id<tab><tab><tab>:NUM"
    //"NUM<tab>subject<tab><tab><tab>message-id<tab><tab><tab>:NUM"
    /**
     *
     * @param nFrom
     * @param nTo
     * @return
     */
    public Vector xover(long nFrom, long nTo) {
        Vector aRet = new Vector();
        log.error("nntp: xover init");
        for (int nPos = 0; nPos < aOver.size(); nPos++) {
            String cOver = (String) aOver.elementAt(nPos);
            long nCurPos = Double.valueOf(cOver.substring(0, cOver.indexOf("\t"))).longValue();

            if (nCurPos >= nFrom && (nTo == -1 || nTo >= nCurPos)) {
                aRet.addElement(cOver);
            }

        }
        log.error("nntp: xover end");
        return aRet;
    }

    // "http://news.interbulletin.com/cgi-bin/ibwrn/artl=" +cArt +"/" +cGroup
    // Prendo ID e poi sparo il messaggio con google
    // <b>MsgId:</b> &lt;cckui01ui7s1ps1d6lq0d07k7dqmfgq919@4ax.com&gt;<br>
    // "http://groups.google.it/groups?output=gplain&selm=" +cArt
    /**
     *
     * @param nId
     * @return
     */
    public String article(long nId) {
        String cRet = null;
        log.error("nntp: art init");

        String cID = aArtNum.getProperty("" + nId);
        if (cID != null) {
            cRet = getArticle(cID);
        }

        log.error("nntp: art end");

        return (cRet == null ? null : lineFormat.format(cRet));
    }

}
