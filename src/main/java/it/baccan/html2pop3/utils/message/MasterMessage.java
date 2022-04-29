package it.baccan.html2pop3.utils.message;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Base64;

/**
 * Abstract base class with usefull method for all the PopMessage
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
public abstract class MasterMessage implements IPopMessage {

    /**
     *
     * @return
     */
    @Override
    public String getMessage() {
        return getMessage(0, true);
    }

    /**
     *
     * @param s
     * @return
     */
    protected String format(String s) {
        String cRet;
        try {
            StringBuilder stringbuffer = new StringBuilder();

            BufferedReader in = new BufferedReader(new StringReader(s));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(".")) {
                    stringbuffer.append(".");
                }
                stringbuffer.append(inputLine + (char) 13 + (char) 10);
            }
            cRet = stringbuffer.toString();
        } catch (Throwable e) {
            cRet = null;
        }
        return cRet;
    }

    /**
     *
     * @param content
     * @return
     */
    protected String get64EncodedAttach(byte[] content) {
        StringBuilder sb = new StringBuilder();
        for (int nBlock = 0; nBlock < content.length; nBlock += 60) {
            int nLen = 60;
            if (nBlock + nLen >= content.length) {
                nLen = content.length - nBlock;
            }
            byte[] buf = new byte[nLen];
            for (int n = 0; n < nLen; n++) {
                buf[n] = content[nBlock + n];
            }
            sb.append(Base64.getEncoder().encodeToString(buf)).append("\r\n");
        }
        return sb.toString();
    }
}
