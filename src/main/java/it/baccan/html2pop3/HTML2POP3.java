/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
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

import it.baccan.html2pop3.plugin.nntp.PluginNNTP;
import it.baccan.html2pop3.plugin.pop3.PluginTin;
import it.baccan.html2pop3.plugin.pop3.PluginLibero;
import it.baccan.html2pop3.plugin.pop3.PluginPOP3;
import it.baccan.html2pop3.plugin.pop3.PluginRSS;
import it.baccan.html2pop3.plugin.pop3.PluginTiscali;
import it.baccan.html2pop3.plugin.smtp.PluginSMTP;
import it.baccan.html2pop3.utils.FakeX509TrustManager;
import it.baccan.html2pop3.utils.Filter;
import it.baccan.html2pop3.utils.Version;
import it.baccan.html2pop3.utils.message.POP3Message;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class HTML2POP3 extends Thread {

    private static String config = "config.cfg";
    //private static String configPath = "";
    private Filter pop3IpFilter = new Filter();
    private Filter pop3PluginFilter = new Filter();
    private Filter pop3UserFilter = new Filter();
    private Filter pop3GlobalFilter = new Filter();
    private Filter smtpIpFilter = new Filter();
    private Filter smtpPluginFilter = new Filter();
    private Filter smtpUserFilter = new Filter();
    private Filter smtpGlobalFilter = new Filter();
    private Filter nntpIpFilter = new Filter();
    private final SortedProperties p = new SortedProperties();
    @Getter
    @Setter
    private String host = "127.0.0.1";
    @Getter
    @Setter
    private int port = 110;
    @Getter
    @Setter
    private int portSMTP = 25;
    @Getter
    @Setter
    private int portNNTP = 119;
    private int nClient = 10;
    @Getter
    @Setter
    private boolean delete = true;
    @Getter
    @Setter
    private boolean deleteOptimized = true;
    @Getter
    @Setter
    private boolean guiError = true;
    private boolean bLifo = true;
    @Getter
    @Setter
    private boolean outlook2002Timeout = true;
    @Getter
    @Setter
    private int maxEmail = -1;
    @Getter
    @Setter
    private boolean debug = false;
    private configChange cc;
    private boolean isRestart = false;

    /**
     *
     * @param args
     */
    public static void main(String args[]) {
        // Creo un oggetto html2pop3
        HTML2POP3 html2pop3 = HTML2POP3.getInstance();
        // Imposto l'eventuale config
        html2pop3.parseCommandLine(args);
        // Carico le properties
        html2pop3.load();
        // Errori in GUI
        html2pop3.setGuiError(false);
        // Start demone
        html2pop3.start();
    }

    /**
     *
     * @return
     */
    public static String getConfig() {
        return config;
    }

    //public static String getConfigPath() {
    //    return configPath;
    //}
    /**
     * Change config filename.
     *
     * @param config new Config filename
     */
    public static void setConfig(final String config) {
        HTML2POP3.config = config;
    }

    /**
     *
     * @param args
     */
    public void parseCommandLine(String args[]) {
        for (int nPar = 0; nPar < args.length; nPar++) {
            if (args[nPar].equalsIgnoreCase("-config") && nPar + 1 < args.length) {
                HTML2POP3.setConfig(args[nPar + 1]);
            }
        }
    }

    /**
     * @conditional (JVM14)
     */
    public void trustAll() {
        try {
            // Creo un TrustManager
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{new FakeX509TrustManager()};

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

        @Override
        public synchronized Enumeration keys() {
            Enumeration keysEnum = super.keys();
            Vector<String> keyList = new Vector<>();
            while (keysEnum.hasMoreElements()) {
                keyList.add((String) keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return keyList.elements();
        }

    }

    private static HTML2POP3 instance = null;

    private HTML2POP3() {
        // Trust di qualsiasi sito, non controlla la fonte SSL
        trustAll();
    }

    /**
     *
     * @return
     */
    public synchronized static HTML2POP3 getInstance() {
        if (instance == null) {
            instance = new HTML2POP3();
        }
        return instance;
    }

    /**
     *
     */
    public void exitFromProgram() {
        System.exit(0);
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
     * @return
     */
    public boolean load() {
        boolean bRet = false;
        try {
            log.info("Load Config [{}]", config);
            try (InputStream fis = new FileInputStream(config)) {
                p.clear();
                p.load(fis);
            }

            // Host + port
            this.host = p.getProperty("host", "127.0.0.1");
            this.port = Double.valueOf(p.getProperty("port", "110")).intValue();
            this.portSMTP = Double.valueOf(p.getProperty("portsmtp", "25")).intValue();
            this.portNNTP = Double.valueOf(p.getProperty("portnntp", "119")).intValue();
            this.nClient = Double.valueOf(p.getProperty("concurrentclient", "10")).intValue();
            this.delete = p.getProperty("delete", "true").equalsIgnoreCase("true");
            this.deleteOptimized = p.getProperty("deleteoptimized", "true").equalsIgnoreCase("true");
            this.bLifo = p.getProperty("coda", "lifo").equalsIgnoreCase("lifo");
            setOutlook2002Timeout(p.getProperty("outlook2002.timeout", "true").equalsIgnoreCase("true"));
            this.debug = p.getProperty("debug", "false").equalsIgnoreCase("true");

            // Email per session
            this.maxEmail = Double.valueOf(p.getProperty("maxdownloadpersession", "-1")).intValue();

            // POP3 default
            POP3Message.setAddHTML(p.getProperty("htmlattach", "true").equalsIgnoreCase("true"));

            // RSS
            PluginRSS.setConfig(getConfigPath(), "rss.cfg");

            // NNTP
            PluginNNTP.setConfig(getConfigPath(), "nntp.cfg");

            // Plugin specific
            PluginTiscali.setDelete(p.getProperty("tiscali.delete", "true").equalsIgnoreCase("true"));
            PluginTin.setDelete(p.getProperty("tin.delete", "true").equalsIgnoreCase("true"));
            PluginLibero.setRead(p.getProperty("libero.read", "false").equalsIgnoreCase("true"));

            // RFC2047
            POP3Message.setrfc2047(p.getProperty("rfc2047", "true").equalsIgnoreCase("true"));

            // Tunneling server
            PluginPOP3.setDefaultServer(p.getProperty("tunnelingserver", "https://www.baccan.it/pop3/"));
            PluginSMTP.setDefaultServer(p.getProperty("tunnelingserver", "https://www.baccan.it/pop3/"));

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
        } catch (FileNotFoundException fnf) {

            log.info("Non riesco a leggere il file " + getConfigFullPath() + ", lanciare html2pop3 dalla directory di " + config);

        } catch (IOException e) {
            log.error("Error", e);
        }
        return bRet;
    }

    private Filter getFilter(String cRoot) {
        Filter ret = new Filter();

        int nFilter = 1;
        while (true) {
            String cFilter = p.getProperty(cRoot + nFilter, "");
            int nSep = cFilter.indexOf(";");
            if (nSep <= 0) {
                break;
            }

            String cRule = cFilter.substring(0, nSep);
            cFilter = cFilter.substring(nSep + 1);

            List<String> v = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(cFilter, ";");
            while (st.hasMoreTokens()) {
                v.add(st.nextToken());
            }

            String[] o = new String[v.size()];
            for (int nPos = 0; nPos < o.length; nPos++) {
                o[nPos] = v.get(nPos);
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
    public Filter getPOP3IpFilter() {
        return pop3IpFilter;
    }

    /**
     *
     * @return
     */
    public Filter getPOP3PluginFilter() {
        return pop3PluginFilter;
    }

    /**
     *
     * @return
     */
    public Filter getPOP3UserFilter() {
        return pop3UserFilter;
    }

    /**
     *
     * @return
     */
    public Filter getPOP3GlobalFilter() {
        return pop3GlobalFilter;
    }

    /**
     *
     * @return
     */
    public Filter getSMTPIpFilter() {
        return smtpIpFilter;
    }

    /**
     *
     * @return
     */
    public Filter getSMTPPluginFilter() {
        return smtpPluginFilter;
    }

    /**
     *
     * @return
     */
    public Filter getSMTPUserFilter() {
        return smtpUserFilter;
    }

    /**
     *
     * @return
     */
    public Filter getSMTPGlobalFilter() {
        return smtpGlobalFilter;
    }

    /**
     *
     * @return
     */
    public Filter getNNTPIpFilter() {
        return nntpIpFilter;
    }

    private String getConfigFullPath() {
        String cRet = "";
        try {
            File file = new File(config);
            cRet = file.getAbsolutePath();
        } catch (Throwable e) {
            log.error("getConfigFullPath", e);
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
            log.error("getConfigPath", e);
        }
        return cRet;
    }

    /**
     *
     */
    public void save() {
        try {
            // Host + port
            p.put("host", host);
            p.put("port", "" + port);
            p.put("portsmtp", "" + portSMTP);
            p.put("portnntp", "" + portNNTP);
            p.put("concurrentclient", "" + nClient);
            p.put("delete", "" + delete);
            p.put("deleteoptimized", "" + deleteOptimized);
            p.put("debug", "" + debug);

            if (bLifo) {
                p.put("coda", "lifo");
            } else {
                p.put("coda", "fifo");
            }

            // POP3 default
            p.put("htmlattach", "" + POP3Message.getAddHTML());

            // Email per sessione
            p.put("maxdownloadpersession", "" + maxEmail);

            // Proxy
            p.put("proxyuser", System.getProperty("proxyUser", ""));
            p.put("proxypassword", System.getProperty("proxyPassword", ""));

            // proxy + port
            p.put("proxyhost", System.getProperty("http.proxyHost", ""));
            p.put("proxyport", System.getProperty("http.proxyPort", ""));

            // DEPRECATA la uso per retrocompatiblita' con JDK MS e vecchi JDK
            p.store(new FileOutputStream(config), null);
        } catch (Throwable e) {
            log.error("Error", e);
            log.info(e.getMessage());
        }
    }

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
        String cVer = Version.getVersion();
        while (cVer.length() < 6) {
            cVer = " " + cVer;
        }
        log.info("+---------------------------------------------------------------------------+");
        log.info("| POP3/SMTP/NNTP simulation server                           Version " + cVer + " |");
        log.info("| Matteo Baccan Opensource Software                   https://www.baccan.it |");
        log.info("+---------------------------------------------------------------------------+");
        log.info("Java Runtime: " + System.getProperty("java.runtime.name"));
        log.info("Java Version: " + System.getProperty("java.vm.version"));
        log.info("Config path: " + getConfigFullPath());

        if (port > 0) {
            log.info("Server POP3 ready at " + host + ":" + port + " max clients: " + nClient);
        } else {
            log.info("Server POP3 disabled");
        }

        if (portSMTP > 0) {
            log.info("Server SMTP ready at " + host + ":" + portSMTP + " max clients: " + nClient);
        } else {
            log.info("Server SMTP disabled");
        }

        if (portNNTP > 0) {
            log.info("Server NNTP ready at " + host + ":" + portNNTP + " max clients: " + nClient);
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

        if (delete) {
            log.info("Le cancellazioni SONO abilitate, il client di posta potra' cancellare la posta");
            if (deleteOptimized) {
                log.info("Le cancellazioni sono fatte DOPO la sconnessione del client di posta");
            } else {
                log.info("Le cancellazioni sono fatte PRIMA della sconnessione del client di posta");
            }
        } else {
            log.info("Le cancellazioni NON sono abilitate, il client di posta non potra' cancellare la posta");
        }

        if (guiError) {
            log.info("Errori gravi visualizzati con messagebox GUI e file di log");
        } else {
            log.info("Errori gravi visualizzati solo nel file di log");
        }
        if (debug) {
            log.info("Modalita' di debug ATTIVA");
        } else {
            log.info("Modalita' di debug disattiva");
        }
        log.info("Attach email originale nella posta emulata: " + POP3Message.getAddHTML());
        log.info("Numero di download massimi per sessione: " + maxEmail);
        log.info("Tunneling server " + p.getProperty("tunnelingserver", "https://www.baccan.it/pop3/"));
        log.info("-----------------------------------------------------------------------------");
        log.info("Plugin specific setting");
        log.info("-----------------------------------------------------------------------------");
        log.info("tiscali: modalita' di cancellazione " + (PluginTiscali.isDelete() ? "CANCELLA" : "MUOVE nel cestino"));
        log.info("tin: modalita' di cancellazione " + (PluginTin.isDelete() ? "CANCELLA" : "MUOVE nel cestino"));
        log.info("libero: flag di lettura " + (PluginLibero.getRead() ? "ATTIVO" : "DISATTIVO"));
        log.info("outlook 2002: timeout " + (isOutlook2002Timeout() ? "ATTIVO" : "DISATTIVO"));
        log.info("supporto per rfc2047: " + (POP3Message.getrfc2047() ? "ATTIVO" : "DISATTIVO"));
        log.info("-----------------------------------------------------------------------------");
        Properties p = System.getProperties();
        Enumeration keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) p.get(key);
            log.info("[{}]: [{}]", key, value.replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t"));
        }
        log.info("-----------------------------------------------------------------------------");
    }

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

        private long timestamp;
        private HTML2POP3 parent;
        private boolean bLoop;

        public configChange(HTML2POP3 p) {
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
