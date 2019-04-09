/*
 * pop3base
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
 * Title:        pop3base
 * Description:  Classe base per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import java.net.*;
import java.io.*;
import java.util.*;

import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.plugin.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public abstract class pop3base extends pluginbase {

    /**
     *
     */
    public pop3base() {
        super();
    }

    // Vettore email
    private Vector aEmail = new Vector();
    private Vector aSize = new Vector();

    /**
     *
     */
    protected void resetEmailInfo() {
        aEmail = new Vector();
        aSize = new Vector();
    }

    /**
     *
     * @param cEmail
     * @param nLen
     * @return
     */
    protected boolean addEmailInfo(String cEmail, int nLen) {
        boolean bRet = false;
        if (!aEmail.contains(cEmail)) {
            if (mailIsUnderStorageLimit()) {
                bRet = true;
                aEmail.addElement(cEmail);
                aSize.addElement(new Double(nLen));
            }
        }
        return bRet;
    }

    /**
     *
     * @return
     */
    protected boolean mailIsUnderStorageLimit() {
        return (maxMessageNum == -1 || maxMessageNum > aEmail.size());
    }

    /**
     *
     * @return
     */
    public int getMessageNum() {
        return aEmail.size();
    }

    @Setter private int maxMessageNum = -1;

    /**
     *
     * @return
     */
    public int getMessageSize() {
        int nTot = 0;
        for (int n = 0; n < getMessageNum(); n++) {
            nTot += getMessageSize(n + 1);
        }
        return nTot;
    }

    /**
     *
     */
    public void invertSort() {
        Vector aEmail2 = new Vector();
        Vector aSize2 = new Vector();
        for (int n = getMessageNum() - 1; n >= 0; n--) {
            aEmail2.addElement(aEmail.elementAt(n));
            aSize2.addElement(aSize.elementAt(n));
        }
        aEmail = aEmail2;
        aSize = aSize2;
    }

    /**
     *
     * @param nPos
     * @return
     */
    public int getMessageSize(int nPos) {
        return ((Double) aSize.elementAt(nPos - 1)).intValue();
    }

    /**
     *
     * @param nPos
     * @return
     */
    public String getMessageID(int nPos) {
        return (String) aEmail.elementAt(nPos - 1);
    }

    // Funzioni di getmail
    /**
     *
     * @param nPos
     * @return
     */
    public String getMessage(int nPos) {
        return getMessageCache(nPos, 0, true);
    }

    /**
     *
     * @param nPos
     * @param nLine
     * @return
     */
    public String getMessageTop(int nPos, int nLine) {
        return getMessageCache(nPos, nLine, false);
    }

    private String cLastCacheKey = "";
    private String cLastCacheMsg = "";

    private String getMessageCache(int nPos, int nLine, boolean bAll) {
        String cRet = cLastCacheMsg;
        String cCacheKey = "|" + nPos + "|" + nLine + "|" + bAll + "|";
        if (!cCacheKey.equals(cLastCacheKey)) {
            cLastCacheMsg = getMessage(nPos, nLine, bAll);
            cRet = cLastCacheMsg;
            cLastCacheKey = cCacheKey;
        } else {
            log.error("Cache: getmail (" + nPos + ") from cache");
        }
        return cRet;
    }

    /**
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    protected abstract String getMessage(int nPos, int nLine, boolean bAll);

    /**
     *
     * @param SO
     * @param nPos
     * @return
     * @throws Exception
     */
    public boolean streamMessage(OutputStream SO, int nPos) throws Exception {
        return streamMessage(SO, nPos, 0, true);
    }

    /**
     *
     * @param SO
     * @param nPos
     * @param nLine
     * @return
     * @throws Exception
     */
    public boolean streamMessageTop(OutputStream SO, int nPos, int nLine) throws Exception {
        return streamMessage(SO, nPos, nLine, false);
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
        return false;
    }

    // Indica quando viene iniziato e terminato il processo di delete dei messaggi
    /**
     *
     */
    public void delMessageStart() {
    }

    /**
     *
     */
    public void delMessageEnd() {
    }

    /**
     * Concrete method that delete all messages from web trashes. Useful when
     * first you move the message into the web trash and then you want to
     * definetely delete them.
     *
     * @throws DeleteMessageException
     */
    public void delMessagesFromTrash() throws DeleteMessageException {
    }

    @Getter @Setter private String lastErr = "";

    /**
     *
     * @return
     */
    public boolean list() {
        return true;
    }

    // Contatti
    /**
     *
     * @return
     */
    public Vector getContact() {
        return new Vector();
    }

    /**
     *
     * @return
     */
    public String getContactXML() {
        StringBuffer oRet = new StringBuffer();
        Vector oContact = getContact();

        try {
            // Formatta l'xml
            oRet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");
            oRet.append("<contacts version=\"1.00\">\r\n");
            oRet.append("   <items>" + oContact.size() + "</items>\r\n");
            for (int nCont = 0; nCont < oContact.size(); nCont++) {
                String[] oEle = (String[]) oContact.elementAt(nCont);
                //log.info( "getContact     :" +oEle[0] +" - " +oEle[1] );
                oRet.append("   <item>\r\n");                       //,"UTF-8"
                oRet.append("      <name>" + convertXML(oEle[0]) + "</name>\r\n");
                oRet.append("      <email>" + convertXML(oEle[1]) + "</email>\r\n");
                oRet.append("   </item>\r\n");
            }
            oRet.append("</contacts>\r\n");
        } catch (Throwable ex) {
            // Azzera il Vector
            oRet = new StringBuffer();
            log.error("Error", ex);
        }

        return oRet.toString();
    }

    private String convertXML(String cEle) {
        return replace(URLEncoder.encode(cEle).replace('+', ' '), "%40", "@");
    }

}
