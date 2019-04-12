/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        SMTP Server
 * Description:  Server SMTP
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3;

import it.baccan.html2pop3.plugin.pop3.plugingmail;
import it.baccan.html2pop3.plugin.smtp.plugincgiemail;
import it.baccan.html2pop3.plugin.smtp.pluginsmtp;
import it.baccan.html2pop3.plugin.smtp.smtpplugin;
import it.baccan.html2pop3.utils.EchoClient;
import it.baccan.html2pop3.utils.MsgBox;
import it.baccan.html2pop3.utils.htmlTool;
import it.baccan.html2pop3.utils.version;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
import java.util.Vector;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class SMTPServer extends BaseServer {

    private final String cLoginString = "220 HTML2POP3 ESMTP Service (" + version.getVersion() + ") ready";


    /**
     *
     * @param p
     */
    public SMTPServer(html2pop3 p) {
        super(p);
    }

    public void run() {
        smtpThread thread;
        try {
            if (getParent().getPortSMTP() > 0) {
                setServerSocket( new ServerSocket(getParent().getPortSMTP(), getParent().getClient(), InetAddress.getByName(getParent().getHost())));
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

                    thread = new smtpThread(socket);
                    thread.start();
                }
            }
        } catch (BindException be) {
            String cLoginStringFound = EchoClient.getLine(getParent().getHost(), getParent().getPortSMTP());
            String cError = "Errore! Porta " + getParent().getPortSMTP() + " in uso,\nValore corrente (" + cLoginStringFound + ")\nCambiare porta nel config.cfg e fare un restart del server SMTP";

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            log.info(cError.replace('\n', ' '));

            if (cLoginString.equals(cLoginStringFound)) {
                log.info("Exit for double run");
                getParent().exitFromProgram();
            } else {
                if (getParent().getGuiError()) {
                    //MsgBox message =
                    new MsgBox("HTML2POP3 server SMTP", cError, false);
                }
            }

        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

    class smtpThread extends Thread {

        private Socket socket;

        public smtpThread(Socket socket) {
            this.socket = socket;
        }

        // Inizio thread di gestione del socket
        @Override
        public void run() {
            try {

                manage(socket);

            } catch (SocketException se) {

                log.error("SMTP server: chiusura connessione: " + se.getMessage());

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
            if (!getParent().getSMTPIpFilter().isAllow(new String[]{cIP})) {
                log.error("500 IP (" + cIP + ") deny");
                html.putData(SO, "500 IP (" + cIP + ") deny\r\n");
                return;
            }

            // initial banner
            html.putData(SO, cLoginString + "\r\n");

            boolean bExit = false;

            String cUser = "";
            String cPwd = "";
            String cFrom = "";
            Vector aTo = new Vector();
            // main loop
            while (!bExit) {
                String cLine = html.getLineNOCRLF(SI);
                String cLineUpper = cLine.toUpperCase();

                log.error("SMTP server: " + cLine);

                if (cLineUpper.startsWith("EHLO") || cLineUpper.startsWith("HELO")) {
                    html.putData(SO, "250-HTML2POP3\r\n");
                    html.putData(SO, "250-AUTH=LOGIN\r\n");
                    html.putData(SO, "250 AUTH LOGIN\r\n");
                    //html.putData( SO, "250 AUTH LOGIN PLAIN\r\n" );

                } else if (cLineUpper.startsWith("AUTH CRAM-MD5")) {
                    html.putData(SO, "504 unsupported AUTH security mechanism\r\n");

                } else if (cLineUpper.startsWith("AUTH PLAIN")) {
                    html.putData(SO, "504 unsupported AUTH security mechanism\r\n");

                    /* ##REL 2.01
                                                //1234567890
                } else if( cLineUpper.startsWith("AUTH PLAIN") ){
                    // user\0pwd\0 in base64
                    if( cLine.length()<=10 ) {
                        html.putData( SO, "334 ?\r\n" );
                        // ##aspetta login e pwd
                    } else {
                        cFrom = cLine.substring(10).trim();
                        html.putData( SO, "235 login authentication successful\r\n" );
                    }
                     */
                } else if (cLineUpper.startsWith("AUTH LOGIN")) {
                    html.putData(SO, "334 VXNlcm5hbWU6\r\n"); // Username: in base64
                    cUser = new String(Base64.getDecoder().decode(html.getLineNOCRLF(SI)));
                    log.error("SMTP server: " + cUser);

                    html.putData(SO, "334 UGFzc3dvcmQ6\r\n"); // Password: in base64
                    cPwd = new String(Base64.getDecoder().decode(html.getLineNOCRLF(SI)));
                    String c = "";
                    while (c.length() < cPwd.length()) {
                        c += "*";
                    }
                    log.error("SMTP server: " + c);

                    html.putData(SO, "235 login authentication successful\r\n");

                    //1234567890
                } else if (cLineUpper.startsWith("MAIL FROM:")) {
                    if (cLine.length() <= 10) {
                        html.putData(SO, "501 Syntax error in parameters or arguments to MAIL command\r\n");
                    } else {
                        cFrom = cLine.substring(10).trim();
                        if (cFrom.startsWith("<") && cFrom.length() > 1) {
                            cFrom = cFrom.substring(1);
                        }
                        if (cFrom.endsWith(">")) {
                            cFrom = cFrom.substring(0, cFrom.length() - 1);
                        }
                        html.putData(SO, "250 MAIL FROM:" + cFrom + " OK\r\n");
                    }

                    //12345678
                } else if (cLineUpper.startsWith("RCPT TO:")) {
                    if (cLine.length() <= 8) {
                        html.putData(SO, "501 Syntax error in parameters or arguments to RCPT command\r\n");
                    } else {
                        String cTo = cLine.substring(8).trim();
                        if (cTo.startsWith("<") && cTo.length() > 1) {
                            cTo = cTo.substring(1);
                        }
                        if (cTo.endsWith(">")) {
                            cTo = cTo.substring(0, cTo.length() - 1);
                        }
                        aTo.addElement(cTo);
                        html.putData(SO, "250 RCPT TO:" + cTo + " OK\r\n");
                    }

                } else if (cLineUpper.startsWith("NOOP")) {
                    html.putData(SO, "250 NOOP\r\n");

                } else if (cLineUpper.startsWith("RSET")) {
                    cUser = "";
                    cPwd = "";
                    cFrom = "";
                    aTo = new Vector();
                    html.putData(SO, "250 RSET\r\n");

                } else if (cLineUpper.startsWith("HELP")) {
                    //html.putData( SO, "214-  STARTTLS\r\n" );
                    //html.putData( SO, "214-  VRFY, EXPN, ETRN\r\n" );
                    //html.putData( SO, "214-For more info, use HELP <valid SMTP command>\r\n" );
                    html.putData(SO, "214-Valid SMTP commands:\r\n");
                    html.putData(SO, "214-  HELO, EHLO, NOOP, RSET, QUIT\r\n");
                    html.putData(SO, "214-  MAIL, RCPT, DATA, HELP\r\n");
                    html.putData(SO, "214 end of help\r\n");

                } else if (cLineUpper.startsWith("DATA")) {

                    int nServer = cUser.indexOf(";");
                    String cServer = "smtp";
                    if (nServer != -1) {
                        cServer = cUser.substring(0, nServer);
                        cUser = cUser.substring(nServer + 1).trim();
                    } else {
                        if (cUser.toUpperCase().indexOf("@GMAIL.COM") != -1) {
                            cServer = "gmail.com";
                        }
                    }

                    log.error("SMTP server: usato " + cServer);

                    // Plugin Filter
                    if (!getParent().getSMTPPluginFilter().isAllow(new String[]{cServer})) {
                        log.error("500 plugin (" + cServer + ") deny");
                        html.putData(SO, "500 plugin (" + cServer + ") deny\r\n");
                        return;
                    }

                    // User Filter
                    if (!getParent().getSMTPUserFilter().isAllow(new String[]{cFrom})) {
                        log.error("500 user (" + cFrom + ") deny");
                        html.putData(SO, "500 user (" + cFrom + ") deny\r\n");
                        return;
                    }

                    // Global Filter
                    if (!getParent().getSMTPGlobalFilter().isAllow(new String[]{cIP, cServer, cFrom})) {
                        log.error("500 global (" + cIP + ")(" + cServer + ")(" + cFrom + ") deny");
                        html.putData(SO, "500 global (" + cIP + ")(" + cServer + ")(" + cFrom + ") deny\r\n");
                        return;
                    }

                    //StringBuffer aHeader = new StringBuffer();
                    StringBuffer aMail = new StringBuffer();
                    //String cSubject = "";

                    html.putData(SO, "354 Start mail input; end with <CRLF>.<CRLF>\r\n");

                    String cSubLine = html.getLine(SI);
                    while (!cSubLine.equalsIgnoreCase("." + (char) 13 + (char) 10)) {
                        aMail.append(cSubLine);
                        cSubLine = html.getLine(SI);
                    }

                    smtpplugin sp = null;
                    if (cServer.equalsIgnoreCase("smtp")) {
                        sp = new pluginsmtp();
                    } else if (cServer.equalsIgnoreCase("gmail.com")) {
                        sp = new plugingmail();
                    } else if (cServer.equalsIgnoreCase("cgiemail")) {
                        sp = new plugincgiemail();
                    } else {
                        log.error("SMTP server: errore, server mancante. Sintassi server;[parameter]. EX smtp;");
                        sp = new pluginsmtp();
                    }

                    boolean bRet = false;
                    if (sp.login(cUser, cPwd)) {
                        bRet = sp.sendMessage(cFrom, aTo, aMail.toString());
                    }

                    log.error("SMTP server: " + bRet + (bRet ? "" : " : ") + sp.getLastErr());

                    if (bRet) {
                        html.putData(SO, "250 <" + System.currentTimeMillis() + "> Mail accepted\r\n");
                    } else {
                        html.putData(SO, "500 " + sp.getLastErr() + "\r\n");
                    }

                    // Reset old value
                    aTo = new Vector();
                    cFrom = "";

                } else if (cLineUpper.startsWith("QUIT")) {
                    html.putData(SO, "221 HTML2POP3 QUIT\r\n");
                    bExit = true;

                } else {
                    html.putData(SO, "500 Syntax Error or Unknown Command\r\n");
                }
            }
        }

    }
}
