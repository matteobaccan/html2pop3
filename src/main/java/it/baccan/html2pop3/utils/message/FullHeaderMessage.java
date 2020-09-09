package it.baccan.html2pop3.utils.message;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.baccan.html2pop3.utils.ContentType;
import java.util.Map;

/**
 * Class that rappresent a pop3 message characterized by full pop headers, body
 * and eventually attachment
 *
 * @author gettingout <gettingout@linux.it>
 * @version 1.0.0
 *
 * Copyright (C) 2004 gettingout <gettingout@linux.it>
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
public class FullHeaderMessage extends MasterMessage {

    private String headers = null;
    private String body = null;
    private String boundary = null;
    private String charset = null;
    private String encoding = null;
    private Map<String, byte[]> attachments = null;

    /**
     *
     * @param headers
     * @param body
     */
    public FullHeaderMessage(String headers, String body) {
        Pattern patBoundary = Pattern.compile("boundary=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern patCharset = Pattern.compile("charset=([\\da-zA-Z\\-&&[\\S]]*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern patEncoding = Pattern.compile("^\\s*Content-Transfer-Encoding:[\\s&&[^\\da-zA-Z\\-]]*([\\da-zA-Z\\-&&[\\S]]*).*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher;

        this.headers = headers;
        this.body = format(body);
        attachments = new HashMap<>();

        //is there a boundary? what is it?
        matcher = patBoundary.matcher(headers);
        if (matcher.find()) {
            boundary = matcher.group(1);
        }

        //is there a charset coding? what is it?
        matcher = patCharset.matcher(headers);
        if (matcher.find()) {
            charset = matcher.group(1);
        } else {
            charset = ISO_8859_1;
        }

        //and what about the encoding?
        matcher = patEncoding.matcher(headers);
        if (matcher.find()) {
            encoding = matcher.group(1);
        } else {
            encoding = ENC_8_BIT;
        }
    }

    /**
     *
     * @param headers
     * @param body
     * @param attachments
     */
    public FullHeaderMessage(String headers, String body, Map<java.lang.String,byte[]> attachments) {
        this(headers, body);
        this.attachments = attachments;
    }

    /* (non-Javadoc)
	 * @see it.baccan.utils.message.IPopMessage#getMessage(int, boolean)
     */
    /**
     *
     * @param line
     * @param all
     * @return
     */
    @Override
    public String getMessage(int line, boolean all) {
        StringBuilder sb = new StringBuilder();
        Iterator iterator;
        String fileName;
        byte[] content;

        sb.append(headers).append("\r\n");

        if (boundary != null) {
            sb.append("\r\n--").append(boundary).append("\r\n");
            sb.append("Content-Type: text/plain; charset=\"").append(charset).append("\"\r\n");
            sb.append("Content-Transfer-Encoding: ").append(encoding).append("\r\n");
        }
        sb.append("\r\n").append(body);

        if ((attachments != null) && (attachments.size() > 0)) {
            iterator = attachments.keySet().iterator();
            while (iterator.hasNext()) {
                fileName = (String) iterator.next();
                content = attachments.get(fileName);

                sb.append("\r\n--").append(boundary).append("\r\n");
                sb.append("Content-Type: ").append(ContentType.getInstance().getFromFilename(fileName)).append(";\r\n");
                sb.append("      name=\"").append(fileName).append("\"\r\n");
                sb.append("Content-Transfer-Encoding: " + ENC_BASE_64 + "\r\n");
                sb.append("Content-Disposition: attachment;\r\n");
                sb.append("      filename=\"").append(fileName).append("\"\r\n\r\n");
                sb.append(get64EncodedAttach(content));
                sb.append("\r\n");
            }
        }

        if (boundary != null) {
            sb.append("--").append(boundary).append("--\r\n\r\n");
        }
        return sb.toString();
    }

    /**
     *
     * @param fileName
     * @param contentAttach
     *
     * @see it.baccan.utils.message.IPopMessage#addAttach(java.lang.String,
     * byte[])
     */
    @Override
    public void addAttach(String fileName, byte[] contentAttach) {
        attachments.put(fileName, contentAttach);
    }
}
