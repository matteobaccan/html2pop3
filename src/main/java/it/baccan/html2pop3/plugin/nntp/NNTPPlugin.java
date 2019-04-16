/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        nntpplugin interface
 * Description:  Interfaccia per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.nntp;

import java.io.OutputStream;
import java.util.ArrayList;

/**
 *
 * @author matteo
 */
public interface NNTPPlugin {

    /**
     *
     * @param outputStream
     * @return
     */
    boolean streamList(OutputStream outputStream);

    /**
     *
     * @param cGroup
     * @return
     */
    long[] group(String cGroup);

    /**
     *
     * @param nFrom
     * @param nTo
     * @return
     */
    ArrayList<String> xover(long nFrom, long nTo);

    /**
     *
     * @param nId
     * @return
     */
    String article(long nId);
}
