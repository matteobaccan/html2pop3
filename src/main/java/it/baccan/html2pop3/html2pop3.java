/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/**
 * Title:        html2pop3 Server
 * Description:  Server POP3
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3;

import java.io.*;
import java.util.*;
import java.text.*;

import it.baccan.html2pop3.utils.*;
import it.baccan.html2pop3.utils.message.*;
import it.baccan.html2pop3.plugin.*;
import it.baccan.html2pop3.plugin.pop3.*;
import it.baccan.html2pop3.plugin.nntp.*;
import it.baccan.html2pop3.plugin.smtp.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class html2pop3 extends Thread {

    private static String cConfig = "/config.cfg";
    private filter pop3IpFilter = new filter();
    private filter pop3PluginFilter = new filter();
    private filter pop3UserFilter = new filter();
    private filter pop3GlobalFilter = new filter();
    private filter smtpIpFilter = new filter();
    private filter smtpPluginFilter = new filter();
    private filter smtpUserFilter = new filter();
    private filter smtpGlobalFilter = new filter();
    private filter nntpIpFilter = new filter();

    /**
     *
     * @param args
     */
    static public void main(String args[]) {
        // Imposto l'eventuale config
        html2pop3.parseCommandLine(args);

        // Partenza
        html2pop3 html2pop3 = new html2pop3();
        html2pop3.start();
    }

    /**
     *
     * @return
     */
    static public String getConfig() {
        return cConfig;
    }

    /**
     *
     * @param c
     */
    static public void setConfig(String c) {
        cConfig = c;
    }

    /**
     *
     * @param args
     */
    static public void parseCommandLine(String args[]) {
        for (int nPar = 0; nPar < args.length; nPar++) {
            if (args[nPar].equalsIgnoreCase("-config") && nPar + 1 < args.length) {
                html2pop3.setConfig(args[nPar + 1]);
            }
        }
    }

    /**
     * @conditional (JVM14)
     */
    public void trustAll() {
        try {
            // Creo un TrustManager
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{new fakeX509TrustManager()};

            // Prendo un'istanza di SSLContext
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");

            // Inizializzo con il "trusta tutto"
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            // Lo metto come default delle connessioni HTTPS
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Throwable ex) {
            log.error("Error", ex);
        }
    }

    class SortedProperties extends Properties {

        public synchronized Enumeration keys() {

            Enumeration keysEnum = super.keys();
            Vector keyList = new Vector();
            while (keysEnum.hasMoreElements()) {
                keyList.addElement(keysEnum.nextElement());
            }

            boolean bSort;
            do {
                bSort = true;
                for (int nPos = 0; nPos < keyList.size() - 1; nPos++) {
                    Object a = keyList.elementAt(nPos);
                    Object b = keyList.elementAt(nPos + 1);

                    Collator c = Collator.getInstance();

                    if (c.compare((String) a, (String) b) > 0) {
                        keyList.setElementAt(a, nPos + 1);
                        keyList.setElementAt(b, nPos);
                        bSort = false;
                    }
                }
            } while (!bSort);

            // Not 1.1.4 compatibile
            //Collections.sort(keyList);
            return keyList.elements();
        }

    }

    private SortedProperties p = new SortedProperties();
    private String cLocalHost = "127.0.0.1";
    private int cLocalPort = 110;
    private int cLocalPortSMTP = 25;
    private int cLocalPortNNTP = 119;
    private int nClient = 10;
    private boolean bDelete = true;
    private boolean bDeleteOptimized = true;
    private boolean bGuiError = true;
    private boolean bLifo = true;
    private boolean bOutlook2002Timeout = true;
    private int nMaxEmail = -1;
    private boolean bDebug = false;

    private html2pop3ExitHook parent;

    /**
     *
     */
    public html2pop3() {
        // Imposta il flag win32, con JDK MS non viene chiamato il metodo
        //setNonWin32();
        // Trust di qualsiasi sito, non controlla la fonte SSL
        trustAll();

        // Imposta il URLStreamHandlerFactory
        //setURLStreamHandlerFactory();
        // Load configuration file
        load();
    }

    //private boolean bIsWin32  = true;
    /**
     * @conditional (JVM14)
     */
    //public void setNonWin32() {
    //   bIsWin32 = false;
    //}
    //public boolean getIsWin32(){
    //    return bIsWin32;
    //}
    /**
     * @conditional (JVM14)
     */
    //public void closeWin32GUI() {
    //   bIsWin32 = false;
    //}
    public void exitFromProgram() {
        if (parent != null) {
            parent.html2pop3Exit();
        }
        System.exit(0);
    }

    /**
     *
     * @return
     */
    public boolean getOutlook2002Timeout() {
        return bOutlook2002Timeout;
    }

    /**
     *
     * @return
     */
    public String getHost() {
        return cLocalHost;
    }

    /**
     *
     * @param c
     */
    public void setHost(String c) {
        cLocalHost = c;
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return cLocalPort;
    }

    /**
     *
     * @param p
     */
    public void setPort(int p) {
        cLocalPort = p;
    }

    /**
     *
     * @return
     */
    public int getPortSMTP() {
        return cLocalPortSMTP;
    }

    /**
     *
     * @param p
     */
    public void setPortSMTP(int p) {
        cLocalPortSMTP = p;
    }

    /**
     *
     * @return
     */
    public int getPortNNTP() {
        return cLocalPortNNTP;
    }

    /**
     *
     * @param p
     */
    public void setPortNNTP(int p) {
        cLocalPortNNTP = p;
    }

    /**
     *
     * @return
     */
    public int getMaxEmail() {
        return nMaxEmail;
    }

    /**
     *
     * @param p
     */
    public void setMaxEmail(int p) {
        nMaxEmail = p;
    }

    /**
     *
     * @return
     */
    public int getClient() {
        return nClient;
    }

    /**
     *
     * @param p
     */
    public void setClient(int p) {
        nClient = p;
    }

    /**
     *
     * @param b
     */
    public void setDelete(boolean b) {
        bDelete = b;
    }

    /**
     *
     * @return
     */
    public boolean getDelete() {
        return bDelete;
    }

    /**
     *
     * @param b
     */
    public void setGuiError(boolean b) {
        bGuiError = b;
    }

    /**
     *
     * @return
     */
    public boolean getGuiError() {
        return bGuiError;
    }

    /**
     *
     * @param b
     */
    public void setDeleteOptimized(boolean b) {
        bDeleteOptimized = b;
    }

    /**
     *
     * @return
     */
    public boolean getDeleteOptimized() {
        return bDeleteOptimized;
    }

    /**
     *
     * @return
     */
    public boolean getLifo() {
        return bLifo;
    }

    /**
     *
     * @param b
     */
    public void setLifo(boolean b) {
        bLifo = b;
    }

    /**
     *
     * @param b
     */
    public void setDebug(boolean b) {
        bDebug = b;
    }

    /**
     *
     * @return
     */
    public boolean getDebug() {
        return bDebug;
    }

    /**
     *
     * @return
     */
    public boolean load() {
        boolean bRet = false;
        try {
            log.info("Load Config [{}]", cConfig);
            try (InputStream fis = this.getClass().getResourceAsStream(cConfig)) {
                p.clear();
                p.load(fis);
            }

            // Host + port
            this.cLocalHost = p.getProperty("host", "127.0.0.1");
            this.cLocalPort = Double.valueOf(p.getProperty("port", "110")).intValue();
            this.cLocalPortSMTP = Double.valueOf(p.getProperty("portsmtp", "25")).intValue();
            this.cLocalPortNNTP = Double.valueOf(p.getProperty("portnntp", "119")).intValue();
            this.nClient = Double.valueOf(p.getProperty("concurrentclient", "10")).intValue();
            this.bDelete = p.getProperty("delete", "true").equalsIgnoreCase("true");
            this.bDeleteOptimized = p.getProperty("deleteoptimized", "true").equalsIgnoreCase("true");
            this.bGuiError = p.getProperty("guierror", "true").equalsIgnoreCase("true");
            this.bLifo = p.getProperty("coda", "lifo").equalsIgnoreCase("lifo");
            this.bOutlook2002Timeout = p.getProperty("outlook2002.timeout", "true").equalsIgnoreCase("true");
            this.bDebug = p.getProperty("debug", "false").equalsIgnoreCase("true");

            // Email per session
            this.nMaxEmail = Double.valueOf(p.getProperty("maxdownloadpersession", "-1")).intValue();

            // POP3 default
            POP3Message.setAddHTML(p.getProperty("htmlattach", "true").equalsIgnoreCase("true"));

            // RSS
            pluginrss.setConfig(getConfigPath(), "rss.cfg");

            // NNTP
            pluginnntp.setConfig(getConfigPath(), "nntp.cfg");

            // Size del log
            //logStreamMemo.setLogSize(Double.valueOf(p.getProperty("logsize", "2000000")).intValue());
            // Plugin specific
            plugintiscali.setDelete(p.getProperty("tiscali.delete", "true").equalsIgnoreCase("true"));
            plugintin.setDelete(p.getProperty("tin.delete", "true").equalsIgnoreCase("true"));
            pluginlibero.setRead(p.getProperty("libero.read", "false").equalsIgnoreCase("true"));

            // RFC2047
            POP3Message.setrfc2047(p.getProperty("rfc2047", "true").equalsIgnoreCase("true"));

            // Tunneling server
            pluginpop3.setDefaultServer(p.getProperty("tunnelingserver", "http://www.baccan.it/pop3/"));
            pluginsmtp.setDefaultServer(p.getProperty("tunnelingserver", "http://www.baccan.it/pop3/"));

            // I vecchi JDK non hanno il metodo di modifica delle proprieta' direttamente
            // sul system
            Properties sp = System.getProperties();

            // Rimuovo le cache sui DNS, in modo da eviare in restart in caso di cambio IP o connessione
            java.security.Security.setProperty("networkaddress.cache.ttl", "0");
            java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");

            sp.put("proxyUser", p.getProperty("proxyuser", ""));
            sp.put("proxyPassword", p.getProperty("proxypassword", ""));

            // proxy + port
            sp.put("http.proxyHost", p.getProperty("proxyhost", ""));
            sp.put("http.proxyPort", p.getProperty("proxyport", ""));

            //qui imposto il proxy di uscita per https e user/pass tramite authenticator
            //per il momento imposto gli stessi valori del proxy http.
            //Prevedere la possibilita' di impostare un proxy diverso in fase di conf.
            System.getProperties().put("https.proxyHost", System.getProperty("http.proxyHost"));
            System.getProperties().put("https.proxyPort", System.getProperty("http.proxyPort"));

            if (p.getProperty("proxyhost", "").length() > 0) {
                sp.put("http.proxySet", "true");
            } else {
                sp.put("http.proxySet", "false");
            }

            System.setProperties(sp);

            // Filter
            pop3IpFilter = getFilter("pop3.ipfilter");
            pop3PluginFilter = getFilter("pop3.pluginfilter");
            pop3UserFilter = getFilter("pop3.userfilter");
            pop3GlobalFilter = getFilter("pop3.globalfilter");
            smtpIpFilter = getFilter("smtp.ipfilter");
            smtpPluginFilter = getFilter("smtp.pluginfilter");
            smtpUserFilter = getFilter("smtp.userfilter");
            smtpGlobalFilter = getFilter("smtp.globalfilter");
            nntpIpFilter = getFilter("nntp.ipfilter");

            bRet = true;
        } catch (java.io.FileNotFoundException fnf) {

            log.info("Non riesco a leggere il file " + getConfigFullPath() + ", lanciare html2pop3 dalla directory di " + cConfig);

        } catch (IOException e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
        return bRet;
    }

    private filter getFilter(String cRoot) {
        filter ret = new filter();

        int nFilter = 1;
        while (true) {
            String cFilter = p.getProperty(cRoot + nFilter, "");
            int nSep = cFilter.indexOf(";");
            if (nSep <= 0) {
                break;
            }

            //log.info("Add: " +cFilter);
            String cRule = cFilter.substring(0, nSep);
            cFilter = cFilter.substring(nSep + 1);

            Vector v = new Vector();
            StringTokenizer st = new StringTokenizer(cFilter, ";");
            while (st.hasMoreTokens()) {
                v.addElement(st.nextToken());
            }

            String[] o = new String[v.size()];
            for (int nPos = 0; nPos < o.length; nPos++) {
                o[nPos] = (String) v.elementAt(nPos);
            }

            ret.add(cRule, o);
            nFilter++;
        }

        return ret;
    }

    /**
     *
     * @return
     */
    public filter getPOP3IpFilter() {
        return pop3IpFilter;
    }

    /**
     *
     * @return
     */
    public filter getPOP3PluginFilter() {
        return pop3PluginFilter;
    }

    /**
     *
     * @return
     */
    public filter getPOP3UserFilter() {
        return pop3UserFilter;
    }

    /**
     *
     * @return
     */
    public filter getPOP3GlobalFilter() {
        return pop3GlobalFilter;
    }

    /**
     *
     * @return
     */
    public filter getSMTPIpFilter() {
        return smtpIpFilter;
    }

    /**
     *
     * @return
     */
    public filter getSMTPPluginFilter() {
        return smtpPluginFilter;
    }

    /**
     *
     * @return
     */
    public filter getSMTPUserFilter() {
        return smtpUserFilter;
    }

    /**
     *
     * @return
     */
    public filter getSMTPGlobalFilter() {
        return smtpGlobalFilter;
    }

    /**
     *
     * @return
     */
    public filter getNNTPIpFilter() {
        return nntpIpFilter;
    }

    private String getConfigFullPath() {
        String cRet = "";
        try {
            File file = new File(cConfig);
            cRet = file.getAbsolutePath();
        } catch (Throwable e) {
        }
        return cRet;
    }

    /**
     *
     * @return
     */
    public String getConfigPath() {
        String cRet = File.separator;
        try {
            int nPos = getConfigFullPath().lastIndexOf(File.separator);
            if (nPos != -1) {
                cRet = getConfigFullPath().substring(0, nPos + 1);
            }
        } catch (Throwable e) {
        }
        return cRet;
    }

    /**
     *
     */
    public void save() {
        try {
            // Host + port
            p.put("host", cLocalHost);
            p.put("port", "" + cLocalPort);
            p.put("portsmtp", "" + cLocalPortSMTP);
            p.put("portnntp", "" + cLocalPortNNTP);
            p.put("concurrentclient", "" + nClient);
            p.put("delete", "" + bDelete);
            p.put("deleteoptimized", "" + bDeleteOptimized);
            p.put("guierror", "" + bGuiError);
            p.put("debug", "" + bDebug);

            if (bLifo) {
                p.put("coda", "lifo");
            } else {
                p.put("coda", "fifo");
            }

            // POP3 default
            p.put("htmlattach", "" + POP3Message.getAddHTML());

            // Email per sessione
            p.put("maxdownloadpersession", "" + nMaxEmail);

            // Proxy
            p.put("proxyuser", System.getProperty("proxyUser", ""));
            p.put("proxypassword", System.getProperty("proxyPassword", ""));

            // proxy + port
            p.put("proxyhost", System.getProperty("http.proxyHost", ""));
            p.put("proxyport", System.getProperty("http.proxyPort", ""));

            // DEPRECATA la uso per retrocompatiblita' con JDK MS e vecchi JDK
            p.save(new FileOutputStream(cConfig), null);
        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

    configChange cc;

    public void run() {
        POP3Server pop3;
        SMTPServer smtp;
        NNTPServer nntp;
        try {
            printInfo();

            pop3 = new POP3Server(this);
            pop3.start();
            smtp = new SMTPServer(this);
            smtp.start();
            nntp = new NNTPServer(this);
            nntp.start();
            cc = new configChange(this);
            cc.start();

            while (true) {
                // Faccio partire il Thread
                try {
                    Thread.sleep(1000);
                } catch (Throwable e) {
                }

                if (isRestart) {
                    log.info("Restarting ...");
                    pop3.finish();
                    while (pop3.isAlive()) {
                        Thread.sleep(100);
                    }

                    smtp.finish();
                    while (smtp.isAlive()) {
                        Thread.sleep(100);
                    }

                    nntp.finish();
                    while (nntp.isAlive()) {
                        Thread.sleep(100);
                    }

                    pop3 = new POP3Server(this);
                    pop3.start();
                    smtp = new SMTPServer(this);
                    smtp.start();
                    nntp = new NNTPServer(this);
                    nntp.start();
                    cc = new configChange(this);
                    cc.start();

                    printInfo();
                    isRestart = false;
                }
            }

        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

    /**
     *
     */
    public void printInfo() {
        String cVer = version.getVersion();
        while (cVer.length() < 6) {
            cVer = " " + cVer;
        }
        log.info("+---------------------------------------------------------------------------+");
        log.info("| POP3/SMTP/NNTP simulation server                           Version " + cVer + " |");
        log.info("| Matteo Baccan Opensource Software                    http://www.baccan.it |");
        log.info("+---------------------------------------------------------------------------+");
        //if( bIsWin32 ){
        //    log.info( "Java Version: " +System.getProperty("java.version") );
        //} else {
        log.info("Java Runtime: " + System.getProperty("java.runtime.name"));
        log.info("Java Version: " + System.getProperty("java.vm.version"));
        //}

        log.info("Config path: " + getConfigFullPath());

        if (cLocalPort > 0) {
            log.info("Server POP3 ready at " + cLocalHost + ":" + cLocalPort + " max clients: " + nClient);
        } else {
            log.info("Server POP3 disabled");
        }

        if (cLocalPortSMTP > 0) {
            log.info("Server SMTP ready at " + cLocalHost + ":" + cLocalPortSMTP + " max clients: " + nClient);
        } else {
            log.info("Server SMTP disabled");
        }

        if (cLocalPortNNTP > 0) {
            log.info("Server NNTP ready at " + cLocalHost + ":" + cLocalPortNNTP + " max clients: " + nClient);
        } else {
            log.info("Server NNTP disabled");
        }

        if (bLifo) {
            log.info("Message download queue: LIFO");
        } else {
            log.info("Message download queue: FIFO");
        }

        if (System.getProperty("http.proxyHost", "").length() > 0) {
            log.info("Proxy enabled " + System.getProperty("http.proxyHost", "") + ":" + System.getProperty("http.proxyPort"));
            if (System.getProperty("proxyUser", "").length() > 0) {
                log.info("Proxy-authorization Basic " + System.getProperty("proxyUser", ""));
            }
        }

        if (bDelete) {
            log.info("Le cancellazioni SONO abilitate, il client di posta potra' cancellare la posta");
            if (bDeleteOptimized) {
                log.info("Le cancellazioni sono fatte DOPO la sconnessione del client di posta");
            } else {
                log.info("Le cancellazioni sono fatte PRIMA della sconnessione del client di posta");
            }
        } else {
            log.info("Le cancellazioni NON sono abilitate, il client di posta non potra' cancellare la posta");
        }

        if (bGuiError) {
            log.info("Errori gravi visualizzati con messagebox GUI e file di log");
        } else {
            log.info("Errori gravi visualizzati solo nel file di log");
        }
        if (bDebug) {
            log.info("Modalita' di debug ATTIVA");
        } else {
            log.info("Modalita' di debug disattiva");
        }
        log.info("Attach email originale nella posta emulata: " + POP3Message.getAddHTML());
        log.info("Numero di download massimi per sessione: " + nMaxEmail);
        //log.info("Dimensione del file di log: " + logStreamMemo.getLogSize());
        log.info("Tunneling server " + p.getProperty("tunnelingserver", "http://www.baccan.it/pop3/"));
        log.info("-----------------------------------------------------------------------------");
        log.info("Plugin specific setting");
        log.info("-----------------------------------------------------------------------------");
        log.info("tiscali: modalita' di cancellazione " + (plugintiscali.getDelete() ? "CANCELLA" : "MUOVE nel cestino"));
        log.info("tin: modalita' di cancellazione " + (plugintin.getDelete() ? "CANCELLA" : "MUOVE nel cestino"));
        log.info("libero: flag di lettura " + (pluginlibero.getRead() ? "ATTIVO" : "DISATTIVO"));
        log.info("outlook 2002: timeout " + (bOutlook2002Timeout ? "ATTIVO" : "DISATTIVO"));
        log.info("supporto per rfc2047: " + (POP3Message.getrfc2047() ? "ATTIVO" : "DISATTIVO"));
        log.info("-----------------------------------------------------------------------------");
        Properties p = System.getProperties();
        Enumeration keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) p.get(key);
            log.info(key + ": " + PluginBase.replace(PluginBase.replace(PluginBase.replace(value, "\r", "\\r"), "\n", "\\n"), "\t", "\\t"));
        }
        log.info("-----------------------------------------------------------------------------");
    }

    private boolean isRestart = false;

    /**
     *
     */
    public void restart() {
        try {
            isRestart = true;
            cc.finish();
        } catch (Throwable e) {
        }
    }

    class configChange extends Thread {

        long timestamp;
        html2pop3 parent;
        boolean bLoop;

        public configChange(html2pop3 p) {
            timestamp = 0;
            parent = p;
            bLoop = true;
        }

        public void finish() {
            bLoop = false;
        }

        public void run() {
            try {
                while (bLoop) {
                    File f = new File(parent.getConfigFullPath());
                    if (timestamp == 0) {
                        timestamp = f.lastModified();
                    } else if (timestamp != f.lastModified()) {
                        parent.load();
                        parent.restart();
                        log.info("Config change ...");
                        bLoop = false;
                    }
                    if (bLoop) {
                        Thread.sleep(1000);
                    }
                }
            } catch (Throwable e) {
            }
        }
    }
}
