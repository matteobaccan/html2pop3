package it.baccan.html2pop3.utils;

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
import java.io.*;
import java.net.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class EchoClient {

    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        EchoClient ec = new EchoClient();
        log.info(ec.getLine("localhost", 110));
        log.info(ec.getLine("localhost", 25));
        log.info(ec.getLine("localhost", 119));
    }

    /**
     *
     * @param cHost
     * @param nPort
     * @return
     */
    public static String getLine(String cHost, int nPort) {
        String cRet;
        
        try (Socket echoSocket = new Socket(cHost, nPort)) {
            echoSocket.setSoTimeout(5000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))) {
                cRet = in.readLine();
            }
        } catch (UnknownHostException e) {
            cRet = "Don't know about host: " + cHost + ":" + nPort;
        } catch (IOException e) {
            cRet = "Couldn't get I/O for the connection to: " + cHost + ":" + nPort;
        }

        return cRet;
    }
}
