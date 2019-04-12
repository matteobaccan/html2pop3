/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        BASE Server
 * Description:  Server BASE
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3;

import java.net.ServerSocket;
import java.net.Socket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class BaseServer extends Thread {

    /**
     *
     */
    @Getter private HTML2POP3 parent;

    /**
     *
     */
    @Getter @Setter private ServerSocket serverSocket = null;

    /**
     *
     */
    @Getter private boolean isFinish = false;

    /**
     *
     * @param p
     */
    public BaseServer(final HTML2POP3 p) {
        parent = p;
    }

    /**
     * @param socket
     * @throws java.lang.Throwable * @conditional (JVM14)
     */
    protected final void setKeepAlive(final Socket socket) throws Throwable {
        socket.setKeepAlive(true);
    }

    /**
     *
     */
    public final void finish() {
        try {
            isFinish = true;
            serverSocket.close();
        } catch (Throwable e) {
            log.error("Error closing server socket [{}]", e.getMessage());
        }
    }
}
