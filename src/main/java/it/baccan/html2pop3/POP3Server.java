/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
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
import it.baccan.html2pop3.plugin.pop3.PluginLinuxIt;
import it.baccan.html2pop3.plugin.pop3.PluginTele2;
import it.baccan.html2pop3.plugin.pop3.plugingmail;
import it.baccan.html2pop3.plugin.pop3.pluginhotmail;
import it.baccan.html2pop3.plugin.pop3.pluginlibero;
import it.baccan.html2pop3.plugin.pop3.plugintiscali;
import it.baccan.html2pop3.plugin.pop3.PluginTin;
import it.baccan.html2pop3.plugin.pop3.pluginfastwebnet;
import it.baccan.html2pop3.plugin.pop3.pluginrss;
import it.baccan.html2pop3.plugin.pop3.plugininfinito;
import it.baccan.html2pop3.plugin.pop3.pluginvirgilio;
import it.baccan.html2pop3.plugin.pop3.plugintim;
import it.baccan.html2pop3.plugin.pop3.pluginsupereva;
import it.baccan.html2pop3.plugin.pop3.pluginpop3;
import it.baccan.html2pop3.utils.EchoClient;
import it.baccan.html2pop3.utils.MsgBox;
import it.baccan.html2pop3.utils.htmlTool;
import it.baccan.html2pop3.utils.string;
import it.baccan.html2pop3.utils.version;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class POP3Server extends baseServer {

    private String cLoginString = "+OK HTML2POP3 server ready (" + version.getVersion() + ")";
    private Properties config = new Properties();

    /**
     *
     * @param p
     */
    public POP3Server(html2pop3 p) {
        super(p);

        String cPath = p.getConfigPath();
        String cConfig = "tunnelpop3.cfg";
        try {
            try (FileInputStream fis = new FileInputStream(cPath + cConfig)) {
                config.load(fis);
            }
        } catch (FileNotFoundException fnf) {
            log.info("Non riesco a leggere il file " + cPath + cConfig);
        } catch (IOException e) {
            log.error("Error", e);
        }
    }

    private boolean isTunnel(String cEmail) {
        return (getTunnelConversion(cEmail).length() > 0);
    }

    private String getTunnelConversion(String cEmail) {
        String cRet = "";

        // Stacco la parte @qualcosa e la cerco nel config
        int nPos = cEmail.indexOf("@");
        if (nPos != -1) {
            String cUser = cEmail.substring(0, nPos);
            cEmail = cEmail.substring(nPos + 1).toLowerCase();
            cRet = config.getProperty(cEmail, "");
            if (cRet.length() > 0) {
                cRet = string.replace(cRet, "%email%", cUser + "@" + cEmail);
                cRet = string.replace(cRet, "%user%", cUser);
            }
        }

        return cRet;
    }

    public void run() {
        pop3Thread thread;
        try {
            if (parent.getPort() > 0) {
                ss = new ServerSocket(parent.getPort(), parent.getClient(), InetAddress.getByName(parent.getHost()));
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

                    thread = new pop3Thread(socket);
                    thread.start();
                }
            }
        } catch (java.net.BindException be) {
            String cLoginStringFound = EchoClient.getLine(parent.getHost(), parent.getPort());
            String cError = "Errore! Porta " + parent.getPort() + " in uso,\nValore corrente (" + cLoginStringFound + ")\nCambiare porta nel config.cfg e fare un restart del server POP3";

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
        private POP3Plugin hp = null;

        public pop3Thread(Socket socket) {
            this.socket = socket;
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

        /**
         * @conditional (JVM14)
         */
        private void getTele2() {
            hp = new PluginTele2();
        }

        /**
         * @conditional (JVM14)
         */
        private void getHotmail() {
            hp = new pluginhotmail();
        }

        /**
         * @conditional (JVM14)
         */
        private void getGmail() {
            hp = new plugingmail();
        }

        /**
         * @conditional (JVM14)
         */
        private void getLinuxIt() {
            hp = new PluginLinuxIt();
        }

        private void manage(Socket socket) throws Throwable {
            // DEBUG
            boolean bDebug = parent.getDebug();
            // Source
            InputStream SI = socket.getInputStream();
            OutputStream SO = socket.getOutputStream();

            // Tool
            htmlTool html = new htmlTool();
            html.setDebug(bDebug);

            String cIP = socket.getInetAddress().getHostAddress();
            // IP Filter
            if (!parent.getPOP3IpFilter().isAllow(new String[]{cIP})) {
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

            hp = null;

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

                        hp = null;
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
                            } else if (cUser.toUpperCase().indexOf("@LIBERO.IT") != -1) {
                                cServer = "libero.it";
                            } else if (cUser.toUpperCase().indexOf("@INWIND.IT") != -1) {
                                cServer = "inwind.it";
                            } else if (cUser.toUpperCase().indexOf("@BLU.IT") != -1) {
                                cServer = "blu.it";
                            } else if (cUser.toUpperCase().indexOf("@GIALLO.IT") != -1) {
                                cServer = "giallo.it";
                            } else if (cUser.toUpperCase().indexOf("@IOL.IT") != -1) {
                                cServer = "iol.it";
                            } else if (cUser.toUpperCase().indexOf("@INFINITO.IT") != -1) {
                                cServer = "infinito.it";
                            } else if (cUser.toUpperCase().indexOf("@GENIE.IT") != -1) {
                                cServer = "infinito.it";
                            } else if (cUser.toUpperCase().indexOf("@TISCALI.IT") != -1) {
                                cServer = "tiscali.it";
                            } else if (cUser.toUpperCase().indexOf("@FASTWEBNET.IT") != -1) {
                                cServer = "fastwebnet.it";
                            } else if (cUser.toUpperCase().indexOf("@TIN.IT") != -1
                                    || cUser.toUpperCase().indexOf("@ATLANTIDE.IT") != -1
                                    || cUser.toUpperCase().indexOf("@TIM.IT") != -1) {
                                cServer = "tin.it";
                            } else if (cUser.toUpperCase().indexOf("@VIRGILIO.IT") != -1) {
                                cServer = "virgilio.it";
                            } else if (cUser.toUpperCase().indexOf("@HOTMAIL.COM") != -1) {
                                cServer = "hotmail.com";
                            } else if (cUser.toUpperCase().indexOf("@SUPEREVA.IT") != -1
                                    || cUser.toUpperCase().indexOf("@FREEMAIL.IT") != -1
                                    || cUser.toUpperCase().indexOf("@FREEWEB.ORG") != -1
                                    || cUser.toUpperCase().indexOf("@SUPERSONIC.IT") != -1
                                    || //
                                    cUser.toUpperCase().indexOf("@DADACASA.COM") != -1
                                    || //
                                    cUser.toUpperCase().indexOf("@CONCENTO.IT") != -1
                                    || //
                                    cUser.toUpperCase().indexOf("@CLARENCE.COM") != -1
                                    || //
                                    cUser.toUpperCase().indexOf("@CICCIOCICCIO.COM") != -1
                                    || cUser.toUpperCase().indexOf("@MYBOX.IT") != -1
                                    || cUser.toUpperCase().indexOf("@MP4.IT") != -1
                                    || cUser.toUpperCase().indexOf("@SUPERDADA.COM") != -1) {
                                cServer = "supereva.it";
                            } else if (cUser.toUpperCase().indexOf("@TELE2.IT") != -1) {
                                cServer = "tele2.it";
                            } else if (cUser.toUpperCase().indexOf("@GMAIL.COM") != -1) {
                                cServer = "gmail.com";
                            } else if (cUser.toUpperCase().indexOf("@RSS") != -1) {
                                cServer = "rss";
                            } else if (cUser.toUpperCase().indexOf("@AGGREGATOR") != -1) {
                                cServer = "rss";
                            } else if (cUser.toUpperCase().indexOf("@LINUX.IT") != -1) {
                                cServer = "linux.it";
                            } else {
                                cServer = "libero.it";
                            }
                        }

                        log.info("POP3 server: usato " + cServer);

                        // Plugin Filter
                        if (!parent.getPOP3PluginFilter().isAllow(new String[]{cServer})) {
                            log.error("-ERR plugin (" + cServer + ") deny");
                            html.putData(SO, "-ERR plugin (" + cServer + ") deny\r\n");
                            return;
                        }

                        // User Filter
                        if (!parent.getPOP3UserFilter().isAllow(new String[]{cUser})) {
                            log.error("-ERR user (" + cUser + ") deny");
                            html.putData(SO, "-ERR user (" + cUser + ") deny\r\n");
                            return;
                        }

                        // Global Filter
                        if (!parent.getPOP3GlobalFilter().isAllow(new String[]{cIP, cServer, cUser})) {
                            log.error("-ERR global (" + cIP + ")(" + cServer + ")(" + cUser + ") deny");
                            html.putData(SO, "-ERR global (" + cIP + ")(" + cServer + ")(" + cUser + ") deny\r\n");
                            return;
                        }

                        if (cServer.equalsIgnoreCase("libero.it")) {
                            hp = new pluginlibero(pluginlibero.MAIL_LIBERO);
                        } else if (cServer.equalsIgnoreCase("inwind.it")) {
                            hp = new pluginlibero(pluginlibero.MAIL_INWIND);
                        } else if (cServer.equalsIgnoreCase("blu.it")) {
                            hp = new pluginlibero(pluginlibero.MAIL_BLU);
                        } else if (cServer.equalsIgnoreCase("iol.it")) {
                            hp = new pluginlibero(pluginlibero.MAIL_IOL);
                        } else if (cServer.equalsIgnoreCase("giallo.it")) {
                            hp = new pluginlibero(pluginlibero.MAIL_GIALLO);
                        } else if (cServer.equalsIgnoreCase("infinito.it")) {
                            hp = new plugininfinito();
                        } else if (cServer.equalsIgnoreCase("tiscali.it")) {
                            hp = new plugintiscali();
                        } else if (cServer.equalsIgnoreCase("fastwebnet.it")) {
                            hp = new pluginfastwebnet();
                            //} else if( cServer.equalsIgnoreCase("email.it") ){
                            //hp = new pluginemail();
                        } else if (cServer.equalsIgnoreCase("tin.it")) {
                            hp = new PluginTin();
                        } else if (cServer.equalsIgnoreCase("virgilio.it")) {
                            hp = new pluginvirgilio();
                        } else if (cServer.equalsIgnoreCase("tim.it")) {
                            hp = new plugintim();
                            //} else if( cServer.equalsIgnoreCase("aliceposta.it") ){
                            //hp = new pluginaliceposta();
                        } else if (cServer.equalsIgnoreCase("hotmail.com")) {
                            //if( parent.getIsWin32() ) {
                            //   log.error( "Hotmail non supportato nella versione Win32" );
                            //}

                            getHotmail();

                        } else if (cServer.equalsIgnoreCase("supereva.it")) {
                            hp = new pluginsupereva();
                        } else if (cServer.equalsIgnoreCase("tele2.it")) {
                            //if( parent.getIsWin32() ) {
                            //   log.error( "Tele2 non supportato nella versione Win32" );
                            //}

                            getTele2();

                        } else if (cServer.equalsIgnoreCase("gmail.com")) {
                            //if( parent.getIsWin32() ) {
                            //   log.error( "Gmail non supportato nella versione Win32" );
                            //}

                            getGmail();

                        } else if (cServer.equalsIgnoreCase("linux.it")) {
                            getLinuxIt();
                        } else if (cServer.equalsIgnoreCase("pop3")) {
                            hp = new pluginpop3();
                        } else if (cServer.equalsIgnoreCase("rss")) {
                            hp = new pluginrss();
                        }

                        if (hp != null) {
                            // Now I can login on server, depending on requested server
                            hp.setMaxMessageNum(parent.getMaxEmail());
                            // Set Debug
                            hp.setDebug(parent.getDebug());

                            boolean bLogin = hp.login(cUser, cPassword);

                            if (bLogin) {
                                if (hp.list()) {
                                    //if( parent.getMaxEmail()!=-1 ) hp.setMessageNum(parent.getMaxEmail());
                                    html.putData(SO, "+OK " + hp.getMessageNum() + " messages\r\n");
                                    if (!parent.getLifo()) {
                                        hp.invertSort();
                                    }
                                    log.info(cServer + ": Trovati " + hp.getMessageNum() + " messaggi");
                                } else {
                                    html.putData(SO, "-ERR errore durante la creazione della lista email\r\n");
                                }
                            } else {
                                if (hp.getLastErr().length() > 0) {
                                    html.putData(SO, "-ERR " + hp.getLastErr() + "\r\n");
                                } else {
                                    html.putData(SO, "-ERR invalid password\r\n");
                                }
                                // Esce 1.10, prima faceva dei loop strani
                                bExit = true;
                            }
                        } else {
                            log.error("POP3 server: errore, server (" + cServer + ") non implementato al momento.");
                            log.error("Controllare che la sintassi sia server;username. EX libero.it;pippo@libero.it");
                            log.error("Eventualmente controllare su http://www.baccan.it che non sia stato rilasciato un aggiornamento che implementa tale server");
                            html.putData(SO, "-ERR invalid configuration. Read manual for correct user setting\r\n");
                            bExit = true;
                        }
                    }

                } else if (cLineUpper.startsWith("TOP")) {
                    int nPos = cLine.indexOf(" ", 4);
                    if (nPos == -1) {
                        html.putData(SO, "-ERR Protocol error\r\n");
                    } else {
                        if (hp == null) {
                            html.putData(SO, "-ERR Command is not valid in this state\r\n");
                        } else {
                            int nMsg = Double.valueOf(cLine.substring(4, nPos).trim()).intValue();
                            int nLine = Double.valueOf(cLine.substring(nPos).trim()).intValue();
                            if (!hp.streamMessageTop(SO, nMsg, nLine)) {
                                String cMessage = hp.getMessageTop(nMsg, nLine);
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
                    if (hp == null) {
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
                    if (parent.getOutlook2002Timeout()) {
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

                    if (hp == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        // Non complinant al 100%
                        // Note that messages marked as deleted are not counted in either total.
                        // Minimal implementations should just end that line of the response with a CRLF pair
                        String cMessage = "+OK " + hp.getMessageNum() + " " + hp.getMessageSize();
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
                    if (hp == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        if (cLine.length() > 5) {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            int nNum = hp.getMessageNum();
                            if (nMsg > nNum) {
                                html.putData(SO, "-ERR no such message, only " + nNum + " messages in maildrop\r\n");
                            } else {
                                html.putData(SO, "+OK " + nMsg + " " + hp.getMessageSize(nMsg) + "\r\n");
                            }

                        } else {
                            html.putData(SO, "+OK\r\n");
                            int nNum = hp.getMessageNum();
                            for (int nPos = 1; nPos <= nNum; nPos++) {
                                html.putData(SO, "" + nPos + " " + hp.getMessageSize(nPos) + "\r\n");
                            }
                            html.putData(SO, ".\r\n");
                        }
                    }

                } else if (cLineUpper.startsWith("UIDL")) {
                    if (hp == null) {
                        html.putData(SO, "-ERR Command is not valid in this state\r\n");
                    } else {
                        if (cLine.length() > 5) {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            int nNum = hp.getMessageNum();
                            if (nMsg > nNum) {
                                html.putData(SO, "-ERR no such message, only " + nNum + " messages in maildrop\r\n");
                            } else {
                                html.putData(SO, "+OK " + nMsg + " " + hp.getMessageID(nMsg) + "\r\n");
                            }

                        } else {
                            html.putData(SO, "+OK\r\n");
                            int nNum = hp.getMessageNum();
                            for (int nPos = 1; nPos <= nNum; nPos++) {
                                String cMessage = "" + nPos + " " + hp.getMessageID(nPos);
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
                        if (hp == null) {
                            html.putData(SO, "-ERR Command is not valid in this state\r\n");
                        } else {
                            int nMsg = Double.valueOf(cLine.substring(4).trim()).intValue();
                            if (!hp.streamMessage(SO, nMsg)) {
                                String cMessage = hp.getMessage(nMsg);
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
                    } else {
                        if (parent.getDelete()) {
                            Double nMsg = Double.valueOf(cLine.substring(4).trim()); //.intValue();
                            boolean bDel = true;
                            for (int nCur = 0; nCur < aDel.size(); nCur++) {
                                Double n = ((Double) aDel.get(nCur));
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
                    if (parent.getDeleteOptimized()) {
                        html.putData(SO, "+OK POP3 server closing connection\r\n");
                        try {
                            socket.close();
                        } catch (Throwable e) {
                        }
                    }

                    if (hp != null) {
                        // Cancello se devo cancellare
                        if (parent.getDelete()) {
                            if (aDel.size() > 0) {
                                log.info("Start removing deleted message from queue ...");
                            }
                            hp.delMessageStart();
                            for (int nCur = 0; nCur < aDel.size(); nCur++) {
                                int nMsg = ((Double) aDel.get(nCur)).intValue();
                                if (hp.delMessage(nMsg)) {
                                    log.info("POP3 server: deleted " + nMsg);
                                } else {
                                    log.error("POP3 server: error deleting " + nMsg);
                                }
                            }

                            // Pulisco il cestino, solo se ho cancellato qualcosa
                            if (aDel.size() > 0) {
                                try {
                                    hp.delMessagesFromTrash();
                                } catch (DeleteMessageException dme) {
                                    log.error("POP3 server: Error while emptying web trash. DeleteMessageException: " + dme.getMessage());
                                }
                            }

                            hp.delMessageEnd();
                            if (aDel.size() > 0) {
                                log.info("End removing deleted message from queue ...");
                            }
                        } else {
                            if (aDel.size() > 0) {
                                log.info("POP3 server: delete disabled");
                            }
                        }
                    }

                    // NON ottimizzata .. messaggio dopo e chiusura dopo
                    if (!parent.getDeleteOptimized()) {
                        html.putData(SO, "+OK POP3 server closing connection\r\n");
                    }

                    //} else if( cLineUpper.length()==0 ){
                } else {
                    html.putData(SO, "-ERR Syntax Error or Unknown Command\r\n");
                }
            }
        }

    }
}
