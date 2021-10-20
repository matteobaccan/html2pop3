package it.baccan.html2pop3.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Class for testing the application
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
@Slf4j
public class Tester {

    /**
     *
     */
    public static String lastMsg = "";

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        final String ADDRESS = "127.0.0.1";
        final int PORT = 110;

        try (Socket sk = new Socket(ADDRESS, PORT)) {
            try (BufferedOutputStream bos = new BufferedOutputStream(sk.getOutputStream())) {
                try (BufferedInputStream bis = new BufferedInputStream(sk.getInputStream())) {
                    printBisData(bis);

                    //sending user and password
                    printBosData(bos, "user " + args[0] + "\r\n");
                    printBisData(bis);
                    printBosData(bos, "pass " + args[1] + "\r\n");
                    printBisData(bis);

                    //retrieve the first (oldest) message received
                    Pattern p = Pattern.compile("[\\d]+");
                    Matcher m = p.matcher(lastMsg);
                    int retr = 1;
                    if (m.find()) {
                        retr = Integer.parseInt(m.group());
                    }

                    //sending retr
                    printBosData(bos, "retr " + retr + "\r\n");
                    printBisData(bis);
                    printBisData(bis);
                    printBosData(bos, "quit\r\n");
                }
            }
        } catch (UnknownHostException uhe) {
            log.error("Error", uhe);
        } catch (IOException ioe) {
            log.error("Error", ioe);
        }

        log.info("Done!");
    }

    /**
     *
     * @param bis
     * @throws IOException
     */
    public static void printBisData(BufferedInputStream bis) throws IOException {
        final int NEW_LINE = 0x01;
        final int NEW_LINE_COMPLETE = 0x02;
        int c = 0x00;
        int currState = 0x00;

        lastMsg = "";
        while ((currState != NEW_LINE_COMPLETE) && (c != -1)) {
            c = bis.read();
            if (c == 13 || c == 10) {
                currState += NEW_LINE;
            }
            System.out.print((c != -1) ? (char) c : '\0');
            lastMsg += (char) c;
        }

    }

    /**
     *
     * @param bos
     * @param data
     * @throws IOException
     */
    public static void printBosData(BufferedOutputStream bos, String data) throws IOException {
        bos.write(data.getBytes());
        bos.flush();
        System.out.print((data.startsWith("pass")) ? "pass *\r\n" : data);
    }
}
