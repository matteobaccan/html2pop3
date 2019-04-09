/*
 * pop3plugin interface
 *
 * Copyright 2004 Matteo Baccan
 * www - http://www.baccan.it
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA (or visit
 * their web site at http://www.gnu.org/).
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

import java.io.*;
import java.util.*;

import it.baccan.html2pop3.exceptions.DeleteMessageException;

/**
 *
 * @author matteo
 */
public interface pop3plugin {

    // Accesso al portale e memorizzazione login e pwd
    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd);

    // List
    /**
     *
     * @return
     */
    boolean list();

    // Get message
    /**
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
     * @param SO
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     * @throws Exception
     */
    boolean streamMessage(OutputStream SO, int nPos, int nLine, boolean bAll) throws Exception;

    /**
     *
     * @param SO
     * @param nPos
     * @return
     * @throws Exception
     */
    boolean streamMessage(OutputStream SO, int nPos) throws Exception;

    /**
     *
     * @param SO
     * @param nPos
     * @param nLine
     * @return
     * @throws Exception
     */
    boolean streamMessageTop(OutputStream SO, int nPos, int nLine) throws Exception;

    /**
     *
     * @param nPos
     * @return
     */
    boolean delMessage(int nPos);

    /**
     *
     */
    void delMessageStart();

    /**
     *
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
     *
     */
    void invertSort();

    /**
     *
     * @throws DeleteMessageException
     */
    public void delMessagesFromTrash() throws DeleteMessageException;

    // Set and get Last Error
    /**
     *
     * @return
     */
    public String getLastErr();

    /**
     *
     * @param c
     */
    public void setLastErr(String c);

    // Contatti
    /**
     *
     * @return
     */
    public Vector getContact();

    /**
     *
     * @return
     */
    public String getContactXML();

    // Debug
    /**
     *
     * @param p
     */
    public void setDebug(boolean p);

    /**
     *
     * @return
     */
    public boolean getDebug();
}
