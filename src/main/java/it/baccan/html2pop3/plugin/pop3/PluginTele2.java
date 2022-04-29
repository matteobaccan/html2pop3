package it.baccan.html2pop3.plugin.pop3;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.utils.*;
import it.baccan.html2pop3.utils.message.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for access the tele2 webmail.
 *
 * @author gettingout <gettingout@linux.it>
 * @version 1.0.1
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
public class PluginTele2 extends POP3Base implements POP3Plugin {

    private final String CLASS_NAME = this.getClass().getName();
    private final String WRONG_LOGIN = "<title>Login non riuscito</title>";
    private final String MAIL_DETAIL = "/cgi-bin/gx.cgi/AppLogic+mobmain";
    private final String BASE_DELETE = "/cgi-bin/gx.cgi/AppLogic+mobmain?del.x=1&msgOp";
    private final String SERVER = "http://webmail.tele2.it";
    private String lastUrl = "";
    private String lastGoodCook = "";
    private String secondServer = null;
    private BoxHandling boxHandling = null;

    /* ******************************************************************************* */
 /* ******************************************************************************* */
 /* ******************************************************************************* */
    /**
     * Class for managing a single mail message
     */
    private class MailMessage {

        private String mailMsg = null;
        private String from = "";
        private String date = "";
        private String to = "";
        private String body = "";
        private String subject = "";
        private String cc = "";
        private Map<String, byte[]> attachments = new HashMap<>();
        private boolean textMsg = true;

        /**
         * Class constructor
         *
         * @param s the stream that rappresent the HTML of the mail detail page
         */
        public MailMessage(String s) {
            String mail4Search = null;
            String downloadHref = "";
            String downloadName = "";
            byte[] attachContent = null;
            int hour = 0;
            int start = -1;
            int end = -1;
            int lastEnd = -1;
            int retry = 0;
            GregorianCalendar gc = new GregorianCalendar();
            Pattern patDate = Pattern.compile("^(\\d{4})/(\\d{2})/(\\d{2}).{5}(AM|PM) (0?\\d{1,2}):(0?\\d{1,2}):(0?\\d{1,2}).*$");
            Matcher matDate = null;

            this.mailMsg = s;
            mail4Search = mailMsg.toUpperCase();

            //retrieving the sender of the mail
            start = mailMsg.indexOf("<b>Da: </b>");
            if (start > -1) {
                if ((start = mailMsg.indexOf("</td><td>", start)) > -1) {
                    end = mailMsg.indexOf("&nbsp;<a", start);
                    from = mailMsg.substring(start + 9, end);
                    from = Converter.html2TextChar(from);
                }
            }

            //retriveing the date
            if ((start = mailMsg.indexOf("<b>Data: </b>")) > -1) {
                if ((start = mailMsg.indexOf("</td><td>", start)) > -1) {
                    date = mailMsg.substring(start + 9, mailMsg.indexOf("</td></tr>", start));
                    try {
                        //2004/11/11 gio PM 12:14:20 GMT+01:00
                        matDate = patDate.matcher(date);
                        if (matDate.find()) {
                            hour = Integer.parseInt(matDate.group(5));
                            if (matDate.group(4).equals("PM") && hour != 12) {
                                hour += 12;
                            } else if (matDate.group(4).equals("AM") && hour == 12) {
                                hour = 0;
                            }

                            gc.set(Integer.parseInt(matDate.group(1)), //year
                                    Integer.parseInt(matDate.group(2)) - 1, //month
                                    Integer.parseInt(matDate.group(3)), //day
                                    hour, //hour
                                    Integer.parseInt(matDate.group(6)), //minutes
                                    Integer.parseInt(matDate.group(7)));//seconds
                            date = formatDate(gc.getTime());
                        }
                    } catch (Exception ex) {
                        date = mailMsg.substring(start + 9, mailMsg.indexOf("</td></tr>", start));
                    }
                }
            }

            //retrieving the addressee
            if ((start = mailMsg.indexOf("<b>A: </b>")) > -1) {
                if ((start = mailMsg.indexOf("</td><td>", start)) > -1) {
                    to = Converter.html2TextChar(mailMsg.substring(start + 9, mailMsg.indexOf("</td></tr>", start)));
                }
            }

            //retrieving the cc
            if ((start = mailMsg.indexOf("<b>Cc: </b>")) > -1) {
                if ((start = mailMsg.indexOf("</td><td>", start)) > -1) {
                    cc = Converter.html2TextChar(mailMsg.substring(start + 9, mailMsg.indexOf("&nbsp;<a href=\"/agent/MobAddr?mod=1&DB_ADDR_NAME=", start)));
                }
            }

            //retrieving the mail subject
            if ((start = mailMsg.indexOf("<b>Oggetto: </b>")) > -1) {
                if ((start = mailMsg.indexOf("</td><td>", start)) > -1) {
                    subject = Converter.html2TextChar(mailMsg.substring(start + 9, mailMsg.indexOf("</td></tr>", start)));
                }
            }

            //retrieving the message body
            //if it's an html message, the work is easy
            start = mail4Search.indexOf("<HTML>", 0);
            if ((start = mail4Search.indexOf("<HTML>", start + 6)) > -1) {
                //this is an email message in html format
                end = mail4Search.indexOf("</HTML>", start) + 7;
                textMsg = false;
                body = Converter.htmlCharCorrect(mailMsg.substring(start, end));
            } else {
                //text mail message
                textMsg = true;
                if ((start = mailMsg.indexOf("</form>", 0)) > -1) {
                    if ((start = mailMsg.indexOf("</form>", start + 1)) > -1) {
                        if ((start = mailMsg.indexOf("</form>", start + 1)) > -1) {
                            if ((start = mailMsg.indexOf("<td>", start + 1)) > -1) {
                                //search for the last "<base href=\"http://webmail.tele2.it\">"
                                end = start;
                                while ((end = mailMsg.indexOf("<base href=\"" + secondServer + "\">", end)) > -1) {
                                    lastEnd = end++;
                                }
                                end = lastEnd;
                                body = mailMsg.substring(start + 4, end);
                                //tolgo gli eventuali "\n" che ci sono all'inizio ed alla fine
                                while ((((int) body.charAt(0)) == 13 || ((int) body.charAt(0)) == 10) && body.length() > 1) {
                                    body = body.substring(1);
                                }
                                while (body.length() > 1 && (((int) body.charAt(body.length() - 1)) == 13 || ((int) body.charAt(body.length() - 1)) == 10)) {
                                    body = body.substring(0, body.length() - 1);
                                }

                                if (body.startsWith("<pre>") || body.startsWith("<PRE>")) {
                                    Pattern p = Pattern.compile("</PRE>", Pattern.CASE_INSENSITIVE);
                                    Matcher m = p.matcher(body);
                                    int count = 0;
                                    start = 0;
                                    while (m.find(start)) {
                                        count++;
                                        start = m.end();
                                    }
                                    if (count > 1) {
                                        //it's a bit strange message so I treat it like html
                                        textMsg = false;
                                        body = Converter.htmlCharCorrect(body);
                                    } else {
                                        //check for another type of strange html message
                                        start = -1;
                                        count = 0;
                                        while ((start = mail4Search.indexOf("</HTML>", ++start)) > -1) {
                                            count++;
                                        }
                                        if (count > 1) {
                                            textMsg = false;
                                            body = Converter.htmlCharCorrect(body);
                                        } else {
                                            //if body starts with <pre> then must end with </pre>
                                            body = body.substring(5);
                                            if ((end = body.toLowerCase().indexOf("</pre>")) > 0) {
                                                body = body.substring(0, end);
                                            }
                                            if (body.endsWith("</pre>") || body.endsWith("</PRE>")) {
                                                body = body.substring(0, body.length() - 6);
                                            }
                                            body = Converter.html2TextChar(body, true);
                                        }
                                    }
                                } else {
                                    //it's still HTML message type
                                    textMsg = false;
                                    body = Converter.htmlCharCorrect(body);
                                }
                            }
                        }
                    }
                }
            }

            //attachment management
            start = 0;
            while ((start = mail4Search.indexOf("<BR>SCARICA ALLEGATO: ", start)) > -1) {
                if ((start = mail4Search.indexOf("<A HREF=\"/AGENT/MOBMAIN/", start)) > -1) {
                    start = mail4Search.indexOf("HREF=\"", start) + 6;
                    end = mail4Search.indexOf("\"", start);
                    downloadHref = secondServer + mailMsg.substring(start, end);
                    //finding the filename
                    downloadName = downloadHref.substring(downloadHref.indexOf("&FileName=") + 10);
                    try {
                        retry = 0;
                        do {
                            log.info("attachment (" + downloadName + ") (" + retry + "): " + downloadHref);
                            attachContent = getPageBytes(downloadHref, lastGoodCook);
                        } while (!isValidSession(attachContent.toString()) && retry++ < 5);
                        if (!isValidSession(attachContent.toString())) {
                            throw new Throwable("error downloading attachment. Invalid Session");
                        }
                    } catch (Throwable t) {
                        log.error("error downloading file " + downloadName + " " + t);
                        continue;
                    }
                    attachments.put(downloadName, attachContent);
                }
            }
        }

        /**
         * retrieving the body of the mail
         *
         * @return
         */
        public String getBody() {
            return body;
        }

        /**
         * retrieving the date of the mail
         *
         * @return
         */
        public String getDate() {
            return date;
        }

        /**
         * Retrieving the sender of the mail
         *
         * @return
         */
        public String getFrom() {
            return from;
        }

        /**
         * retrieving the addressee of the mail
         *
         * @return
         */
        public String getTo() {
            return to;
        }

        /**
         * retrieving the subject of the mail
         *
         * @return
         */
        public String getSubject() {
            return subject;
        }

        /**
         * retrieving the cc of the mail
         *
         * @return
         */
        public String getCc() {
            return cc;
        }

        /**
         * retrieving the attachments of the mail
         *
         * @return
         */
        public Map<String, byte[]> getAttachments() {
            return attachments;
        }

        /**
         * return if the mail message is in plain text or not
         *
         * @return
         */
        public boolean isTextMsg() {
            return textMsg;
        }

    }

    //end MailMessage

    /* ******************************************************************************* */
    /* ******************************************************************************* */
    /* ******************************************************************************* */
    private interface BoxHandling {

        public String getLoginResponse(int retry) throws Throwable;

        public String getUrl();

        public String getMessage(int pos, int line, boolean all);

        public boolean delMessage(int pos);

        public boolean isValidLogin(String post);

        public int extractMail(String post) throws Exception;

        public void delMessagesFromTrash() throws DeleteMessageException;
    }

    //BoxHandling

    /* ******************************************************************************* */
    /* ******************************************************************************* */
    /* ******************************************************************************* */
    private class MBoxHandling1 implements BoxHandling {

        private String newUrl = null;

        @Override
        public String getLoginResponse(int retry) throws Throwable {
            String retFromPost = null;

            retFromPost = postPage(lastUrl, lastGoodCook, "", lastUrl).toString();
            newUrl = secondServer + getLocation();
            lastGoodCook = getCookie().replaceAll("path=/", "");

            //redirect ancora. Mannaggia a loro!
            log.info("PlugInTele2: II pagina II redirect(" + retry + ")");
            retFromPost = getPage(newUrl, lastGoodCook).toString();
            return retFromPost;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#getUrl()
         */
        @Override
        public String getUrl() {
            return newUrl;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#isValidLogin(java.lang.String)
         */
        @Override
        public boolean isValidLogin(String post) {
            return post.indexOf(WRONG_LOGIN) == -1;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#extractMail(java.lang.String)
         */
        @Override
        public int extractMail(String pageContent) throws Exception {
            final String EMAIL_ID_START = "/cgi-bin/gx.cgi/AppLogic+mobmain?msgvw=";
            final String EMAIL_ID_END = "\"";
            final String EMAIL_SIZE_START = "<td align=\"right\">&nbsp;";
            final String EMAIL_SIZE_END = "&nbsp;</td>";
            final String SEARCH_NEXT_PAGE = ">Pagina Succ.&nbsp;&gt;&gt;";//"/agent/mobmain?baseMsg=";
            final String PAGING_MAIL = "/agent/mobmain";
            final int MOLTIPLICATOR = 1024;
            int nMail = 0;
            int pos = 0;
            int endPos = 0;
            int emailSize = 0;
            int retry2 = 0;
            StringBuffer emailId = new StringBuffer();
            StringBuffer buffer = new StringBuffer();
            String post = null;
            String retFromPost = null;

            //scroll the page in search for all the message ids and sizes
            while (pos != -1) {
                pos = pageContent.indexOf(EMAIL_ID_START, pos);
                if (pos > -1) {
                    endPos = pageContent.indexOf(EMAIL_ID_END, pos);
                    if (endPos > -1) {
                        emailId.delete(0, emailId.length());
                        emailId.append(pageContent.substring(pos + EMAIL_ID_START.length(), endPos));
                        pos = endPos;

                        //search for mail size
                        pos = pageContent.indexOf(EMAIL_SIZE_START, pos);
                        endPos = pageContent.indexOf(EMAIL_SIZE_END, pos);
                        buffer.delete(0, buffer.length());
                        buffer.append(pageContent.substring(pos + EMAIL_SIZE_START.length(), endPos));

                        log.error("PlugInTele2: " + emailId + ": " + buffer);

                        try {
                            if (buffer.toString().indexOf('k') > -1 || buffer.toString().indexOf('K') > -1) {
                                //kilobyte
                                endPos = buffer.toString().toUpperCase().indexOf('K');
                                emailSize = Integer.parseInt(buffer.substring(0, endPos)) * MOLTIPLICATOR;
                            } else {
                                if (buffer.toString().indexOf('m') > -1 || buffer.toString().indexOf('M') > -1) {
                                    //megabyte
                                    endPos = buffer.toString().toUpperCase().indexOf('M');
                                    emailSize = Integer.parseInt(buffer.substring(0, endPos)) * MOLTIPLICATOR * MOLTIPLICATOR;
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            emailSize = MOLTIPLICATOR;
                        }
                        //adding the email to the structure
                        if (addEmailInfo(emailId.toString(), emailSize)) {
                            nMail++;
                        }
                    } else {
                        //exiting from the loop
                        pos = -1;
                    }
                }
            }

            //paginazione
            if (pageContent.indexOf(SEARCH_NEXT_PAGE) > -1 && nMail > 0) {
                retry2 = 0;
                do {
                    log.info("PlugInTele2: seconda pagina, paginazione (" + retry2 + "), messaggio " + (nMail + 1));
                    post = "baseMsg=" + (nMail + 1) + "&fold=INBOX";
                    retFromPost = filterRem(postPage(secondServer + PAGING_MAIL, lastGoodCook, post).toString());

                    //riprovo per 5 volte poiché ho notato che spesso dietro a proxies si
                    //mangia i cookies e genera questi errori.
                } while (!isValidSession(retFromPost) && (retry2++ < 5));
                lastUrl = secondServer + PAGING_MAIL;
                nMail += extractMail(retFromPost);
            }

            return nMail;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#getMessage(int, int, boolean)
         */
        @Override
        public String getMessage(int pos, int line, boolean all) {
            String retFromPost = null;
            String messageId = getMessageID(pos);
            String ret = null;
            MailMessage mail = null;
            POP3Message p3m = null;
            int retry = 0;

            try {
                //solito problema dei cookies dietro al proxy
                do {
                    log.error("PluginTele2.getMessage() retriving message " + messageId + " (" + retry + ")");
                    retFromPost = postPage(secondServer + MAIL_DETAIL, lastGoodCook, "msgvw=" + messageId).toString();
                } while (!isValidSession(retFromPost) && (retry++ < 5));

                if (isValidSession(retFromPost)) {
                    mail = new MailMessage(retFromPost);
                    p3m = new POP3Message();
                    p3m.setCharset(CharsetCoding.ISO_8859_1);
                    p3m.setDa(mail.getFrom());
                    p3m.setA(mail.getTo());
                    p3m.setData(mail.getDate());
                    p3m.setBody(mail.getBody());
                    p3m.setOggetto(mail.getSubject());
                    if (mail.getCc().length() > 0) {
                        p3m.setCc(mail.getCc());
                    }
                    //attachments
                    Map hm = mail.getAttachments();
                    Iterator keySet = mail.getAttachments().keySet().iterator();
                    while (keySet.hasNext()) {
                        String s = (String) keySet.next();
                        p3m.addAttach(s, (byte[]) hm.get(s));
                    }
                    if (mail.isTextMsg()) {
                        p3m.setMessageType(POP3Message.TEXT_MESSAGE);
                    }
                    ret = p3m.getMessage(line, all);
                } else {
                    log.info("PluginTele2.getMessage() Invalid Session");
                }
            } catch (Exception e) {
                log.error("PluginTele2.getMessage() Exception: " + e);
                log.error("Error", e);
            }
            return ret;
        }

        /* (non-Javadoc)
     * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#delMessage(int)
         */
        @Override
        public boolean delMessage(int pos) {
            String messageId = getMessageID(pos);
            String deleteUrl = secondServer + BASE_DELETE + messageId + "=on";
            String retPage = null;
            int retry = 0;
            boolean ret = true;

            try {
                do {
                    log.info("deleting " + messageId + " (" + retry + "): " + deleteUrl);
                    retPage = getPage(deleteUrl, lastGoodCook).toString();
                } while (!isValidSession(retPage) && retry++ < 5);
            } catch (Exception ex) {
                log.error("PluginTele2.delMessage() error posting data");
                log.error("Error", ex);
                ret = false;
            } catch (Throwable t) {
                log.error("PluginTele2.delMessage() error posting data");
                log.error("Error", t);
                ret = false;
            }

            return ret;
        }

        /* (non-Javadoc)
     * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#delMessagesFromTrash()
         */
        @Override
        public void delMessagesFromTrash() throws DeleteMessageException {
            final String TRASH_URL = secondServer + "/cgi-bin/gx.cgi/AppLogic+mobmain?fold=Trash";
            int retry = 0;
            String retPage = null;
            Pattern pattern = Pattern.compile("<a\\s+href=\"(/cgi\\-bin/gx\\.cgi/AppLogic\\+mobmain\\?del\\.x=1&([a-z0-9]+)=on)\"\\s*>",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = null;

            try {
                do {
                    log.info(CLASS_NAME + " entering the trash (" + retry + ")");
                    retPage = getPage(TRASH_URL, lastGoodCook).toString();
                } while (!isValidSession(retPage) && retry++ < 5);

                if (isValidSession(retPage)) {
                    retPage = retPage.replace("&amp;", "&");
                    matcher = pattern.matcher(retPage);
                    while (matcher.find()) {
                        retry = 0;
                        do {
                            log.info(CLASS_NAME + " Removing message " + matcher.group(2) + " from trash (" + retry + ")");
                            retPage = getPage(secondServer + matcher.group(1), lastGoodCook).toString();
                        } while (!isValidSession(retPage) && retry++ < 5);
                    }
                } else {
                    throw new DeleteMessageException("Not valid session after loop");
                }
            } catch (DeleteMessageException dme) {
                throw dme;
            } catch (Throwable t) {
                log.error(CLASS_NAME + " Throwable: error deleting messages from the trash");
                log.error("Error", t);
                throw new DeleteMessageException(t.getMessage());
            }
        }

    }

    //MBoxHandling1

    /* ******************************************************************************* */
    /* ******************************************************************************* */
    /* ******************************************************************************* */
    private class MBoxHandling2 implements BoxHandling {

        private String newUrl = null;
        private Map<String, Integer> months = null;
        private String baseHref = null;

        public MBoxHandling2() {
            months = new HashMap<>();
            months.put("Jan", Integer.valueOf(0));
            months.put("Feb", Integer.valueOf(1));
            months.put("Mar", Integer.valueOf(2));
            months.put("Apr", Integer.valueOf(3));
            months.put("Maj", Integer.valueOf(4));
            months.put("Jun", Integer.valueOf(5));
            months.put("Jul", Integer.valueOf(6));
            months.put("Aug", Integer.valueOf(7));
            months.put("Sep", Integer.valueOf(8));
            months.put("Oct", Integer.valueOf(9));
            months.put("Nov", Integer.valueOf(10));
            months.put("Dec", Integer.valueOf(11));
        }

        private int getMonth(String month) {
            int ret = -1;

            try {
                ret = ((Integer) months.get(month)).intValue();
            } catch (Exception e) {
                log.error(CLASS_NAME + " error getting the month: " + month);
                ret = -1;
            }
            return ret;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#getLoginResponse(int)
         */
        @Override
        public String getLoginResponse(int retry) throws Throwable {
            String retFromPost = null;

            log.info("nuova gestione della casella!!!!");
            retFromPost = postPage(lastUrl, lastGoodCook, "", lastUrl).toString();

            //altro redirect (dovrebbe essere la inbox vera)
            baseHref = lastUrl.substring(0, lastUrl.lastIndexOf("/"));
            newUrl = lastUrl.substring(0, lastUrl.lastIndexOf("/")) + "/mailbox.wssp?Mailbox=INBOX&";
            retFromPost = getPage(newUrl, lastGoodCook).toString();
            return retFromPost;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#getUrl()
         */
        @Override
        public String getUrl() {
            return newUrl;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#isValidLogin(java.lang.String)
         */
        @Override
        public boolean isValidLogin(String post) {
            return !Pattern.matches("(?i)<input.+name=\"username\"", post);
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#extractMail(java.lang.String)
         */
        @Override
        public int extractMail(String post) throws Exception {
            Pattern linkMessage = Pattern.compile("<A.+HREF=\"(Message.wssp\\?Mailbox=INBOX&MSG=(\\d+))\"[^>]*>",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Pattern indxField = Pattern.compile("indxField\\+\\+;");
            Pattern dimension = Pattern.compile("var.+x.+=.+'(\\d+)';");
            Pattern page2 = Pattern.compile("page2.+= +(\\d+);");
            Pattern totalPages = Pattern.compile("totalPages.+= +(\\d+);");
            Matcher matcher = null;
            Matcher matcher2 = null;
            Matcher matcher3 = null;
            String mailId = "";
            String pageContent = null;
            int mails = 0;
            int cnt = 0;
            int pos = 0;
            int size = 0;
            int index = 0;

            matcher = linkMessage.matcher(post);
            matcher2 = indxField.matcher(post);
            matcher3 = dimension.matcher(post);
            while (matcher.find()) {
                index++;
                mailId = matcher.group(2);
                //mailUrl = matcher.group(1);
                if (index % 2 != 0) {
                    //reperimento della dimesione del messaggio
                    cnt = 0;
                    pos = matcher.end();
                    while (cnt < 3 && matcher2.find(pos)) {
                        cnt++;
                        pos = matcher2.end();
                    }
                    if (matcher3.find(pos)) {
                        try {
                            size = Integer.parseInt(matcher3.group(1));
                        } catch (NumberFormatException nfe) {
                            size = 0;
                        }
                        if (size == 0) {
                            size = 1024;
                        }
                    }
                    if (addEmailInfo(mailId, size)) {
                        mails++;
                    }
                }
            }

            //Paginazione (mailbox.wssp?Mailbox=INBOX&next=1&)
            matcher = page2.matcher(post);
            matcher2 = totalPages.matcher(post);
            if (matcher.find() && matcher2.find()) {
                try {
                    if (Integer.parseInt(matcher.group(1)) < Integer.parseInt(matcher2.group(1))) {
                        lastUrl = lastUrl.substring(0, lastUrl.lastIndexOf("/")) + "/mailbox.wssp?Mailbox=INBOX&next=1&";
                        try {
                            pageContent = getPage(lastUrl, lastGoodCook).toString();
                            mails += extractMail(pageContent);
                        } catch (Throwable t) {
                            log.error("Error paging the inbox");
                            log.error("Error", t);
                        }
                    }
                } catch (NumberFormatException nfe) {
                }
            }
            return mails;
        }

        /* (non-Javadoc)
         * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#getMessage(int, int, boolean)
         */
        @Override
        public String getMessage(int pos, int line, boolean all) {
            String messageId = getMessageID(pos);
            String mailUrl = baseHref + "/Message.wssp?Mailbox=INBOX&MSG=" + messageId;
            String pageData = null;
            String ret = null;
            String body = null;
            String attachName = null;
            Matcher matcher = null;
            POP3Message p3m = new POP3Message();
            Pattern from = Pattern.compile("^\\s*str \\+= '(.+) &lt;(.+)&gt;", Pattern.MULTILINE);
            Pattern subject = Pattern.compile("^\\s*str \\+= '(.*)';", Pattern.MULTILINE);
            Pattern date = Pattern.compile("^\\s*str \\+= '(\\w{3}, \\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} \\+\\d{4})';", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Pattern to = Pattern.compile("^\\s*str \\+= '(.*)';", Pattern.MULTILINE);
            Pattern cc = Pattern.compile("^\\s*str \\+= '(.*)';", Pattern.MULTILINE);
            Pattern textBody = Pattern.compile("(?<=<tt>)(.*)(?=</tt>)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern htmlBody = Pattern.compile("(?<=<tr><td>)(.*)(?=</td></tr>)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern testText = Pattern.compile("(?<=<tr><td><tt>)(.*)(?=</td></tr>)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern attachments = Pattern.compile("<a +href=\"(MessagePart/INBOX/[^/]+/([^\"]+))\">", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            int position = -1;
            int count = 0;
            byte[] attachContent = null;

            log.info("retreiving message " + messageId);

            try {
                pageData = getPage(mailUrl, lastGoodCook).toString();

                //sender
                if ((position = pageData.indexOf("str += 'Da:';")) > -1) {
                    matcher = from.matcher(pageData);
                    if (matcher.find(position)) {
                        p3m.setDa(Converter.unescapeJavascript(Converter.html2TextChar(matcher.group(1))) + " <" + matcher.group(2) + ">");
                        position = matcher.end();
                    }
                }
                //subject
                if ((position = pageData.indexOf("str += 'Oggetto:';", position)) > -1) {
                    matcher = subject.matcher(pageData);
                    count = 0;
                    while (count++ < 3 && matcher.find(position)) {
                        position = matcher.end();
                    }
                    p3m.setOggetto(matcher.group(1));
                }
                //date
                if ((position = pageData.indexOf("str += 'Date:';", position)) > -1) {
                    matcher = date.matcher(pageData);
                    if (matcher.find(position)) {
                        p3m.setData(matcher.group(1));
                        position = matcher.end();
                    }
                }
                //to
                if ((position = pageData.indexOf("str += 'A:';", position)) > -1) {
                    matcher = to.matcher(pageData);
                    count = 0;
                    while (count++ < 3 && matcher.find(position)) {
                        position = matcher.end();
                    }
                    p3m.setA(Converter.unescapeJavascript(Converter.html2TextChar(matcher.group(1))).replaceAll("\\s", " "));
                }
                //cc
                if ((position = pageData.indexOf("str += 'Cc:';", position)) > -1) {
                    matcher = cc.matcher(pageData);
                    count = 0;
                    while (count++ < 3 && matcher.find(position)) {
                        position = matcher.end();
                    }
                    p3m.setCc(Converter.unescapeJavascript(Converter.html2TextChar(matcher.group(1))).replaceAll("\\s", " "));
                }
                //body
                if ((position = pageData.indexOf("<!--%INCLUDE \"MessageTaskBarbottom.wssi\"-->", position)) > -1) {
                    matcher = testText.matcher(pageData);
                    if (matcher.find(position)) {
                        //it's a text message
                        matcher = textBody.matcher(pageData);
                        p3m.setMessageType(POP3Message.TEXT_MESSAGE);
                        if (matcher.find(position)) {
                            body = Converter.html2TextChar(matcher.group(1), true).replaceAll("(?i)<br>", "");
                        }
                    } else {
                        //it's html message
                        p3m.setMessageType(POP3Message.HTML_MESSAGE);
                        matcher = htmlBody.matcher(pageData);
                        if (matcher.find(position + 5)) {
                            body = matcher.group(1);
                            position = matcher.end();
                        }
                    }
                    p3m.setBody(body);
                }
                //attachments
                matcher = attachments.matcher(pageData);
                while (matcher.find(position)) {
                    attachName = Converter.urlUnencode(matcher.group(2));
                    log.info("retrieving attachment: " + attachName);
                    attachContent = getPageBytes(baseHref + "/" + matcher.group(1), lastGoodCook);
                    p3m.addAttach(attachName, attachContent);
                    position = matcher.end();
                }
                ret = p3m.getMessage(line, all);
            } catch (Throwable t) {
                log.error("error retrieving message");
                log.error("Error", t);
            }
            return ret;
        }

        /* (non-Javadoc)
     * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#delMessage(int)
         */
        @Override
        public boolean delMessage(int pos) {
            String messageId = getMessageID(pos);
            String deleUrl = baseHref + "/mailbox.wssp?Mailbox=INBOX&&MSG=" + messageId + "&Delete=&";
            boolean ret = true;

            log.info("deleting message " + messageId);
            try {
                getPage(deleUrl, lastGoodCook).toString();
            } catch (Throwable t) {
                log.error("error moving message " + messageId + " to trash");
                log.error("Error", t);
                ret = false;
            }
            return ret;
        }

        /* (non-Javadoc)
     * @see it.baccan.plugin.pop3.PluginTele2.BoxHandling#delMessagesFromTrash()
         */
        @Override
        public void delMessagesFromTrash() throws DeleteMessageException {
            log.info("deleting messages from trash");
            try {
                getPage(baseHref + "/removeall.wssp?MessageText=Trash&", lastGoodCook);
                getPage(baseHref + "/Mailbox.wssp?Mailbox=Trash&DeleteAll=1&", lastGoodCook);
            } catch (Throwable t) {
                log.error(CLASS_NAME + " Throwable: error deleting messages from the trash");
                log.error("Error", t);
                throw new DeleteMessageException(t.getMessage());
            }
        }

    }

    //MBoxHandling2


    /* (non-Javadoc)
     * @see pop3base#getMessage(int, int, boolean)
     */
    /**
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    @Override
    public String getMessage(int nPos, int nLine, boolean bAll) {
        return boxHandling.getMessage(nPos, nLine, bAll);
    }

    /* (non-Javadoc)
     * @see pop3plugin#login(java.lang.String, java.lang.String)
     */
    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    @Override
    public boolean login(String cUser, String cPwd) {
        String post = "";
        String retFromPost = "";
        String cookBuff = "";
        boolean ret = false;
        boolean err = false;
        byte retry2 = 0;
        int nMail = 0;

        for (int retry = 0; retry < 3; retry++) {
            err = false;
            try {
                log.info("PlugInTele2: login init (" + retry + ") su " + SERVER);

                //prima pagina dove si immettono user e password
                log.info("PlugInTele2: prima pagina");
                retFromPost = filterRem(postPage(SERVER, null, "", lastUrl).toString());
                lastUrl = SERVER;
                cookBuff = null;

                //effettuiamo il post dei dati che ha come action /
                retry2 = 0;
                post = "SessionSkin=it_IT.Tele0Frames"
                        + "&Username=" + URLEncoder.encode(cUser, "UTF-8")
                        + "&Password=" + URLEncoder.encode(cPwd, "UTF-8")
                        + "&SELECTEDLOCALE=it_IT"
                        + "&FRAMES=0"
                        + "&LOCALE=it_IT";
                do {
                    log.info("PlugInTele2: II pagina post dati (" + retry2 + ")");
                    retFromPost = postPage(SERVER, cookBuff, post, lastUrl).toString();
                    lastGoodCook = getCookie().replaceAll("path=/", "");
                    lastUrl = getLocation();

                    //Se ho una stringa vuota, allora ho un errore nella password
                    if (lastUrl.isEmpty()) {
                        return false;
                    }

                    //gestiamo il redirect a manina
                    log.info("PlugInTele2: II pagina I redirect (" + retry2 + ")");
                    secondServer = lastUrl.substring(0, lastUrl.indexOf('/', 8));

                    if (secondServer.equalsIgnoreCase(SERVER)) {
                        //nuova gestione della casella. Adesso hanno due modi diversi di gestire le caselle
                        boxHandling = new MBoxHandling2();
                    } else {
                        boxHandling = new MBoxHandling1();
                    }
                    retFromPost = boxHandling.getLoginResponse(retry2);
                    //riprovo per 5 volte poiché ho notato che spesso dietro a proxies si
                    //mangia i cookies e genera questi errori.
                } while ((!isValidSession(retFromPost)) && (retry2++ < 5));

                if (!isValidSession(retFromPost)) {
                    continue;
                }

                lastUrl = boxHandling.getUrl();

                if (boxHandling.isValidLogin(retFromPost)) {
                    //prelevo l'elenco delle mail gestendo la paginazione
                    log.info("Estrazione numero mail");
                    nMail = boxHandling.extractMail(retFromPost);
                    log.info("PlugInTele2: numeroMail " + nMail);
                    ret = true;
                } else {
                    //LOGIN ERRATO!
                    ret = false;
                }
            } catch (UnsupportedEncodingException uee) {
                log.error("PluginTele2.login() UnsupportedEncodingException: " + uee);
                log.error("Error", uee);
            } catch (Exception e) {
                err = true;
                log.error("PluginTele2.login() Exception: " + e);
                log.error("Error", e);
            } catch (Throwable t) {
                err = true;
                log.error("PluginTele2.login() Throwable: " + t);
                log.error("Error", t);
            }

            if (!err) {
                break;
            } else {
                try {
                    if (retry < 2) {
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                }
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see pop3plugin#delMessage(int)
     */
    /**
     *
     * @param nPos
     * @return
     */
    @Override
    public boolean delMessage(int nPos) {
        return boxHandling.delMessage(nPos);
    }

    /**
     * Return if there could be a session error
     *
     * @param pageContent: the content of the page returned
     * @return true is session is valid, false if is invalid
     */
    private boolean isValidSession(String pageContent) {
        return !((pageContent.indexOf("Unable to find template: \"") > -1)
                || (pageContent.indexOf("<title>Session error</title>") > -1)
                || (pageContent.indexOf("Impossibile trovare il modello: \"") > -1)
                || (pageContent.indexOf("Sessione non valida.") > -1));
    }

    /* (non-Javadoc)
     * @see it.baccan.plugin.pop3.pop3base#delMessagesFromTrash()
     */
    @Override
    public void delMessagesFromTrash() throws DeleteMessageException {
        super.delMessagesFromTrash();
        boxHandling.delMessagesFromTrash();
    }
}
