package it.baccan.html2pop3.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Class used for some conversion
 *
 * @author getting_out <gettingout@linux.it>
 * @version 0.0.1
 *
 * Copyright (C) 2004 Free Software Foundation http://www.fsf.org
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
public class Converter {

    private Converter() {
    }

    private static final String EMPTY_STRING = "";

    /**
     * Convert characters like "&gt;" with the correspective ">"
     *
     * @param htmlString the string with html code
     * @return the string converted
     */
    public static String html2TextChar(String htmlString) {
        return html2TextChar(htmlString, false);
    }

    /**
     *
     * @param htmlString
     * @param checkLinks
     * @return
     */
    public static String html2TextChar(String htmlString, boolean checkLinks) {
        Pattern patSpecialChar = Pattern.compile("&#([\\d]+);", Pattern.MULTILINE);
        String textString = htmlString.replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&amp;", "&")
                .replaceAll("<font[^>]*>", EMPTY_STRING)
                .replace("</font>", EMPTY_STRING)
                .replace("<pre>", EMPTY_STRING)
                .replace("</pre>", EMPTY_STRING)
                .replace("<center>", EMPTY_STRING)
                .replace("</center>", EMPTY_STRING)
                .replace("<small>", EMPTY_STRING)
                .replace("</small>", EMPTY_STRING)
                .replace("<br>", EMPTY_STRING)
                .replaceAll("<hr[^>]*>", EMPTY_STRING);

        //searching for special html char line &#214; and replacing them with 
        //the appropriate char
        Matcher matcher = patSpecialChar.matcher(textString);
        while (matcher.find()) {
            textString = textString.replaceAll(matcher.group(), String.valueOf((char) Integer.parseInt(matcher.group(1))));
            matcher = patSpecialChar.matcher(textString);
        }

        //do I have to trap links too?
        if (checkLinks) {
            Pattern openAElement = Pattern.compile("<a[^>]*>([^><]*)</a>", Pattern.CASE_INSENSITIVE);
            Matcher matchOpenA = openAElement.matcher(textString);

            try {
                while (matchOpenA.find()) {
                    textString = matchOpenA.replaceFirst(matchOpenA.group(1));
                    matchOpenA = openAElement.matcher(textString);
                }
            } catch (IllegalArgumentException iae) {
                log.error("Converter.html2TextChar() Error while processing the link. IllegalArgumentException: " + iae.getMessage());
                //ialog.error("Error",e);
            } catch (IndexOutOfBoundsException iobe) {
                log.error("Converter.html2TextChar() Error while processing the link. IndexOutOfBoundsException: " + iobe.getMessage());
                //ioblog.error("Error",e);
            }
        }
        return textString;
    }

    /**
     * Correct some characters to be correctly viewed in mailer.For example "à"
     * character with "&agrave;"
     *
     * @param cRss
     * @return the string corrected
     */
    public static String htmlCharCorrect(String cRss) {
        cRss = cRss.replace("" + 0xe0, "&agrave;");
        cRss = cRss.replace("" + 0xc0, "&Agrave;");
        cRss = cRss.replace("" + 0xec, "&igrave;");
        cRss = cRss.replace("" + 0xcc, "&Igrave;");
        cRss = cRss.replace("" + 0xe8, "&egrave;");
        cRss = cRss.replace("" + 0xc8, "&Egrave;");
        cRss = cRss.replace("" + 0xf2, "&ograve;");
        cRss = cRss.replace("" + 0xd2, "&Ograve;");
        cRss = cRss.replace("" + 0xf9, "&ugrave;");
        cRss = cRss.replace("" + 0xd9, "&Ugrave;");
        return cRss;
    }

    /**
     * Eliminate the javascript escapes from a string. For example, if the
     * string has "\(", this will be replaced with "("
     *
     * @param s: the string to replace
     * @return the string replaced
     */
    public static String unescapeJavascript(String s) {
        Pattern p = Pattern.compile("\\\\(.)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            s = m.replaceFirst(m.group(1));
            m = p.matcher(s);
        }
        return s;
    }

    /**
     *
     * @param s
     * @return
     */
    public static String urlUnencode(String s) {
        Pattern p = Pattern.compile("%(\\d+)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            s = m.replaceFirst(String.valueOf((char) Integer.parseInt(m.group(1), 16)));
            m = p.matcher(s);
        }
        return s;
    }
}
