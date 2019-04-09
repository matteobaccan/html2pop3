/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        NNTP Server
 * Description:  Server NNTP
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3;

import java.net.*;
import java.io.*;
import java.util.*;

import it.baccan.html2pop3.utils.*;
import it.baccan.html2pop3.plugin.nntp.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class NNTPServer extends baseServer {

    private String cLoginString = "200 HTML2POP3 news server (" + version.getVersion() + ") ready";

    /**
     *
     * @param p
     */
    public NNTPServer(html2pop3 p) {
        super(p);
    }

    public void run() {
        nntpThread thread;
        try {
            if (parent.getPortNNTP() > 0) {
                ss = new ServerSocket(parent.getPortNNTP(), parent.getClient(), InetAddress.getByName(parent.getHost()));
                while (true) {
                    // Faccio partire il Thread
                    Socket socket = null;
                    try {
                        // Attendo il client
                        socket = ss.accept();
                    } catch (Throwable e) {
                        if (isFinish) {
                            return;
                        } else {
                            throw e;
                        }
                    }

                    // Metto anche il timeout ai socket 60 minuti
                    socket.setSoTimeout(60 * 60 * 1000);

                    // Aggiungo un keepalive .. per I client pigri
                    setKeepAlive(socket);

                    thread = new nntpThread(socket);
                    thread.start();
                }
            }
        } catch (java.net.BindException be) {
            String cLoginStringFound = EchoClient.getLine(parent.getHost(), parent.getPortNNTP());
            String cError = "Errore! Porta " + parent.getPortNNTP() + " in uso,\nValore corrente (" + cLoginStringFound + ")\nCambiare porta nel config.cfg e fare un restart del server NNTP";

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            log.info(cError.replace('\n', ' '));

            if (cLoginString.equals(cLoginStringFound)) {
                log.info("Exit for double run");
                parent.exitFromProgram();
            } else {
                if (parent.getGuiError()) {
                    //MsgBox message =
                    new MsgBox("HTML2POP3 server NNTP", cError, false);
                }
            }

        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

    class nntpThread extends Thread {

        private Socket socket;

        public nntpThread(Socket socket) {
            this.socket = socket;
        }

        // Inizio thread di gestione del socket
        public void run() {
            try {

                manage(socket);

            } catch (java.net.SocketException se) {

                log.error("NNTP server: chiusura connessione: " + se.getMessage());

            } catch (Throwable e) {

                log.error("Error", e);
                log.info(e.getMessage());
            }

            try {
                socket.close();
            } catch (Throwable e) {
            }
        }

        private void manage(Socket socket) throws Throwable {
            // Source
            InputStream SI = socket.getInputStream();
            OutputStream SO = socket.getOutputStream();

            // Tool
            htmlTool html = new htmlTool();

            String cIP = socket.getInetAddress().getHostAddress();
            // IP Filter
            if (!parent.getNNTPIpFilter().isAllow(new String[]{cIP})) {
                log.error("500 IP (" + cIP + ") deny");
                html.putData(SO, "500 IP (" + cIP + ") deny\r\n");
                return;
            }

            // initial banner
            html.putData(SO, cLoginString + "\r\n");

            String cServer = "nntp";
            nntpbase sp = null;
            if (cServer.equalsIgnoreCase("nntp")) {
                sp = new pluginnntp();
                //} else {
                //log.error( "NNTP server: errore, server mancante." );
                //sp = new pluginnntp();
            }

            boolean bExit = false;

            String cGroup = "";
            //String cArt   = "";
            String cUser = "";
            String cPwd = "";
            // main loop
            while (!bExit) {
                String cLine = html.getLineNOCRLF(SI);
                String cLineUpper = cLine.toUpperCase();

                log.error("NNTP server: " + cLine);
                //12345678901234
                if (cLineUpper.startsWith("AUTHINFO USER")) {
                    if (cLine.length() <= 13) {
                        html.putData(SO, "501 Syntax error in parameters or arguments to AUTHINFO USER command\r\n");
                    } else {
                        cUser = cLine.substring(13).trim();
                        html.putData(SO, "381 More Authentication Required\r\n");
                    }

                    //12345678901234
                } else if (cLineUpper.startsWith("AUTHINFO PASS")) {
                    if (cLine.length() <= 13) {
                        html.putData(SO, "501 Syntax error in parameters or arguments to AUTHINFO PASS command\r\n");
                    } else {
                        cPwd = cLine.substring(13).trim();
                        html.putData(SO, "281 Authentication Accepted\r\n");
                    }

                } else if (cLineUpper.startsWith("MODE READER")) {
                    html.putData(SO, "200 OK\r\n");

                    // Da .CFG con dati fake di 1 msg
                } else if (cLineUpper.startsWith("LIST")) {
                    html.putData(SO, "215 list of newsgroups follows\r\n");
                    sp.streamList(SO);

                    //Vector aGroup = sp.list();
                    //for( int nCur=0; nCur<aGroup.size(); nCur++ ){
                    //html.putData( SO, ((String)aGroup.elementAt( nCur )) +" 00001 00001 n\r\n" );
                    //}
                    html.putData(SO, ".\r\n");

                } else if (cLineUpper.startsWith("GROUP")) {
                    if (cLine.length() <= 5) {
                        html.putData(SO, "411 No Such Group\r\n");
                    } else {
                        cGroup = cLine.substring(5).trim();
                        long[] nMsg = sp.group(cGroup);
                        html.putData(SO, "211 " + (nMsg[1] - nMsg[0]) + " " + nMsg[0] + " " + nMsg[1] + " " + cGroup + " group selected\r\n");
                    }

                } else if (cLineUpper.startsWith("XOVER")) {
                    if (cGroup.length() == 0) {
                        html.putData(SO, "412 No Group Selected\r\n");
                    } else if (cLine.length() <= 5) {
                        html.putData(SO, "500 Syntax error in parameters or arguments to XOVER command\r\n");
                    } else {
                        String cFrom = cLine.substring(5).trim();
                        String cTo = "";
                        int nSub = cFrom.indexOf("-");
                        if (nSub != -1) {
                            if (nSub == cFrom.length() - 1) {
                                cFrom = cFrom.substring(0, nSub);
                                cTo = "-1";
                            } else {
                                cTo = cFrom.substring(nSub + 1);
                                cFrom = cFrom.substring(0, nSub);
                            }
                        } else {
                            cTo = cFrom;
                        }

                        Vector aRet = sp.xover(Double.valueOf(cFrom).longValue(), Double.valueOf(cTo).longValue());

                        html.putData(SO, "224 Overview Information Follows\r\n");
                        for (int nCur = 0; nCur < aRet.size(); nCur++) {
                            html.putData(SO, ((String) aRet.elementAt(nCur)) + "\r\n");
                        }
                        html.putData(SO, ".\r\n");
                    }

                    //12345678
                } else if (cLineUpper.startsWith("ARTICLE")) {
                    if (cLine.length() <= 7) {
                        html.putData(SO, "423 No Such Article In Group\r\n");
                    } else {
                        String cCurArt = cLine.substring(7).trim();
                        String cBody = sp.article(Double.valueOf(cCurArt).longValue());
                        if (cBody == null) {
                            html.putData(SO, "501 Error Retrieving Article In Group\r\n");
                        } else {
                            html.putData(SO, "220 " + cCurArt + "\r\n");
                            html.putData(SO, cBody + "\r\n");
                            html.putData(SO, ".\r\n");
                        }
                    }

                    //12345678
                    //} else if( cLineUpper.startsWith("STAT") ){
                    //if( cLine.length()<=4 ) {
                    //html.putData( SO, "501 Syntax error in parameters or arguments to STAT command\r\n" );
                    //} else {
                    //cArt = cLine.substring(4).trim();
                    //html.putData( SO, "223 " +cArt +" article retrieved - statistics\r\n" );
                    //}
                    //12345678
                    //} else if( cLineUpper.startsWith("HEAD") ){
                    //html.putData( SO, "221 " +cArt +" article retrieved - head\r\n" );
                    //html.putData( SO, "xxxxxxxxxxxxxxx\r\n" );
                    //html.putData( SO, ".\r\n" );
                    //12345678
                    //} else if( cLineUpper.startsWith("BODY") ){
                    //html.putData( SO, "221 " +cArt +" article retrieved - body\r\n" );
                    //html.putData( SO, "xxxxxxxxxxxxxxx\r\n" );
                    //html.putData( SO, ".\r\n" );
                    //12345678
                    //} else if( cLineUpper.startsWith("NEXT") ){
                    //html.putData( SO, "222 " +cArt +" article retrieved - statistics\r\n" );
                    //html.putData( SO, "xxxxxxxxxxxxxxx\r\n" );
                    //html.putData( SO, ".\r\n" );
                } else if (cLineUpper.startsWith("QUIT")) {
                    html.putData(SO, "205 HTML2POP3 QUIT\r\n");
                    bExit = true;

                } else {
                    html.putData(SO, "500 Syntax Error or Unknown Command\r\n");
                }
            }
            //*/
        }

    }
}
