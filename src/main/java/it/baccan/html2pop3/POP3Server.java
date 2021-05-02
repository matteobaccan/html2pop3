/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        POP3 Server
 * Description:  Server POP3
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3;

import it.baccan.html2pop3.exceptions.DeleteMessageException;
import it.baccan.html2pop3.plugin.pop3.POP3Plugin;
import it.baccan.html2pop3.utils.EchoClient;
import it.baccan.html2pop3.utils.MsgBox;
import it.baccan.html2pop3.utils.HTMLTool;
import it.baccan.html2pop3.utils.POP3Selector;
import it.baccan.html2pop3.utils.Version;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class POP3Server extends BaseServer {

    private String cLoginString = "+OK HTML2POP3 server ready (" + Version.getVersion() + ")";
    private final Properties tunnelpop3 = new Properties();

    /**
     *
     * @param html2pop3
     */
    public POP3Server(HTML2POP3 html2pop3) {
        super(html2pop3);

        String cPath = html2pop3.getConfigPath();
        String cConfig = "tunnelpop3.cfg";
        try {
            try (FileInputStream fis = new FileInputStream(cPath + cConfig)) {
                tunnelpop3.load(fis);
            }

            tunnelpop3.entrySet().forEach(action -> {
                if (POP3Selector.server2POP3Plugin(action.getKey().toString()) != null) {
                    log.error("Rimuovere il server [{}] da [{}]", action.getKey(), cConfig);
                }
            });
        } catch (FileNotFoundException fnf) {
            log.info("Non riesco a leggere il file " + cPath + cConfig);
        } catch (IOException e) {
            log.error("Error", e);
        }
    }

    private boolean isTunnel(String cEmail) {
        return (getTunnelConversion(cEmail).length() > 0);
    }

    private String getTunnelConversion(final String fullEmail) {
        String ret = "";

        String email = fullEmail;
        // Stacco la parte @qualcosa e la cerco nel config
        int nPos = email.indexOf("@");
        if (nPos != -1) {
            String cUser = email.substring(0, nPos);
            email = email.substring(nPos + 1).toLowerCase();
            ret = tunnelpop3.getProperty(email, "");
            if (ret.length() > 0) {
                ret = ret.replace("%email%", cUser + "@" + email);
                ret = ret.replace("%user%", cUser);
            }
        }
        return ret;
    }

    @Override
    public void run() {
        pop3Thread thread;
        try {
            if (getParent().getPort() > 0) {
                setServerSocket(new ServerSocket(getParent().getPort(), getParent().getClient(), InetAddress.getByName(getParent().getHost())));
                while (true) {
                    // Faccio partire il Thread
                    Socket socket = null;
                    try {
                        // Attendo il client
                        socket = getServerSocket().accept();
                    } catch (Throwable e) {
                        if (isFinish()) {
                            return;
                        } else {
                            throw e;
                        }
                    }

                    // Metto anche il timeout ai socket 60 minuti
                    socket.setSoTimeout(60 * 60 * 1000);

                    // Aggiungo un keepalive .. per I client pigri
                    setKeepAlive(socket);

                    thread = new pop3Thread(socket);
                    thread.start();
                }
            }
        } catch (BindException be) {
            String cLoginStringFound = EchoClient.getLine(getParent().getHost(), getParent().getPort());
            String cError = "Errore! Porta " + getParent().getPort() + " in uso,\nValore corrente (" + cLoginStringFound + ")\nCambiare porta nel config.cfg e fare un restart del server POP3";

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            log.info(cError.replace('\n', ' '));

            if (cLoginString.equals(cLoginStringFound)) {
                log.info("Exit for double run");
                getParent().exitFromProgram();
            } else {
                if (getParent().isGuiError()) {
                    new MsgBox("HTML2POP3 server POP3", cError, false);
                }
            }

        } catch (Throwable e) {
            log.error("Error", e);
            log.error(e.getMessage());
        }
    }

    class pop3Thread extends Thread {

        private final Socket socket;
        private POP3Plugin pop3Plugin = null;

        public pop3Thread(Socket s) {
            socket = s;
        }

        // Inizio thread di gestione del socket
        @Override
        public void run() {
            try {
                manage(socket);
            } catch (SocketException se) {
                log.error("POP3 server: chiusura connessione: " + se.getMessage());
            } catch (Throwable e) {
                log.error("Error", e);
            }

            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }

        private void manage(Socket socket) throws Throwable {
            // DEBUG
            boolean bDebug = getParent().isDebug();
            // Source
            InputStream SI = socket.getInputStream();
            OutputStream SO = socket.getOutputStream();

            // Tool
            HTMLTool html = new HTMLTool();
            html.setDebug(bDebug);

            String cIP = socket.getInetAddress().getHostAddress();
            // IP Filter
            if (!getParent().getPOP3IpFilter().isAllow(new String[]{cIP})) {
                log.error("-ERR IP (" + cIP + ") deny");
                html.putData(SO, "-ERR IP (" + cIP + ") deny\r\n");
                return;
            }

            // initial banner
            html.putData(SO, cLoginString + "\r\n");

            boolean bExit = false;
            String cUser = "";
            String cPassword = "";
            ArrayList<Double> aDel = new ArrayList<>();

            pop3Plugin = null;

            // main loop
            while (!bExit) {
                String cLine = html.getLineNOCRLF(SI);
                String cLineUpper = cLine.toUpperCase();

                if (cLineUpper.startsWith("PASS")) {
                    if (bDebug) {
                        log.debug("POP3 server: " + cLine);
                    } else {
                        // Ma e' possibile che le persone vedendo 3 * pensino di avere la password di
                        // 3 caratteri???????
                        // Vediamo se cosi piace di piu'
                        String c = "PASS ";
                        while (c.length() < cLine.length()) {
                            c += "*";
                        }

                        // Aggiungo il CRC32 per capire, in caso di piu' login, se la password che viene
                        // inviata e' sempre la stessa
                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        crc.update(cLine.getBytes(), 0, cLine.length());
                        c += " CRC32:" + crc.getValue();

                        log.info("POP3 server: " + c);
                    }
                } else {
                    if (cLine.length() > 0 || bDebug) {
                        log.debug("POP3 server: " + cLine);
                    }
                }

                if (cLineUpper.startsWith("USER")) {
                    if (cLine.length() < 6) {
                        html.putData(SO, "-ERR Protocol error\r\n");
                    } else {
                        cUser = cLine.substring(5);
                        html.putData(SO, "+OK Password required\r\n");
                    }

                } else if (cLineUpper.startsWith("PASS")) {
                    if (cUser.length() == 0) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        cPassword = "";
                        if (cLine.length() > 4) {
                            cPassword = cLine.substring(5);
                        }

                        pop3Plugin = null;
                        int nServer = cUser.indexOf(";");
                        String cServer = "";
                        if (nServer != -1) {
                            cServer = cUser.substring(0, nServer);
                            cUser = cUser.substring(nServer + 1).trim();
                        } else {
                            if (isTunnel(cUser)) {
                                // Convert generic to valid emil
                                cUser = getTunnelConversion(cUser);
                                // Strip server
                                nServer = cUser.indexOf(";");
                                if (nServer != -1) {
                                    cServer = cUser.substring(0, nServer);
                                    cUser = cUser.substring(nServer + 1).trim();
                                } else {
                                    cServer = "pop3";
                                }
                                log.info("POP3 server: tunneling mode. User converted to " + cUser);

                                // Per gli utenti pigri
                            } else {
                                cServer = POP3Selector.user2Server(cUser);
                            }
                        }

                        log.info("POP3 server: usato " + cServer);

                        // Plugin Filter
                        if (!getParent().getPOP3PluginFilter().isAllow(new String[]{cServer})) {
                            log.error("-ERR plugin (" + cServer + ") deny");
                            html.putData(SO, "-ERR plugin (" + cServer + ") deny\r\n");
                            return;
                        }

                        // User Filter
                        if (!getParent().getPOP3UserFilter().isAllow(new String[]{cUser})) {
                            log.error("-ERR user (" + cUser + ") deny");
                            html.putData(SO, "-ERR user (" + cUser + ") deny\r\n");
                            return;
                        }

                        // Global Filter
                        if (!getParent().getPOP3GlobalFilter().isAllow(new String[]{cIP, cServer, cUser})) {
                            log.error("-ERR global (" + cIP + ")(" + cServer + ")(" + cUser + ") deny");
                            html.putData(SO, "-ERR global (" + cIP + ")(" + cServer + ")(" + cUser + ") deny\r\n");
                            return;
                        }

                        pop3Plugin = POP3Selector.server2POP3Plugin(cServer);

                        if (pop3Plugin != null) {
                            // Now I can login on server, depending on requested server
                            pop3Plugin.setMaxMessageNum(getParent().getMaxEmail());
                            // Set Debug
                            pop3Plugin.setDebug(getParent().isDebug());

                            boolean bLogin = pop3Plugin.login(cUser, cPassword);

                            if (bLogin) {
                                if (pop3Plugin.list()) {
                                    //if( getParent().getMaxEmail()!=-1 ) hp.setMessageNum(getParent().getMaxEmail());
                                    html.putData(SO, "+OK " + pop3Plugin.getMessageNum() + " messages\r\n");
                                    if (!getParent().getLifo()) {
                                        pop3Plugin.invertSort();
                                    }
                                    log.info(cServer + ": Trovati " + pop3Plugin.getMessageNum() + " messaggi");
                                } else {
                                    html.putData(SO, "-ERR errore durante la creazione della lista email\r\n");
                                }
                            } else {
                                if (pop3Plugin.getLastErr().length() > 0) {
                                    html.putData(SO, "-ERR " + pop3Plugin.getLastErr() + "\r\n");
                                } else {
                                    html.putData(SO, "-ERR invalid password\r\n");
                                }
                                // Esce 1.10, prima faceva dei loop strani
                                bExit = true;
                            }
                        } else {
                            log.error("POP3 server: errore, server (" + cServer + ") non implementato al momento.");
                            log.error("Controllare che la sintassi sia server;username. EX libero.it;pippo@libero.it");
                            log.error("Eventualmente controllare su https://www.baccan.it che non sia stato rilasciato un aggiornamento che implementa tale server");
                            html.putData(SO, "-ERR invalid configuration. Read manual for correct user setting\r\n");
                            bExit = true;
                        }
                    }

                } else if (cLineUpper.startsWith("TOP")) {
                    int nPos = cLine.indexOf(" ", 4);
                    if (nPos == -1) {
                        html.putData(SO, "-ERR Protocol error\r\n");
                    } else {
                        if (pop3Plugin == null) {
                            html.putData(SO, "-ERR Command is not valid in this state\r\n");
                        } else {
                            int nMsg = Double.valueOf(cLine.substring(4, nPos).trim()).intValue();
                            int nLine = Double.valueOf(cLine.substring(nPos).trim()).intValue();
                            if (!pop3Plugin.streamMessageTop(SO, nMsg, nLine)) {
                                String cMessage = pop3Plugin.getMessageTop(nMsg, nLine);
                                if (cMessage != null) {
                                    cMessage = "+OK top of message follows\r\n" + cMessage + ".\r\n";
                                    html.putData(SO, cMessage);
                                } else {
                                    html.putData(SO, "-ERR no such message\r\n");
                                }
                            }
                        }
                    }

                } else if (cLineUpper.startsWith("APOP")) {
                    html.putData(SO, "-ERR permission denied\r\n");

                } else if (cLineUpper.startsWith("AUTH")) {
                    if (cLine.length() == 4) {
                        html.putData(SO, "+OK\r\n");
                        html.putData(SO, ".\r\n");
                    } else {
                        html.putData(SO, "-ERR The specified authentication package is not supported\r\n");
                    }

                } else if (cLineUpper.startsWith("NOOP")) {
                    // RFC 1939 compliant
                    if (pop3Plugin == null) {
                        html.putData(SO, "-ERR unrecognized POP3 command or wrong state\r\n");
                    } else {
                        html.putData(SO, "+OK\r\n");
                    }

                } else if (cLineUpper.startsWith("RSET")) {
                    // Remove delete flag
                    aDel = new ArrayList<>();
                    html.putData(SO, "+OK\r\n");

                } else if (cLineUpper.startsWith("STAT")) {
                    // Di default e' attivo, opzionalmente si puo' staccare
                    if (getParent().isOutlook2002Timeout()) {
                        // Sleep for 1 second
                        // Questa riga sembra stupida e inutile .. vero .. ma Outlook 2002, se gli si risponde
                        // troppo presto ... non riesce a prendere la risposta e a gestirla .. si perde in non
                        // so quali meandri del suo codice .. rendendo html2pop3 inutilizzabile .. mammamia
                        // mi sono ridotto a correggere gli errori degli altri
                        //
                        // Info
                        // http://groups.google.it/groups?q=+Outlook+2002+stat&hl=it&lr=&ie=UTF-8&selm=OhQISjVdCHA.1300%40tkmsftngp08&rnum=2
                        //
                        // Inoltre, come detto da MS, il fix al problema e' nel SP3 di Outlook 2003
                        // http://groups.google.it/groups?hl=it&lr=&ie=UTF-8&selm=uS3M6VYAEHA.444%40TK2MSFTNGP11.phx.gbl
                        //
                        try {
                            Thread.sleep(1000);
                        } catch (Throwable ex) {
                        }
                        // ----------------------
                    }

                    if (pop3Plugin == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        // Non complinant al 100%
                        // Note that messages marked as deleted are not counted in either total.
                        // Minimal implementations should just end that line of the response with a CRLF pair
                        String cMessage = "+OK " + pop3Plugin.getMessageNum() + " " + pop3Plugin.getMessageSize();
                        html.putData(SO, cMessage + "\r\n");
                        if (bDebug) {
                            log.info(cMessage);
                        }
                    }

                } else if (cLineUpper.startsWith("STLS")) {
                    html.putData(SO, "-ERR Protocol error\r\n");

                    //} else if( cLineUpper.startsWith("CAPA") ){
                    //html.putData( SO, "+OK Capability list follows\r\n" );
                    //html.putData( SO, "TOP\r\n" );
                    //html.putData( SO, "USER\r\n" );
                    //html.putData( SO, "UIDL\r\n" );
                    //html.putData( SO, "SASL PLAIN LOGIN\r\n" );
                    //html.putData( SO, ".\r\n" );
                    //} else if( cLineUpper.startsWith("STLS") ){
                    //html.putData( SO, "+OK Begin SSL/TLS negotiation now.\r\n" );
                } else if (cLineUpper.startsWith("LIST")) {
                    if (pop3Plugin == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        if (cLine.length() > 5) {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            int nNum = pop3Plugin.getMessageNum();
                            if (nMsg > nNum) {
                                html.putData(SO, "-ERR no such message, only " + nNum + " messages in maildrop\r\n");
                            } else {
                                html.putData(SO, "+OK " + nMsg + " " + pop3Plugin.getMessageSize(nMsg) + "\r\n");
                            }

                        } else {
                            html.putData(SO, "+OK\r\n");
                            int nNum = pop3Plugin.getMessageNum();
                            for (int nPos = 1; nPos <= nNum; nPos++) {
                                html.putData(SO, "" + nPos + " " + pop3Plugin.getMessageSize(nPos) + "\r\n");
                            }
                            html.putData(SO, ".\r\n");
                        }
                    }

                } else if (cLineUpper.startsWith("UIDL")) {
                    if (pop3Plugin == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        if (cLine.length() > 5) {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            int nNum = pop3Plugin.getMessageNum();
                            if (nMsg > nNum) {
                                html.putData(SO, "-ERR no such message, only " + nNum + " messages in maildrop\r\n");
                            } else {
                                html.putData(SO, "+OK " + nMsg + " " + pop3Plugin.getMessageID(nMsg) + "\r\n");
                            }

                        } else {
                            html.putData(SO, "+OK\r\n");
                            int nNum = pop3Plugin.getMessageNum();
                            for (int nPos = 1; nPos <= nNum; nPos++) {
                                String cMessage = "" + nPos + " " + pop3Plugin.getMessageID(nPos);
                                html.putData(SO, cMessage + "\r\n");
                                if (bDebug) {
                                    log.info(cMessage);
                                }
                            }

                            html.putData(SO, ".\r\n");
                        }
                    }

                } else if (cLineUpper.startsWith("RETR")) {   //RETR num
                    if (cLine.length() == 4) {
                        html.putData(SO, "-ERR Protocol error\r\n");
                    } else {
                        if (pop3Plugin == null) {
                            html.putData(SO, "-ERR Command is not valid in this state\r\n");
                        } else {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            if (!pop3Plugin.streamMessage(SO, nMsg)) {
                                String cMessage = pop3Plugin.getMessage(nMsg);
                                if (cMessage != null) {
                                    html.putData(SO, "+OK " + cMessage.length() + 2 + " bytes\r\n");
                                    html.putData(SO, cMessage + "\r\n");
                                    html.putData(SO, ".\r\n");
                                } else {
                                    html.putData(SO, "-ERR error retrieving message " + nMsg + "\r\n");
                                }
                            }
                        }
                    }

                } else if (cLineUpper.startsWith("DELE")) {   //DELE num
                    if (cLine.length() == 4) {
                        html.putData(SO, "-ERR Protocol error\r\n");
                    } else {//getParent().setDelete(true);
                        if (getParent().isDelete()) {
                            Double nMsg = Double.valueOf(cLine.substring(4).trim());
                            boolean bDel = true;
                            for (int nCur = 0; nCur < aDel.size(); nCur++) {
                                Double n = aDel.get(nCur);
                                if (n.equals(nMsg)) {
                                    bDel = false;
                                }
                            }

                            if (bDel) {
                                aDel.add(nMsg);
                                html.putData(SO, "+OK message marked for deletion\r\n");
                            } else {
                                html.putData(SO, "-ERR already deleted\r\n");
                            }
                        } else {
                            html.putData(SO, "-ERR delete disabled\r\n");
                        }
                    }

                } else if (cLineUpper.startsWith("QUIT")) {
                    bExit = true;

                    // Ottimizzata, messaggio e chiusura subito, poi eventuale delete
                    if (getParent().isDeleteOptimized()) {
                        html.putData(SO, "+OK POP3 server closing connection\r\n");
                        try {
                            socket.close();
                        } catch (Throwable e) {
                        }
                    }

                    if (pop3Plugin != null) {
                        // Cancello se devo cancellare
                        if (getParent().isDelete()) {
                            if (!aDel.isEmpty()) {
                                log.info("Start removing deleted message from queue ...");
                            }
                            pop3Plugin.delMessageStart();
                            for (int nCur = 0; nCur < aDel.size(); nCur++) {
                                int nMsg = aDel.get(nCur).intValue();
                                if (pop3Plugin.delMessage(nMsg)) {
                                    log.info("POP3 server: deleted " + nMsg);
                                } else {
                                    log.error("POP3 server: error deleting " + nMsg);
                                }
                            }

                            // Pulisco il cestino, solo se ho cancellato qualcosa
                            if (!aDel.isEmpty()) {
                                try {
                                    pop3Plugin.delMessagesFromTrash();
                                } catch (DeleteMessageException dme) {
                                    log.error("POP3 server: Error while emptying web trash. DeleteMessageException: [{}]", dme.getMessage());
                                }
                            }

                            pop3Plugin.delMessageEnd();
                            if (!aDel.isEmpty()) {
                                log.info("End removing deleted message from queue ...");
                            }
                        } else {
                            if (!aDel.isEmpty()) {
                                log.info("POP3 server: delete disabled");
                            }
                        }
                    }

                    // NON ottimizzata .. messaggio dopo e chiusura dopo
                    if (!getParent().isDeleteOptimized()) {
                        html.putData(SO, "+OK POP3 server closing connection\r\n");
                    }

                } else {
                    html.putData(SO, "-ERR Syntax Error or Unknown Command\r\n");
                }
            }
        }

    }
}
