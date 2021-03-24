/*
 * Hotmail/Webdav plugin
 *
 * Copyright 2003 Matteo Baccan
 * www - https://www.baccan.it
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
/**
 * Title:        Hotmail HTML2POP3
 * Description:  Convertitore da HTML a POP3 per infinito.it
 * Copyright:    Copyright (c) 2003
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin.pop3;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class PluginHotmail extends POP3Base implements POP3Plugin {

    /**
     *
     * @param cUser
     * @param cPwd
     * @return
     */
    public boolean login(String cUser, String cPwd) {
        /*
        boolean bRet = false;
        try {
           log.error( "Hotmail: login init" );

           String currVersion = HttpMail.getVersion();

           log.info( "Hotmail: httpmail: " +currVersion.replace('\n','-') );

           hotmail = new HttpMail();

           if( System.getProperty("http.proxyHost","").length()>0 )
              hotmail.setProxyServer(System.getProperty("http.proxyHost",""), Double.valueOf( System.getProperty("http.proxyPort","")).intValue());
              if( System.getProperty("proxyUser","").length()>0 )
                 hotmail.setProxyUserInformation(System.getProperty("proxyUser"    ,""), System.getProperty("proxyPassword",""));

           boolean k = hotmail.open( cUser, cPwd );
           if( k ){
              k = hotmail.connect();
              if( k ){
                 Vector folders;
                 folders = hotmail.getFolders();

                 log.error( "Hotmail: login inbox" );

                 fInfo = (FolderInfo)folders.elementAt(0);

                 log.error( "Hotmail: real folder " +fInfo.FolderName );

                 k = hotmail.parseMailboxInfo(fInfo.FolderURL);
                 if( k ){
                     log.error( "Hotmail: get messages" );
                     //Vector aEmail; = new Vector();
                     Vector aEmail = hotmail.getMessages();

                     for( int nPos=0; nPos<aEmail.size(); nPos++ ){
                        MailInfo mail = (MailInfo)aEmail.elementAt( nPos );
                        addEmailInfo( mail.MessageURL, mail.Length );
                     }
                 }
                 bRet = true;
               }
            }
            log.error( "Hotmail: login end" );
        } catch (Throwable ex) {
            log.error("Error",ex);
        }
        return bRet;
        //*/
        return false;
    }

    /**
     *
     * @param nPos
     * @param nLine
     * @param bAll
     * @return
     */
    public String getMessage(int nPos, int nLine, boolean bAll) {
        /*
        StringBuffer oMail = new StringBuffer();
        try {
            log.error( "Hotmail: getmail init" );

            String cMsgId = getMessageID( nPos );

            log.error( "Hotmail: getmail ID (" +cMsgId +")" );

            oMail.append( hotmail.getMessage( cMsgId ) );

            log.error( "Hotmail: getmail end" );
        } catch (Throwable ex) {
            oMail = null;
            log.error("Error",ex);
        }
        return (oMail==null?null:lineFormat.format(oMail.toString()));
        //*/
        return "";
    }

    /**
     *
     * @param nPos
     * @return
     */
    public boolean delMessage(int nPos) {
        /*
        boolean bRet = false;
        try {
            log.error( "Hotmail: delmessage" );

            String cMsgId = getMessageID( nPos );

            log.error( "Hotmail: delmessage " +cMsgId );

            Vector mailToDelete = new Vector();
            mailToDelete.addElement( cMsgId );
            hotmail.deleteMail(mailToDelete, fInfo.FolderURL);

            log.error( "Hotmail: delmessage end" );
        } catch (Throwable ex) {
            log.error("Error",ex);
        }
        return bRet;
        //*/
        return false;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        PluginHotmail infinito = new PluginHotmail();
        if (infinito.login(args[0], args[1])) {
            int nNum = infinito.getMessageNum();
            int nSiz = infinito.getMessageSize();
            log.info("getMessageNum  :" + nNum);
            log.info("getMessageSize :" + nSiz);
            for (int nPos = 1; nPos <= nNum; nPos++) {
                log.info("getMessageID   (" + nPos + "):" + infinito.getMessageID(nPos));
                log.info("getMessageSize (" + nPos + "):" + infinito.getMessageSize(nPos));
                log.info("getMessage     (" + nPos + "):" + infinito.getMessage(nPos));
            }
        }
    }

}
