/*
 * htmlgui.java
 *
 * Created on 26 ottobre 2003, 16.39
 */
package it.baccan.html2pop3;

import it.baccan.html2pop3.utils.message.*;

/**
 *
 * @author Matteo Baccan
 */
public class htmlgui extends javax.swing.JFrame {

    /**
     * Creates new form htmlgui
     */
    private html2pop3 h2p = null;

    /**
     *
     */
    public htmlgui() {
        initComponents();

        oTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if (oTab.getSelectedIndex() == 3) {
                    scrollToEnd();
                }
            }
        });

        h2p = new html2pop3();
        h2p.start();

        oHost.setText(h2p.getHost());
        oPort.setText("" + h2p.getPort());
        oDelete.setSelected(h2p.getDelete());
        oProxyUser.setText(System.getProperty("proxyUser"));
        oProxyPassword.setText(System.getProperty("proxyPassword"));
        oProxyHost.setText(System.getProperty("http.proxyHost"));
        oProxyPort.setText(System.getProperty("http.proxyPort"));
        oConcurrentClient.setText("" + h2p.getClient());
        oCoda.setSelectedItem(h2p.getLifo() ? "lifo" : "fifo");
        oDeleteOptimized.setSelected(h2p.getDeleteOptimized());
        oHTMLAttach.setSelected(POP3Message.getAddHTML());
        oSessionEmail.setText("" + h2p.getMaxEmail());

    }

    private class PrintStreamMemo extends java.io.PrintStream {

        private java.io.PrintStream pParent = null;

        public PrintStreamMemo(java.io.PrintStream out) {
            super(System.out);
            pParent = out;
        }

        public void write(byte buf[], int off, int len) {
            pParent.write(buf, off, len);
            oLog.append(new String(buf, off, len));
            scrollToEnd();
        }

        public void println(String x) {
            pParent.println(x);
            oLog.append(x + "\r\n");
            scrollToEnd();
        }

        public void write(int b) {
            pParent.write(b);
            oLog.append("" + b);
            scrollToEnd();
        }
    }

    private void scrollToEnd() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        jScrollPane1.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jTextPane1 = new javax.swing.JTextPane();
        oTab = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        oHost = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        oPort = new javax.swing.JTextField();
        jTextPane2 = new javax.swing.JTextPane();
        oConcurrentClient = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        oCoda = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        oProxyHost = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        oProxyPort = new javax.swing.JTextField();
        jTextPane3 = new javax.swing.JTextPane();
        jLabel7 = new javax.swing.JLabel();
        oProxyUser = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        oProxyPassword = new javax.swing.JPasswordField();
        jPanel5 = new javax.swing.JPanel();
        jTextPane4 = new javax.swing.JTextPane();
        oDelete = new javax.swing.JCheckBox();
        oDeleteOptimized = new javax.swing.JCheckBox();
        oHTMLAttach = new javax.swing.JCheckBox();
        oSessionEmail = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        oLog = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jList1 = new javax.swing.JList();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("HTML 2 POP3");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });
        getContentPane().setLayout(null);

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("HTML 2 POP3");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(0, 0, 300, 30);

        jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel1.setLayout(null);

        jTextPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane1.setEditable(false);
        jTextPane1.setText("Tramite questo programma è possibile creare un finto server POP3 in grado di interfacciarsi alle pagine HTML di libero.it o infinito.it e permettere l'uso della posta, anche in presenza di firewall o di chiusura della porta POP3. Fare riferimento alla documentazione per i dettagli");
        jPanel1.add(jTextPane1);
        jTextPane1.setBounds(10, 10, 470, 70);

        jPanel2.setLayout(null);

        jLabel3.setText("Server");
        jPanel2.add(jLabel3);
        jLabel3.setBounds(10, 10, 60, 16);

        oHost.setText("localhost");
        oHost.setToolTipText("per indicare un server locale inserire localhost o 127.0.0.1");
        jPanel2.add(oHost);
        oHost.setBounds(70, 10, 220, 22);

        jLabel4.setText("Porta");
        jPanel2.add(jLabel4);
        jLabel4.setBounds(10, 30, 60, 16);

        oPort.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        oPort.setText("110");
        oPort.setToolTipText("per usare la porta di default indicare 110");
        jPanel2.add(oPort);
        oPort.setBounds(70, 30, 50, 22);

        jTextPane2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane2.setEditable(false);
        jTextPane2.setText("Indicare l'indirizzo e la porta del server POP3 virtuale. Se non sapete cosa mettere e' buon segno, lasciate su localhost");
        jPanel2.add(jTextPane2);
        jTextPane2.setBounds(10, 60, 540, 70);

        oConcurrentClient.setText("50");
        oConcurrentClient.setToolTipText("Indica il numero di computer che possono connettersi al server in parallelo");
        jPanel2.add(oConcurrentClient);
        oConcurrentClient.setBounds(430, 10, 30, 22);

        jLabel9.setText("Client concorrenti");
        jPanel2.add(jLabel9);
        jLabel9.setBounds(310, 10, 110, 16);

        jLabel10.setText("Coda");
        jPanel2.add(jLabel10);
        jLabel10.setBounds(310, 30, 60, 16);

        oCoda.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "lifo", "fifo" }));
        oCoda.setToolTipText("Rappresenta l'ordine di download dei messaggi");
        jPanel2.add(oCoda);
        oCoda.setBounds(430, 30, 70, 22);

        oTab.addTab("Server", jPanel2);

        jPanel3.setLayout(null);

        jLabel5.setText("Proxy");
        jPanel3.add(jLabel5);
        jLabel5.setBounds(10, 10, 41, 16);

        oProxyHost.setToolTipText("indicare il nome dell'eventuale proxy");
        jPanel3.add(oProxyHost);
        oProxyHost.setBounds(70, 10, 220, 22);

        jLabel6.setText("Porta");
        jPanel3.add(jLabel6);
        jLabel6.setBounds(10, 30, 30, 16);

        oProxyPort.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        oProxyPort.setToolTipText("indicare il numero dell'eventuale porta del proxy");
        jPanel3.add(oProxyPort);
        oProxyPort.setBounds(70, 30, 50, 22);

        jTextPane3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane3.setEditable(false);
        jTextPane3.setText("In caso di proxy HTTP indicarne l'indirizzo e la porta e poi chiedere un restart");
        jPanel3.add(jTextPane3);
        jTextPane3.setBounds(10, 60, 540, 70);

        jLabel7.setText("Utente");
        jPanel3.add(jLabel7);
        jLabel7.setBounds(330, 10, 41, 16);

        oProxyUser.setToolTipText("indicare il nome dell'eventuale proxy");
        jPanel3.add(oProxyUser);
        oProxyUser.setBounds(400, 10, 120, 20);

        jLabel8.setText("Password");
        jPanel3.add(jLabel8);
        jLabel8.setBounds(330, 30, 60, 16);

        oProxyPassword.setToolTipText("indicare il numero dell'eventuale porta del proxy");
        jPanel3.add(oProxyPassword);
        oProxyPassword.setBounds(400, 30, 120, 22);

        oTab.addTab("Proxy", jPanel3);

        jPanel5.setLayout(null);

        jTextPane4.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTextPane4.setEditable(false);
        jTextPane4.setText("Opzioni del programma");
        jPanel5.add(jTextPane4);
        jTextPane4.setBounds(10, 60, 540, 70);

        oDelete.setText("Abilita la cancellazione");
        oDelete.setToolTipText("Se non selezionato permette di mantenere la posta sul server");
        jPanel5.add(oDelete);
        oDelete.setBounds(10, 10, 160, 20);

        oDeleteOptimized.setText("Cancellazione ottimizzata");
        oDeleteOptimized.setToolTipText("Se ottimizzata le cancellazioni sono fatte dopo la sconnessione da html2pop3");
        jPanel5.add(oDeleteOptimized);
        oDeleteOptimized.setBounds(10, 30, 170, 25);

        oHTMLAttach.setText("HTML attach per mail emulate");
        oHTMLAttach.setToolTipText("Permette di avere in attach il testo della mail in caso di casella in emulazione");
        jPanel5.add(oHTMLAttach);
        oHTMLAttach.setBounds(180, 10, 200, 20);

        oSessionEmail.setText("-1");
        oSessionEmail.setToolTipText("Indica quante mail devono essere scaricate per sessione. -1 indica tutte le disponibili");
        jPanel5.add(oSessionEmail);
        oSessionEmail.setBounds(520, 30, 30, 22);

        jLabel11.setText("Email per sessione");
        jPanel5.add(jLabel11);
        jLabel11.setBounds(400, 30, 110, 20);

        oTab.addTab("Preferenze", jPanel5);

        jPanel4.setLayout(null);

        oLog.setFont(new java.awt.Font("Courier New", 0, 10)); // NOI18N
        jScrollPane1.setViewportView(oLog);

        jPanel4.add(jScrollPane1);
        jScrollPane1.setBounds(0, 0, 550, 160);

        oTab.addTab("Log", jPanel4);

        jPanel1.add(oTab);
        oTab.setBounds(10, 80, 560, 190);

        jButton1.setText("Restart");
        jButton1.setToolTipText("Salva le impostazioni e riattiva il server");
        jButton1.setAlignmentY(0.0F);
        jButton1.setIconTextGap(0);
        jButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);
        jButton1.setBounds(490, 10, 80, 30);
        jPanel1.add(jList1);
        jList1.setBounds(510, 60, 0, 0);

        jButton2.setText("Clear");
        jButton2.setToolTipText("Cancella la finestra dei log");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2);
        jButton2.setBounds(490, 50, 80, 30);

        getContentPane().add(jPanel1);
        jPanel1.setBounds(10, 30, 580, 280);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("by Matteo Baccan");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(310, 10, 250, 16);

        setSize(new java.awt.Dimension(599, 338));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Add your handling code here:
        // Cancellazione dei log di sistema
        oLog.setText("");
        h2p.printInfo();
        scrollToEnd();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Add your handling code here:
        // Imposta sulla pagina di log
        oTab.setSelectedIndex(3);

        boolean isNew = h2p.isAlive();
        if (!isNew) {
            h2p = new html2pop3();
        }

        //Codice di restart
        h2p.setHost(oHost.getText());
        h2p.setPort(Double.valueOf(oPort.getText()).intValue());
        h2p.setDelete(oDelete.isSelected());
        h2p.setClient(Double.valueOf(oConcurrentClient.getText()).intValue());
        h2p.setLifo(oCoda.getSelectedItem().toString().equalsIgnoreCase("lifo"));
        h2p.setDeleteOptimized(oDeleteOptimized.isSelected());
        h2p.setMaxEmail(Double.valueOf(oSessionEmail.getText()).intValue());
        POP3Message.setAddHTML(oHTMLAttach.isSelected());
        System.setProperty("proxyUser", oProxyUser.getText());
        System.setProperty("proxyPassword", oProxyPassword.getText());
        System.setProperty("http.proxyHost", oProxyHost.getText());
        System.setProperty("http.proxyPort", oProxyPort.getText());
        if (oProxyHost.getText().length() > 0) {
            System.setProperty("http.proxySet", "true");
        } else {
            System.setProperty("http.proxySet", "false");
        }
        h2p.save();

        if (!isNew) {
            h2p.start();
        } else {
            h2p.restart();
        }

        scrollToEnd();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * Exit the Application
     */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        System.exit(0);
    }//GEN-LAST:event_exitForm

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new htmlgui().show();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JList jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPane3;
    private javax.swing.JTextPane jTextPane4;
    private javax.swing.JComboBox oCoda;
    private javax.swing.JTextField oConcurrentClient;
    private javax.swing.JCheckBox oDelete;
    private javax.swing.JCheckBox oDeleteOptimized;
    private javax.swing.JCheckBox oHTMLAttach;
    private javax.swing.JTextField oHost;
    private javax.swing.JTextArea oLog;
    private javax.swing.JTextField oPort;
    private javax.swing.JTextField oProxyHost;
    private javax.swing.JPasswordField oProxyPassword;
    private javax.swing.JTextField oProxyPort;
    private javax.swing.JTextField oProxyUser;
    private javax.swing.JTextField oSessionEmail;
    private javax.swing.JTabbedPane oTab;
    // End of variables declaration//GEN-END:variables

}
