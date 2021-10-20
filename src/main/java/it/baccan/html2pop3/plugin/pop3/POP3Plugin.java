/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        pop3plugin interface
 * Description:  Interfaccia per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import it.baccan.html2pop3.exceptions.DeleteMessageException;
import java.io.OutputStream;
import java.util.List;

/**
 *
 * @author matteo
 */
public interface POP3Plugin {

    /**
     * Accesso al portale e memorizzazione login e pwd.
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    boolean login(String cUser, String cPwd);

    /**
     * List email.
     *
     * @return
     */
    boolean list();

    /**
     * Get del messaggio.
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    String getMessage(int nPos, int nLine, boolean bAll);

    /**
     *
     * @param nPos
     * @return
     */
    String getMessage(int nPos);

    /**
     *
     * @param nPos
     * @param nLine
     * @return
     */
    String getMessageTop(int nPos, int nLine);

    /**
     *
     * @param outputStream
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     * @throws Exception
     */
    boolean streamMessage(OutputStream outputStream, int nPos, int nLine, boolean bAll) throws Exception;

    /**
     *
     * @param outputStream
     * @param nPos
     * @return
     * @throws Exception
     */
    boolean streamMessage(OutputStream outputStream, int nPos) throws Exception;

    /**
     *
     * @param outputStream
     * @param nPos
     * @param nLine
     * @return
     * @throws Exception
     */
    boolean streamMessageTop(OutputStream outputStream, int nPos, int nLine) throws Exception;

    /**
     *
     * @param nPos
     * @return
     */
    boolean delMessage(int nPos);

    /**
     * Inizio rimozione messaggi.
     */
    void delMessageStart();

    /**
     * Termine rimozione messaggi.
     */
    void delMessageEnd();

    // Implementate nella classe base
    /**
     *
     * @return
     */
    int getMessageNum();

    /**
     *
     * @param nNum
     */
    void setMaxMessageNum(int nNum);

    /**
     *
     * @return
     */
    int getMessageSize();

    /**
     *
     * @param nPos
     * @return
     */
    int getMessageSize(int nPos);

    /**
     *
     * @param nPos
     * @return
     */
    String getMessageID(int nPos);

    /**
     * Sort invertito.
     */
    void invertSort();

    /**
     *
     * @throws DeleteMessageException
     */
    void delMessagesFromTrash() throws DeleteMessageException;

    /**
     * Set and get Last Error.
     *
     * @return
     */
    String getLastErr();

    /**
     *
     * @param c
     */
    void setLastErr(String c);

    /**
     * Contatti.
     *
     * @return
     */
    List<String[]> getContact();

    /**
     *
     * @return
     */
    String getContactXML();

    /**
     * Abilita il debug.
     *
     * @param p
     */
    void setDebug(boolean p);

    /**
     *
     * @return
     */
    boolean isDebug();
}
