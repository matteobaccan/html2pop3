/*
 * smtpplugin interface
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
 * Title:        smtpplugin interface
 * Description:  Interfaccia per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.smtp;

import java.util.Vector;

/**
 *
 * @author matteo
 */
public interface SMTPPlugin {
    // Login to the SMTP server

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd);

    // Send message
    /**
     *
     * @param cFrom
     * @param aTo
     * @param cMsg
     * @return
     */
    public boolean sendMessage(String cFrom, Vector aTo, String cMsg);

    // Get last error
    /**
     *
     * @return
     */
    public String getLastErr();
}
