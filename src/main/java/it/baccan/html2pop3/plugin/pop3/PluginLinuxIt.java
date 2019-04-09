package it.baccan.html2pop3.plugin.pop3;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.exceptions.ParsingMailException;
import it.baccan.html2pop3.utils.CharsetCoding;
import it.baccan.html2pop3.utils.Converter;
import it.baccan.html2pop3.utils.message.FullHeaderMessage;
import it.baccan.html2pop3.utils.message.POP3Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for access the linux.it webmail.
 *
 * @author getting_out <gettingout@linux.it>
 * @version 1.0.0
 *
 * Copyright (C) 2004 Free Software Foundation (http://www.fsf.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Licence details at http://www.gnu.org/licenses/gpl.txt
 */
@Slf4j
public class PluginLinuxIt extends pop3base implements pop3plugin {

    private final String CLASS_NAME = this.getClass().getName();
    private final String BASE_SERVER = "http://picard.linux.it/squirrelmail/src/";
    private final String EOL = String.valueOf((char) 13) + String.valueOf((char) 10);
    private String lastGoodCook = null;

    /**
     * Class that manage the single mail message
     *
     * @author getting_out <gettingout@linux.it>
     */
    private class MailMessage {

        private String body = null;
        private String headers = null;
        private HashMap attachments = null;

        MailMessage(String body, String headers) throws ParsingMailException {
            try {
                this.body = analyzeBody(body);
            } catch (ParsingMailException pme) {
                this.body = "";
                log.error(pme.getMessage());
            }
            this.headers = analyzeHeaders(headers);
            this.attachments = getAttachments(body);
        }

        /**
         * Analyse the body html page and get back all the attachments
         *
         * @param thePage the html page to analyse
         * @return an hashmap with the filename and the content
         * @throws ParsingMailException
         */
        private HashMap getAttachments(String thePage) throws ParsingMailException {
            final String URL_DOWNLOAD = BASE_SERVER + "download.php";
            final String VIEW = "view";
            final String DOWNLOAD = "download";
            final int FILE_NAME = 0x01;
            final int POST_DATA = 0x02;
            final int ALL_OK = 0x03;
            final int DONE = 0x00;
            Pattern patLink = Pattern.compile("<a[^>]*href=\"[^>\"]*\\?([^>\"]*)\"[^>]*>([^>]*)</a>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matLink = null;
            String onlyAttach = null;
            String postData = null;
            String fileName = null;
            HashMap attachments = null;
            int pos = 0;
            int state = DONE;
            byte[] attachContent = null;

            if ((pos = thePage.indexOf("<b>Attachments:</b>")) >= 0) {
                //ok abbiamo allegati
                onlyAttach = thePage.substring(pos);
                matLink = patLink.matcher(onlyAttach);
                attachments = new HashMap();
                while (matLink.find()) {
                    if (!matLink.group(2).equalsIgnoreCase(DOWNLOAD) && !matLink.group(2).equalsIgnoreCase(VIEW)) {
                        //questo è il nome del file
                        fileName = matLink.group(2);
                        state |= FILE_NAME;
                    } else {
                        if (matLink.group(2).equalsIgnoreCase(DOWNLOAD)) {
                            //questo è il post dei dati per il avviare il download
                            postData = matLink.group(1).replaceAll("&amp;", "&");
                            state |= POST_DATA;
                        }
                    }
                    if (state == ALL_OK) {
                        log.info("LinuxIt::MailMessage::getAttachments() downloading " + fileName);
                        state = DONE;
                        try {
                            attachContent = getPageBytes(URL_DOWNLOAD + "?" + postData, lastGoodCook);
                            attachments.put(fileName, attachContent);
                        } catch (Exception e) {
                            log.error("LinuxIt::MailMessage::getAttachments() Exception: error while downloading attachment");
                            log.error("Error", e);
                            throw new ParsingMailException(e.getMessage());
                        }
                    }
                }
            }
            return attachments;
        }

        /**
         * Analyse the html page and get back the body of the mail
         *
         * @param body the html page with the body
         * @return the clean body
         * @throws ParsingMailException
         */
        private String analyzeBody(String body) throws ParsingMailException {
            final String DOWN_URL = "download.php";
            final String PASSED_ENT_ID = "passed_ent_id=";
            String theBody = null;
            String buff = null;
            StringBuffer sb = new StringBuffer();
            Pattern patBodyText = Pattern.compile("<pre>(.+)</pre>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Pattern p = Pattern.compile("<a[^>]*href=\"(" + DOWN_URL + "[^\"]*)\"[^>]*>");
            Matcher matBodyText = null;
            Matcher m = null;
            boolean ok4TheBody = false;

            //I messaggi sono tutti di testo. Quelli in html arrivano con l'html allegato
            if ((body != null) && (!body.trim().equals(""))) {
                sb.delete(0, sb.length());
                m = p.matcher(body);
                while (m.find()) {
                    //there are some url that allow you to download the body as text.
                    //let's try to use it
                    if (m.group(1).indexOf(PASSED_ENT_ID) > 0) {
                        ok4TheBody = true;
                        //ok, we have a link where download the body.
                        //May be more than one so I'll append to a StringBuffer to get
                        //the full body
                        sb.append(getBodyFromLink(BASE_SERVER + m.group(1)));
                    }
                }
                if (ok4TheBody) {
                    theBody = sb.toString();
                } else {
                    //we do not have downloaded the body from the apposite link.
                    //analyse the page.
                    matBodyText = patBodyText.matcher(body);
                    if (matBodyText.find()) {
                        theBody = Converter.html2TextChar(matBodyText.group(1), true);
                    } else {
                        throw new ParsingMailException("Error during the body analysing");
                    }
                }
            } else {
                theBody = "";
            }
            return theBody;
        }

        /**
         * Function that retreive the body in text form from the appropiate URL.
         *
         * @param url: the url from which download it
         * @return the body
         */
        private String getBodyFromLink(String url) {
            String content = null;

            try {
                content = new String(getPageBytes(url.replaceAll("&amp;", "&"), lastGoodCook));
            } catch (Exception e) {
                log.error("PluginLinuxIt::getBodyFromLink()");
                log.error("Error while downloading the body from link. Exception: " + e.getMessage());
                content = null;
            }
            return content;
        }

        /**
         * Analyse the html page and get back the headers of the mail
         *
         * @param headers the html page with the headers
         * @return the clean headers
         * @throws ParsingMailException
         */
        private String analyzeHeaders(String headers) throws ParsingMailException {
            int start = 0;
            int end = 0;
            String headUpper = headers.toUpperCase();
            String key = null;
            String value = null;
            String buff = null;
            StringBuffer fullHeaders = new StringBuffer();
            Pattern patHeader = Pattern.compile("<b>.+</b>", Pattern.CASE_INSENSITIVE);
            Pattern pat4Bold = Pattern.compile("<b>(.+)</b>", Pattern.CASE_INSENSITIVE);
            Matcher matHeader = null;
            Matcher mat4Bold = null;
            Matcher mat = null;

            start = headUpper.indexOf("<TABLE");
            start = headUpper.indexOf("<TABLE", start + 1);
            start = headUpper.indexOf("<TABLE", start + 1);
            start = headUpper.indexOf("<TR>", start); //qui cominciano gli header
            end = headUpper.indexOf("</TABLE>", start);
            end = headUpper.lastIndexOf("</TT>", end) + 5; //qui finiscono

            headers = headers.substring(start, end);

            matHeader = patHeader.matcher(headers);
            while (matHeader.find()) {
                mat4Bold = pat4Bold.matcher(matHeader.group());
                if (mat4Bold.find()) {
                    key = mat4Bold.group(1);
                    value = headers.substring(matHeader.end(), headers.indexOf("</tt>", matHeader.end()))
                            .replaceAll("\n", "")
                            .replaceAll("\r", "")
                            .replaceAll("<br />", EOL)
                            .replaceAll("&nbsp;", " ");
                    value = Converter.html2TextChar(value, true);
                }
                fullHeaders.append(key + " " + value);
            }

            return fullHeaders.toString() + "\n";
        }

        /**
         * Give the current message type
         *
         * @return the message type
         */
        int getMessageType() {
            return POP3Message.TEXT_MESSAGE;
        }

        private String parseMultipleAddresses(String currentAddresses, int lastEndPos) {
            Pattern pat = Pattern.compile("\\S.*");
            Matcher mat = null;
            String buf = headers;

            try {
                while (currentAddresses.charAt(currentAddresses.length() - 1) == ',') {
                    buf = buf.substring(lastEndPos + 2);
                    mat = pat.matcher(buf);
                    if (mat.find()) {
                        currentAddresses += mat.group();
                        lastEndPos = mat.end();
                    }
                }
            } catch (StringIndexOutOfBoundsException sioobe) {
                log.error("PluginLinuxIt$MailMessage.parseMultipleAddresses() StringIndexOutOfBoundsException");
                //siooblog.error("Error",e);
            }
            return currentAddresses;
        }

        /**
         * Give the current addressee of the mail
         *
         * @return the addressee
         */
        String getTo() {
            Pattern pat = Pattern.compile("^To:(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher mat = pat.matcher(headers);
            String ret = "not@found";
            String buff = null;
            char c = '\0';
            int i = 0;

            if (mat.find()) {
                ret = parseMultipleAddresses(mat.group(1).trim(), mat.end());
            }
            return ret;
        }

        /**
         * Give the current CC of the mail
         *
         * @return the CC in mail
         */
        String getCC() {
            Pattern pat = Pattern.compile("^CC:(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher mat = pat.matcher(headers);
            String ret = "";

            if (mat.find()) {
                ret = parseMultipleAddresses(mat.group(1).trim(), mat.end());
            }
            return ret;
        }

        /**
         * Give the current From of the mail
         *
         * @return the sender of the mail
         */
        String getFrom() {
            Pattern patFrom = Pattern.compile("^From:(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher mat = patFrom.matcher(headers);
            String ret = "no@found";

            if (mat.find()) {
                ret = mat.group(1).trim();
            }

            return ret;
        }

        /**
         * Give the date of the current mail
         *
         * @return the date
         */
        String getDate() {
            Pattern pat = Pattern.compile("^Date:(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher mat = pat.matcher(headers);
            String ret = "";

            if (mat.find()) {
                ret = mat.group(1).trim();
            }
            return ret;
        }

        /**
         * Return the subject of the mail
         *
         * @return the subject
         */
        String getSubject() {
            Pattern pat = Pattern.compile("^Subject:(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher mat = pat.matcher(headers);
            String ret = "";

            if (mat.find()) {
                ret = mat.group(1).trim();
            }
            return ret;
        }

        /**
         * Return the body of the mail
         *
         * @return the body
         */
        String getBody() {
            return body;
        }

        /**
         * Return the attachments of the message
         *
         * @return an HashMap with the attachments
         */
        HashMap getAttachments() {
            return attachments;
        }

        /**
         * @return the headers of the message
         */
        public String getHeaders() {
            return headers;
        }

    }

    /* (non-Javadoc)
	 * @see it.baccan.plugin.pop3.pop3base#getMessage(int, int, boolean)
     */
    /**
     *
     * @param pos
     * @param line
     * @param all
     * @return
     */
    public String getMessage(int pos, int line, boolean all) {
        final String MESSAGE_BODY = BASE_SERVER + "read_body.php";
        final String MESSAGE_HEADERS = BASE_SERVER + "view_header.php";
        String messageId = getMessageID(pos);
        String postData = null;
        String message = null;
        String headers = null;
        String ret = null;
        String s = null;
        MailMessage mm = null;
        POP3Message p3m = null;
        HashMap attach = null;
        Iterator iterator = null;

        try {
            log.info("LinuxIt::getMessage() getting message body for " + messageId);
            postData = "mailbox=INBOX&passed_id=" + messageId + "&startMessage=1";
            message = postPage(MESSAGE_BODY, lastGoodCook, postData).toString();

            log.info("LinuxIt::getMessage() getting message headers for " + messageId);
            headers = postPage(MESSAGE_HEADERS, lastGoodCook, postData).toString();

            //handling the message with the appropriate class
            mm = new MailMessage(message, headers);

            FullHeaderMessage fhm = new FullHeaderMessage(mm.getHeaders(), mm.getBody());
            //attachments
            attach = mm.getAttachments();
            if (attach != null) {
                //ok, we have attachments
                iterator = attach.keySet().iterator();
                while (iterator.hasNext()) {
                    s = (String) iterator.next();
                    fhm.addAttach(s, (byte[]) attach.get(s));
                }
            }

            ret = fhm.getMessage();
        } catch (ParsingMailException pme) {
            log.error("LinuxIt::getMessage() ParsingMailException on message " + messageId);
            log.error("Error", pme);
        } catch (Throwable t) {
            log.error("LinuxIt::getMessage() errore nel reperimento del corpo del messaggio");
            log.error("Error", t);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see it.baccan.plugin.pop3.pop3plugin#login(java.lang.String, java.lang.String)
     */
    /**
     *
     * @param user
     * @param password
     * @return
     */
    public boolean login(String user, String password) {
        final String LOGIN_STAGE_I = BASE_SERVER + "login.php";
        final String LOGIN_STAGE_II = BASE_SERVER + "redirect.php";
        //				LOGIN_STAGE_III is a redirect so I get it dinamically
        String returnPage = null;
        String postData = null;
        boolean ret = false;
        int mailsNumber = 0;

        try {
            log.info("LinuxIt::login() stage I");
            returnPage = getPage(LOGIN_STAGE_I).toString();

            log.info("LinuxIt::login() stage II");
            //username for login is the mail address without domain
            user = user.substring(0, user.indexOf("@"));
            postData = "login_username=" + URLEncoder.encode(user, CharsetCoding.UTF_8) + "&"
                    + "secretkey=" + URLEncoder.encode(password, CharsetCoding.UTF_8) + "&"
                    + "js_autodetect_results=1&just_logged_in=1";
            lastGoodCook = getCookie();
            returnPage = postPage(LOGIN_STAGE_II, lastGoodCook, postData).toString();
            lastGoodCook += getCookie();

            if (returnPage.indexOf("login.php") < 0) {
                //right Login
                //we have a redirect
                log.info("LinuxIt::login() stage III");
                returnPage = getPage(BASE_SERVER + getLocation(), lastGoodCook).toString();

                //extracting mail
                mailsNumber = extractMail();
                log.info("LinuxIt::login() number of mail: " + mailsNumber);
                ret = true;
            }

        } catch (Throwable ex) {
            log.error("LinuxIt::login() errore durante la fase di login.");
            log.error("Error", ex);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see it.baccan.plugin.pop3.pop3plugin#delMessage(int)
     */
    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        final String DELETE_URL = BASE_SERVER + "delete_message.php";
        String postData = null;
        String messageId = getMessageID(nPos);
        boolean ret = false;

        //WARNING: this doesn't delete the message but move it to the trash can
        //         on the web
        postData = "mailbox=INBOX&message=" + messageId + "&sort=6&startMessage=1";
        try {
            log.info("LinuxIt::delMessage() deleting message " + messageId);
            postPage(DELETE_URL, lastGoodCook, postData);
            ret = true;
        } catch (Exception e) {
            log.error("LinuxIt::delMessage() error while deleting message");
            log.error("Error", e);
        }

        return ret;
    }

    /**
     * Extract the list of mail for the inbox and all their ID.
     *
     * @return the number of mails found on server
     */
    private int extractMail() {
        final String LIST_PAGE = BASE_SERVER + "right_main.php";
        String returnPage = null;
        String postData = null;
        Pattern patEmailId = Pattern.compile(";passed_id=[0-9]*&");
        Pattern patEmailId2 = Pattern.compile("[0-9]+");
        Matcher matEmailId = null;
        Matcher matEmailId2 = null;
        StringBuffer buff = new StringBuffer();
        int ret = 0;

        try {
            //get the page with mail list
            log.info("LinuxIt::extractMail() listing mail");
            returnPage = getPage(LIST_PAGE, lastGoodCook).toString();

            //showing all messages, no pagination
            log.info("LinuxIt::extractMail() listing mail - All messages");
            postData = "PG_SHOWALL=1&amp;use_mailbox_cache=0&amp;startMessage=1&amp;mailbox=INBOX";
            returnPage = postPage(LIST_PAGE, lastGoodCook, postData).toString();

            matEmailId = patEmailId.matcher(returnPage);
            //extracting each emailId
            while (matEmailId.find()) {
                buff.delete(0, buff.length());
                buff.append(matEmailId.group());
                matEmailId2 = patEmailId2.matcher(buff);
                if (matEmailId2.find()) {
                    if (addEmailInfo(matEmailId2.group(), 1024)) {
                        ret++;
                    }
                }
            }
        } catch (Throwable t) {
            log.error("LinuxIt::extractMail() errore durante l'estrazione dell'elenco delle mail");
            log.error("Error", t);
        }

        return ret;
    }

    /* (non-Javadoc)
	 * @see it.baccan.plugin.pop3.pop3base#delMessagesFromTrash()
     */
    public void delMessagesFromTrash() throws DeleteMessageException {
        final String EMPTY_TRASH_URL = BASE_SERVER + "empty_trash.php";

        super.delMessagesFromTrash();

        log.info(CLASS_NAME + " deleting message from trash");
        try {
            getPage(EMPTY_TRASH_URL, lastGoodCook);
        } catch (Throwable t) {
            throw new DeleteMessageException(t.getMessage());
        }
    }

}
