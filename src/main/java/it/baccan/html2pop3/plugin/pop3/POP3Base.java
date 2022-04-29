/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
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
import it.baccan.html2pop3.utils.CharsetCoding;
import java.util.concurrent.atomic.AtomicReference;
import kong.unirest.Header;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import kong.unirest.Unirest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public abstract class POP3Base extends PluginBase {

    // Vettore email
    private List<String> aEmail = new ArrayList<>(10);
    private List<Double> aSize = new ArrayList<>(10);
    private String cLastCacheKey = "";
    private String cLastCacheMsg = "";
    @Getter
    private UnirestInstance unirest = null;

    @Getter
    @Setter
    private String lastErr = "";
    @Setter
    private int maxMessageNum = -1;

    /**
     *
     */
    public POP3Base() {
        unirest = Unirest.spawnInstance();
    }

    /**
     *
     */
    protected void resetEmailInfo() {
        aEmail = new ArrayList<>();
        aSize = new ArrayList<>();
    }

    /**
     *
     * @param cEmail
     * @param nLen
     * @return
     */
    protected boolean addEmailInfo(String cEmail, int nLen) {
        boolean bRet = false;
        if (!aEmail.contains(cEmail) && mailIsUnderStorageLimit()) {
            bRet = true;
            aEmail.add(cEmail);
            aSize.add(Double.valueOf(nLen));
        }
        return bRet;
    }

    /**
     *
     * @return
     */
    protected boolean mailIsUnderStorageLimit() {
        return maxMessageNum == -1 || maxMessageNum > aEmail.size();
    }

    /**
     *
     * @return
     */
    public int getMessageNum() {
        return aEmail.size();
    }

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
        List<String> aEmail2 = new ArrayList<>();
        List<Double> aSize2 = new ArrayList<>();
        for (int n = getMessageNum() - 1; n >= 0; n--) {
            aEmail2.add(aEmail.get(n));
            aSize2.add(aSize.get(n));
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
        return aSize.get(nPos - 1).intValue();
    }

    /**
     *
     * @param nPos
     * @return
     */
    public String getMessageID(int nPos) {
        return aEmail.get(nPos - 1);
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
        log.info("delMessageStart: not implemented");
    }

    /**
     *
     */
    public void delMessageEnd() {
        log.info("delMessageEnd: not implemented");
    }

    /**
     * Concrete method that delete all messages from web trashes. Useful when
     * first you move the message into the web trash and then you want to
     * definetely delete them.
     *
     * @throws DeleteMessageException
     */
    public void delMessagesFromTrash() throws DeleteMessageException {
        log.info("delMessagesFromTrash: not implemented");
    }

    /**
     *
     * @return
     */
    public boolean list() {
        return true;
    }

    /**
     * Contatti.
     *
     * @return
     */
    public List<String[]> getContact() {
        return new ArrayList<>();
    }

    /**
     *
     * @return
     */
    public String getContactXML() {
        StringBuilder oRet = new StringBuilder();
        List<String[]> oContact = getContact();

        try {
            // Formatta l'xml
            oRet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n");
            oRet.append("<contacts version=\"1.00\">\r\n");
            oRet.append("   <items>").append(oContact.size()).append("</items>\r\n");
            for (int nCont = 0; nCont < oContact.size(); nCont++) {
                String[] oEle = (String[]) oContact.get(nCont);
                oRet.append("   <item>\r\n");                       //,"UTF-8"
                oRet.append("      <name>").append(convertXML(oEle[0])).append("</name>\r\n");
                oRet.append("      <email>").append(convertXML(oEle[1])).append("</email>\r\n");
                oRet.append("   </item>\r\n");
            }
            oRet.append("</contacts>\r\n");
        } catch (Throwable ex) {
            // Azzera il Vector
            oRet = new StringBuilder();
            log.error("Error", ex);
        }

        return oRet.toString();
    }

    private String convertXML(String cEle) throws UnsupportedEncodingException {
        return URLEncoder
                .encode(cEle, CharsetCoding.UTF_8)
                .replace('+', ' ')
                .replace("%40", "@");
    }

    protected void logHeaders(final HttpResponse stringResponse) {
        log.info("Response: [{}]=[{}] -----------------------------------------------------", stringResponse.getStatus(), stringResponse.getStatusText());
        List<Header> headers = stringResponse.getHeaders().all();
        headers.forEach(action -> {
            log.debug("[{}]=[{}]", action.getName(), action.getValue());
        });
        log.trace("Body [{}]", stringResponse.getBody());
    }

    protected String getLocation(final HttpResponse stringResponse) {
        AtomicReference<String> ret = new AtomicReference<>("");
        List<Header> headers = stringResponse.getHeaders().all();
        headers.forEach((Header action) -> {
            if ("Location".equalsIgnoreCase(action.getName())) {
                ret.set(action.getValue());
            }
        });
        return ret.get();
    }

}
