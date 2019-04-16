/**
 * Title: Libero HTML2POP3 Description: Version class Copyright: Copyright (c) 2004 Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.utils;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import lombok.Getter;

/**
 *
 * @author matteo
 */
public class MsgBox extends JDialog implements ActionListener {

    @Getter private boolean id = false;
    private Button ok, can;

    /**
     *
     * @param title
     * @param msg
     * @param okcan
     */
    public MsgBox(String title, String msg, boolean okcan) {
        this(title, msg, okcan, null);
    }

    /**
     *
     * @param title
     * @param message
     * @param okcan
     * @param cText
     */
    public MsgBox(String title, String message, boolean okcan, String cText) {
        super(null, title, Dialog.ModalityType.APPLICATION_MODAL);

        String msg = message;
        GridBagConstraints c = new GridBagConstraints();
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        int nPos = 0;
        while (nPos != -1) {
            nPos = msg.indexOf("\n");
            Label l;
            if (nPos == -1) {
                l = new Label(msg);
            } else {
                l = new Label(msg.substring(0, nPos));
                msg = msg.substring(nPos + 1);
            }
            if (l.getText().indexOf("<b>") != -1) {
                l.setFont(new Font("Arial", Font.BOLD, 12));
                l.setText(l.getText().replace("<b>", ""));
            }
            //l.setAlignment(Label.CENTER);
            l.setAlignment(Label.LEFT);
            gridbag.setConstraints(l, c);
            add(l);
        }

        if (cText != null) {
            Label l = new Label("Log di esecuzione - in caso di errori, manda il log all'autore del programma");
            l.setAlignment(Label.CENTER);
            //l.set
            gridbag.setConstraints(l, c);
            add(l);

            TextArea position = new TextArea(15, 120);
            //position.setLocation(5,5);
            //position.setSize(150,1200);
            position.setBackground(Color.white);
            position.setText(cText);
            position.setEditable(false);
            position.setFont(new Font("Courier", Font.PLAIN, 10));

            //c.gridwidth = GridBagConstraints.REMAINDER;
            gridbag.setConstraints(position, c);

            add(position);
        }

        Panel p = new Panel();
        p.setLayout(new FlowLayout());

        p.add(ok = new Button("OK"));
        ok.addActionListener(this);

        if (okcan) {
            p.add(can = new Button("Cancel"));
            can.addActionListener(this);
        }

        //c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(p, c);
        add(p);

        //pack();
        //Dimension d = getToolkit().getScreenSize();
        //setLocation(d.width/4,d.height/3);
        pack();

        Dimension dim = getToolkit().getScreenSize();
        Rectangle abounds = getBounds();
        setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);

        setVisible(true);
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == ok) {
            id = true;
            setVisible(false);
        } else if (ae.getSource() == can) {
            setVisible(false);
        }
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        new MsgBox("Titolom", "Ciao\npippo\npluto", true, "xxxxxxxxxxxxxx\r\nssssssssssssss\r\nrrrrrr");
    }
}
