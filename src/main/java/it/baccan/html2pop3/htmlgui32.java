/*
 * HTML2POP3 server - win32 gui
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
package it.baccan.html2pop3;

/**
 * Title: WIN32 gui Description: Server POP3 Copyright: Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

import it.baccan.html2pop3.utils.*;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Matteo
 */
@Slf4j
public class htmlgui32 {

    /**
     *
     * @param args
     */
    public static void main(String args[]) {
        // Imposto l'eventuale config
        html2pop3.parseCommandLine(args);

        // Lancio il server
        final html2pop3 html2pop3 = new html2pop3();
        html2pop3.start();

        /* Use an appropriate Look and Feel */
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            log.error("Error", ex);
        } catch (IllegalAccessException ex) {
            log.error("Error", ex);
        } catch (InstantiationException ex) {
            log.error("Error", ex);
        } catch (ClassNotFoundException ex) {
            log.error("Error", ex);
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        //Schedule a job for the event-dispatching thread:
        //adding TrayIcon.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                htmlgui32 g = new htmlgui32();
                g.createAndShowGUI(html2pop3, html2pop3.getConfigPath() + "html2pop3.log", html2pop3.getConfigPath() + "config.cfg");
            }
        });

    }

    private void createAndShowGUI(html2pop3 html2pop3, final String cLogPath, final String cCfgPath) {
        //Check the SystemTray support
        if (!SystemTray.isSupported()) {
            log.info("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();

        final TrayIcon trayIcon = new TrayIcon(createImage("/html2pop3.gif", "tray icon"));
        trayIcon.setImageAutoSize(false);
        trayIcon.setToolTip("HTML2POP3 " + Version.getVersion() + " su " + html2pop3.getHost() + ":" + html2pop3.getPort() + "/" + html2pop3.getPortSMTP() + "/" + html2pop3.getPortNNTP() + ". Esci con click destro");

        final SystemTray tray = SystemTray.getSystemTray();

        ///*
        // Create a popup menu components
        MenuItem aboutItem = new MenuItem("About HTML2POP3 " + Version.getVersion());
        MenuItem baccanItem = new MenuItem("Verifica aggiornamenti su Baccan.it");
        //MenuItem forumItem = new MenuItem("Forum di supporto");
        MenuItem infoItem = new MenuItem("Visualizza html2pop3.log");
        MenuItem regaloItem = new MenuItem("Fai un regalo all'autore");
        MenuItem exitItem = new MenuItem("Exit");

        //Add components to popup menu
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(baccanItem);
        //popup.add(forumItem);
        popup.add(infoItem);
        popup.addSeparator();
        popup.add(regaloItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.info("TrayIcon could not be added.");
            return;
        }

        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "HTML2POP3 " + Version.getVersion() + " by Matteo Baccan http://www.baccan.it");
            }
        });

        regaloItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                htmlgui32.upenUrl("http://www.amazon.it/registry/wishlist/1K93QPV77925P");
            }
        });

        baccanItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                htmlgui32.upenUrl("http://www.baccan.it");
            }
        });

        /*
      forumItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            htmlgui32.upenUrl("http://freepops.diludovico.it/forumdisplay.php?15-HTML2POP3");
         }
      });
         */
        infoItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new MsgBox("HTML2POP3 " + Version.getVersion() + " win32",
                        "File di log: " + cLogPath + "\nFile di configurazione: " + cCfgPath + "\n", true, readCache(cLogPath));
            }
        });

        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                log.info("Exit from trybar");
                tray.remove(trayIcon);
                System.exit(0);
            }
        });

    }

    /**
     *
     * @param url
     */
    public static void upenUrl(String url) {
        boolean bRun = false;
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    URI uri = new URI(url);
                    desktop.browse(uri);
                    bRun = true;
                } catch (Exception ee) {
                    log.error(ee.getMessage());
                }
            }
        }
        if (!bRun) {
            JOptionPane.showMessageDialog(null, url);
        }
    }

    //Obtain the image URL
    /**
     *
     * @param path
     * @param description
     * @return
     */
    protected static Image createImage(String path, String description) {
        URL imageURL = htmlgui32.class.getResource(path);

        if (imageURL == null) {
            log.error("Resource not found: " + path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    private static String readCache(String cFile) {
        String cRet = null;
        try {
            File oFile = new File(cFile);
            if (oFile.exists()) {
                FileInputStream fInput = new FileInputStream(cFile);
                byte[] bufferSO = new byte[fInput.available()];
                fInput.read(bufferSO);
                fInput.close();
                cRet = new String(bufferSO);
            }
        } catch (Throwable ex) {
            log.info("readCache", ex);
        }
        return cRet;
    }
}
