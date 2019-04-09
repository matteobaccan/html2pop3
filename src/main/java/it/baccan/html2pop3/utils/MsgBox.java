/**
 * Title: Libero HTML2POP3 Description: Version class Copyright: Copyright (c) 2004 Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.utils;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 *
 * @author matteo
 */
public class MsgBox extends JDialog implements ActionListener {

    /**
     *
     */
    public boolean id = false;
    Button ok, can;

    /**
     *
     * @param tit
     * @param msg
     * @param okcan
     */
    public MsgBox(String tit, String msg, boolean okcan) {
        this(tit, msg, okcan, null);
    }

    /**
     *
     * @param tit
     * @param msg
     * @param okcan
     * @param cText
     */
    public MsgBox(String tit, String msg, boolean okcan, String cText) {
        //super(new JFrame(""), tit, true);
        super(null, Dialog.ModalityType.APPLICATION_MODAL);
        //int nRow = msg.length()-string.replace(msg,"\n","").length();
        //int nRow = msg.length()-msg.replaceAll("\n","").length();

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
                l.setText(string.replace(l.getText(), "<b>", ""));
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

        if (okcan == true) {
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
    public static void main(String args[]) {
        new MsgBox("Titolom", "Ciao\npippo\npluto", true, "xxxxxxxxxxxxxx\r\nssssssssssssss\r\nrrrrrr");
    }
}
