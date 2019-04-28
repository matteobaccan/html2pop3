/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
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
import it.baccan.html2pop3.utils.MsgBox;
import it.baccan.html2pop3.utils.Version;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Matteo
 */
@Slf4j
public class HTMLGUI32 {

    /**
     *
     * @param args
     */
    public static void main(String args[]) {
        // Creo un oggetto html2pop3
        final HTML2POP3 html2pop3 = HTML2POP3.getInstance();        
        // Imposto l'eventuale config
        html2pop3.parseCommandLine(args);
        // Carico le properties
        html2pop3.load();
        // Errori in GUI
        html2pop3.setGuiError(true);
        // Start demone
        html2pop3.start();

        /* Use an appropriate Look and Feel */
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            log.error("Error", ex);
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        //Schedule a job for the event-dispatching thread:
        //adding TrayIcon.
        SwingUtilities.invokeLater(() -> {
            HTMLGUI32 g = new HTMLGUI32();
            g.createAndShowGUI(html2pop3, html2pop3.getConfigPath() + "html2pop3.log", html2pop3.getConfigPath() + "config.cfg");
        });

    }

    private void createAndShowGUI(HTML2POP3 html2pop3, final String cLogPath, final String cCfgPath) {
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

        // Create a popup menu components
        MenuItem aboutItem = new MenuItem("About HTML2POP3 " + Version.getVersion());
        MenuItem baccanItem = new MenuItem("Vai al sito dell'autore: Baccan.it");
        MenuItem githubItem = new MenuItem("Scarica l'ultima versione da Github");
        MenuItem infoItem = new MenuItem("Visualizza html2pop3.log");
        MenuItem regaloItem = new MenuItem("Fai un regalo all'autore");
        MenuItem exitItem = new MenuItem("Exit");

        //Add components to popup menu
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(baccanItem);
        popup.add(githubItem);
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

        aboutItem.addActionListener((ActionEvent e) -> {
            JOptionPane.showMessageDialog(null, "HTML2POP3 " + Version.getVersion() + " by Matteo Baccan http://www.baccan.it");
        });

        regaloItem.addActionListener((ActionEvent e) -> {
            HTMLGUI32.upenUrl("http://www.amazon.it/registry/wishlist/1K93QPV77925P");
        });

        baccanItem.addActionListener((ActionEvent e) -> {
            HTMLGUI32.upenUrl("http://www.baccan.it");
        });

        githubItem.addActionListener((ActionEvent e) -> {
            HTMLGUI32.upenUrl("https://github.com/matteobaccan/html2pop3/releases");
        });

        infoItem.addActionListener((ActionEvent e) -> {
            new MsgBox("HTML2POP3 " + Version.getVersion() + " win32",
                    "File di log: " + cLogPath + "\nFile di configurazione: " + cCfgPath + "\n", true, readCache(cLogPath));
        });

        exitItem.addActionListener((ActionEvent e) -> {
            log.info("Exit from trybar");
            tray.remove(trayIcon);
            System.exit(0);
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
                } catch (IOException | URISyntaxException ee) {
                    log.error(ee.getMessage());
                }
            }
        }
        if (!bRun) {
            JOptionPane.showMessageDialog(null, url);
        }
    }

    /**
     * Obtain the image URL.
     *
     * @param path
     * @param description
     * @return
     */
    protected static Image createImage(String path, String description) {
        URL imageURL = HTMLGUI32.class.getResource(path);

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
                try (FileInputStream fInput = new FileInputStream(cFile)) {
                    byte[] bufferSO = new byte[fInput.available()];
                    fInput.read(bufferSO);
                    cRet = new String(bufferSO);
                }
            }
        } catch (IOException ex) {
            log.info("readCache", ex);
        }
        return cRet;
    }
}
