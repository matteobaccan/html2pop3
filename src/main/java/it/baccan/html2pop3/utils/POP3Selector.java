/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 * 
 */
package it.baccan.html2pop3.utils;

import it.baccan.html2pop3.plugin.pop3.POP3Plugin;
import it.baccan.html2pop3.plugin.pop3.PluginEmailIt;
import it.baccan.html2pop3.plugin.pop3.PluginFastwebnet;
import it.baccan.html2pop3.plugin.pop3.PluginGmail;
import it.baccan.html2pop3.plugin.pop3.PluginHotmail;
import it.baccan.html2pop3.plugin.pop3.PluginInfinito;
import it.baccan.html2pop3.plugin.pop3.PluginLibero;
import it.baccan.html2pop3.plugin.pop3.PluginLinuxIt;
import it.baccan.html2pop3.plugin.pop3.PluginPOP3;
import it.baccan.html2pop3.plugin.pop3.PluginRSS;
import it.baccan.html2pop3.plugin.pop3.PluginSupereva;
import it.baccan.html2pop3.plugin.pop3.PluginTele2;
import it.baccan.html2pop3.plugin.pop3.PluginTim;
import it.baccan.html2pop3.plugin.pop3.PluginTin;
import it.baccan.html2pop3.plugin.pop3.PluginTiscali;
import it.baccan.html2pop3.plugin.pop3.PluginVirgilio;

/**
 *
 * @author Matteo
 */
public class POP3Selector {

    private POP3Selector() {
        // Utility class
    }

    public static final String user2Server(final String user) {
        String server;
        String localUser = user.toUpperCase();
        if (localUser.contains("@LIBERO.IT")) {
            server = "libero.it";
        } else if (localUser.contains("@INWIND.IT")) {
            server = "inwind.it";
        } else if (localUser.contains("@BLU.IT")) {
            server = "blu.it";
        } else if (localUser.contains("@GIALLO.IT")) {
            server = "giallo.it";
        } else if (localUser.contains("@IOL.IT")) {
            server = "iol.it";
        } else if (localUser.contains("@INFINITO.IT")) {
            server = "infinito.it";
        } else if (localUser.contains("@GENIE.IT")) {
            server = "infinito.it";
        } else if (localUser.contains("@TISCALI.IT")) {
            server = "tiscali.it";
        } else if (localUser.contains("@FASTWEBNET.IT")) {
            server = "fastwebnet.it";
        } else if (localUser.contains("@TIN.IT")
                || localUser.contains("@ATLANTIDE.IT")
                || localUser.contains("@TIM.IT")) {
            server = "tin.it";
        } else if (localUser.contains("@VIRGILIO.IT")) {
            server = "virgilio.it";
        } else if (localUser.contains("@HOTMAIL.COM")) {
            server = "hotmail.com";
        } else if (localUser.contains("@SUPEREVA.IT")
                || localUser.contains("@FREEMAIL.IT")
                || localUser.contains("@FREEWEB.ORG")
                || localUser.contains("@SUPERSONIC.IT")
                || localUser.contains("@DADACASA.COM")
                || localUser.contains("@CONCENTO.IT")
                || localUser.contains("@CLARENCE.COM")
                || localUser.contains("@CICCIOCICCIO.COM")
                || localUser.contains("@MYBOX.IT")
                || localUser.contains("@MP4.IT")
                || localUser.contains("@SUPERDADA.COM")) {
            server = "supereva.it";
        } else if (localUser.contains("@TELE2.IT")) {
            server = "tele2.it";
        } else if (localUser.contains("@GMAIL.COM")) {
            server = "gmail.com";
        } else if (localUser.contains("@RSS")) {
            server = "rss";
        } else if (localUser.contains("@AGGREGATOR")) {
            server = "rss";
        } else if (localUser.contains("@LINUX.IT")) {
            server = "linux.it";
        } else if (localUser.contains("@EMAIL.IT")) {
            server = "email.it";
        } else {
            server = "libero.it";
        }
        return server;
    }

    public static final POP3Plugin server2POP3Plugin(final String server) {
        POP3Plugin plugin = null;
        if (server.equalsIgnoreCase("libero.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_LIBERO);
        } else if (server.equalsIgnoreCase("inwind.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_INWIND);
        } else if (server.equalsIgnoreCase("blu.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_BLU);
        } else if (server.equalsIgnoreCase("iol.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_IOL);
        } else if (server.equalsIgnoreCase("giallo.it")) {
            plugin = new PluginLibero(PluginLibero.MAIL_GIALLO);
        } else if (server.equalsIgnoreCase("infinito.it")) {
            plugin = new PluginInfinito();
        } else if (server.equalsIgnoreCase("tiscali.it")) {
            plugin = new PluginTiscali();
        } else if (server.equalsIgnoreCase("fastwebnet.it")) {
            plugin = new PluginFastwebnet();
        } else if (server.equalsIgnoreCase("email.it")) {
            plugin = new PluginEmailIt();
        } else if (server.equalsIgnoreCase("tin.it")) {
            plugin = new PluginTin();
        } else if (server.equalsIgnoreCase("virgilio.it")) {
            plugin = new PluginVirgilio();
        } else if (server.equalsIgnoreCase("tim.it")) {
            plugin = new PluginTim();
        } else if (server.equalsIgnoreCase("hotmail.com")) {
            plugin = new PluginHotmail();
        } else if (server.equalsIgnoreCase("supereva.it")) {
            plugin = new PluginSupereva();
        } else if (server.equalsIgnoreCase("tele2.it")) {
            plugin = new PluginTele2();
        } else if (server.equalsIgnoreCase("gmail.com")) {
            plugin = new PluginGmail();
        } else if (server.equalsIgnoreCase("linux.it")) {
            plugin = new PluginLinuxIt();
        } else if (server.equalsIgnoreCase("pop3")) {
            plugin = new PluginPOP3();
        } else if (server.equalsIgnoreCase("rss")) {
            plugin = new PluginRSS();
        }

        return plugin;
    }

}
