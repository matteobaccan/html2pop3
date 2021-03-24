/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        nntpbase
 * Description:  Classe base per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.nntp;

import it.baccan.html2pop3.plugin.PluginBase;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 *
 * @author matteo
 */
public abstract class NNTPBase extends PluginBase {

    /**
     *
     * @param outputStream
     * @return
     */
    public abstract boolean streamList(OutputStream outputStream);

    /**
     *
     * @param group
     * @return
     */
    public abstract long[] group(String group);

    /**
     *
     * @param from
     * @param to
     * @return
     */
    public abstract ArrayList<String> xover(long from, long to);

    /**
     *
     * @param nId
     * @return
     */
    public abstract String article(long nId);

}
