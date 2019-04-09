/*
 * plugin3base
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
/**
 * Title:        plugin3base
 * Description:  Classe base per la costruzione dei plugin
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Matteo Baccan
 * @version 1.0
 */
package it.baccan.html2pop3.plugin;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;

import it.baccan.html2pop3.utils.*;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public abstract class pluginbase {

    private boolean bIsWin32 = true;

    /**
     *
     */
    public pluginbase() {
        // Imposta il flag win32, con JDK MS non viene chiamato il metodo
        setNonWin32();
    }

    /**
     * @conditional (JVM14)
     */
    private void setNonWin32() {
        bIsWin32 = false;
    }

    /**
     * @conditional (JVM14)
     */
    private void setInstanceFollowRedirects(URLConnection con) {
        ((HttpURLConnection) con).setInstanceFollowRedirects(false);
    }

    private String cookieHeader = "";
    private String locationHeader = "";
    private String contentDispositionHeader = "";
    private String contentTypeHeader = "";

    /**
     *
     * @param con
     */
    protected void processField(URLConnection con) {
        cookieHeader = "";
        locationHeader = "";
        contentDispositionHeader = "";
        contentTypeHeader = "";

        // I cookie possono essere N .. provo a collezionarli
        int n = 1; // n=0 has no key, and the HTTP return status in the value field
        boolean done = false;
        try {
            while (!done) {
                String headerKey = con.getHeaderFieldKey(n);
                String headerVal = con.getHeaderField(n);

                if (bDebug) {
                    log.info("HEAD: " + headerKey + ": " + headerVal);
                }

                if (headerKey == null && headerVal == null) {
                    done = true;
                } else if (headerKey != null && headerVal != null) {
                    if (headerKey.equalsIgnoreCase("set-cookie")) {
                        int nSpace = headerVal.indexOf(" ");
                        if (nSpace != -1) {
                            headerVal = headerVal.substring(0, nSpace);
                        }
                        nSpace = headerVal.indexOf(";");
                        if (nSpace != -1) {
                            headerVal = headerVal.substring(0, nSpace + 1);
                        }
                        headerVal = headerVal.trim();
                        if (!headerVal.endsWith(";")) {
                            headerVal += ";";
                        }
                        cookieHeader += headerVal + " ";
                    } else if (headerKey.equalsIgnoreCase("location")) {
                        locationHeader = headerVal;
                    } else if (headerKey.equalsIgnoreCase("Content-Disposition")) {
                        contentDispositionHeader = headerVal;
                    } else if (headerKey.equalsIgnoreCase("Content-Type")) {
                        contentTypeHeader = headerVal;
                    }
                }
                n++;
            }
        } catch (Throwable e) {
            log.error("Errore su codifica cookie " + n);
        }
        cookieHeader = cookieHeader.trim();
        locationHeader = locationHeader.trim();

        //if( cookieHeader.endsWith(";") ) cookieHeader = cookieHeader.substring(0,cookieHeader.length()-1);
    }

    /**
     *
     * @return
     */
    protected String getCookie() {
        return cookieHeader;
    }

    /**
     *
     * @param cookie
     */
    protected void setCookie(String cookie) {
        cookieHeader = cookie;
    }

    /**
     *
     * @return
     */
    protected String getLocation() {
        return locationHeader;
    }

    /**
     *
     * @return
     */
    protected String getContentDisposition() {
        return contentDispositionHeader;
    }

    /**
     *
     * @return
     */
    protected String getContentType() {
        return contentTypeHeader;
    }

    /**
     *
     * @return
     */
    protected String getContentTypeCharset() {
        String cRet = "";
        int nPos = contentTypeHeader.indexOf("charset=");
        if (nPos != -1) {
            cRet = contentTypeHeader.substring(nPos + 8);
        }
        nPos = cRet.indexOf(";");
        if (nPos != -1) {
            cRet = cRet.substring(0, nPos);
        }
        //log.info("charset="+cRet);
        return cRet;
    }

    /**
     *
     * @param cUrl
     * @return
     * @throws Throwable
     */
    protected StringBuffer getPage(String cUrl) throws Throwable {
        return getPage(cUrl, null, 0, true, "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @return
     * @throws Throwable
     */
    protected StringBuffer getPage(String cUrl, String cCookie) throws Throwable {
        return getPage(cUrl, cCookie, 0, true, "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param nLine
     * @return
     * @throws Throwable
     */
    protected StringBuffer getPage(String cUrl, String cCookie, int nLine) throws Throwable {
        return getPage(cUrl, cCookie, nLine, true, "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param nLine
     * @param bAll
     * @return
     * @throws Throwable
     */
    protected StringBuffer getPage(String cUrl, String cCookie, int nLine, boolean bAll) throws Throwable {
        return getPage(cUrl, cCookie, nLine, bAll, "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param nLine
     * @param bAll
     * @param cRef
     * @return
     * @throws Throwable
     */
    protected StringBuffer getPage(String cUrl, String cCookie, int nLine, boolean bAll, String cRef) throws Throwable {
        return getPage(cUrl, cCookie, nLine, bAll, cRef, "");
    }

    ///*
    /**
     *
     * @param cUrl
     * @param cCookie
     * @param nLine
     * @param bAll
     * @param cRef
     * @param cAuthorization
     * @return
     * @throws Exception
     */
    protected StringBuffer getPage(String cUrl, String cCookie, int nLine, boolean bAll, String cRef, String cAuthorization) throws Exception {
        boolean endOfHdr_1stBlankLine = false; // [YS]
        // [YS] Signal End of Header =possibly= at First blank line
        // [YS] il piu' semplice segnale di termine dell' header e' dato dalla prima linea vuota

        if (bDebug) {
            log.info("HTTP/GET: " + cUrl);
        }
        if (bDebug) {
            log.info("Cookie: " + cCookie);
        }

        URL urlObject = new URL(cUrl);
        //URL urlObject = getNewUrl( cUrl );

        //HttpURLConnection con = (HttpURLConnection)urlObject.openConnection();
        URLConnection con = urlObject.openConnection();

        con.setRequestProperty("User-Agent", getAgent());
        con.setRequestProperty("Pragma", "no-cache");
        con.setRequestProperty("Accept-Language", "it");
        //con.setRequestProperty( "Keep-Alive", "false" );
        con.setRequestProperty("Host", urlObject.getHost());
        if (cCookie != null && cCookie.length() > 0) {
            con.setRequestProperty("Cookie", cCookie);
        }
        // Referer
        if (cRef.length() > 0) {
            con.setRequestProperty("Referer", cRef);
        }

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if (cEncode.length() > 0) {
            if (System.getProperty("proxyPassword", "").length() > 0) {
                cEncode += ":" + System.getProperty("proxyPassword", "");
            }
            cEncode = new String(it.baccan.html2pop3.utils.Base64.encode(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        if (cAuthorization.length() > 0) {
            String cEncodeA = new String(it.baccan.html2pop3.utils.Base64.encode(cAuthorization.getBytes()));
            con.setRequestProperty("Authorization", "Basic " + cEncodeA);
        }

        // Prendo I cookie
        processField(con);

        // Lettura risultato
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer sb = new StringBuffer();

        int nPos = 0;
        // Correzione di Marco Sburlati, grazie Marco (Matteo Baccan)
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine).append((char) 13).append((char) 10);

            if (inputLine.length() == 0) {
                endOfHdr_1stBlankLine = true;
            }
            if (!bAll) {
                if (endOfHdr_1stBlankLine) {
                    if (nPos == nLine) {
                        break;
                    }
                    nPos++;
                }
            }
        }

        // NON va bene .. no no no .. non posso sconnettere col jdk Microsoft!! .. ma siamo fuori?
        //con.disconnect();
        // Questo invece non da errore, ma con Microsoft e' come se comunque scaricasse
        // la pagina .. mannaggia mannaggia
        in.close();

        return sb;
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @return
     * @throws Exception
     */
    protected byte[] getPageBytes(String cUrl, String cCookie) throws Exception {
        if (bDebug) {
            log.info("HTTP/GETBYTES: " + cUrl);
        }

        URL urlObject = new URL(cUrl);
        //URL urlObject = getNewUrl( cUrl );

        //HttpURLConnection con = (HttpURLConnection)urlObject.openConnection();
        URLConnection con = urlObject.openConnection();
        con.setRequestProperty("User-Agent", getAgent());
        con.setRequestProperty("Pragma", "no-cache");
        con.setRequestProperty("Accept-Language", "it");
        //con.setRequestProperty( "Keep-Alive", "false" );
        con.setRequestProperty("Host", urlObject.getHost());
        if (cCookie != null && cCookie.length() > 0) {
            con.setRequestProperty("Cookie", cCookie);
        }

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if (cEncode.length() > 0) {
            if (System.getProperty("proxyPassword", "").length() > 0) {
                cEncode += ":" + System.getProperty("proxyPassword", "");
            }
            cEncode = new String(it.baccan.html2pop3.utils.Base64.encode(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        // Prendo I cookie
        processField(con);

        // Lettura risultato
        InputStream in = con.getInputStream();

        ByteArrayOutputStream cReply = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            cReply.write(c);
        }

        in.close();

        return cReply.toByteArray();
    }

    //protected HttpURLConnection streamPageTop( String cUrl, String cCookie ) throws Exception {
    /**
     *
     * @param cUrl
     * @param cCookie
     * @return
     * @throws Exception
     */
    protected URLConnection streamPageTop(String cUrl, String cCookie) throws Exception {
        return streamPageTop(cUrl, cCookie, "");
    }

    //protected HttpURLConnection streamPageTop( String cUrl, String cCookie, String cRef ) throws Exception {
    /**
     *
     * @param cUrl
     * @param cCookie
     * @param cRef
     * @return
     * @throws Exception
     */
    protected URLConnection streamPageTop(String cUrl, String cCookie, String cRef) throws Exception {
        if (bDebug) {
            log.info("HTTP/STREAMPAGETOP: " + cUrl);
        }
        URL urlObject = new URL(cUrl);
        //URL urlObject = getNewUrl( cUrl );

        //HttpURLConnection con = (HttpURLConnection)urlObject.openConnection();
        URLConnection con = (URLConnection) urlObject.openConnection();
        con.setRequestProperty("User-Agent", getAgent());
        con.setRequestProperty("Pragma", "no-cache");
        con.setRequestProperty("Accept-Language", "it");
        //con.setRequestProperty( "Keep-Alive", "false" );
        con.setRequestProperty("Host", urlObject.getHost());
        if (cCookie != null && cCookie.length() > 0) {
            con.setRequestProperty("Cookie", cCookie);
        }
        // Referer
        if (cRef.length() > 0) {
            con.setRequestProperty("Referer", cRef);
        }

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if (cEncode.length() > 0) {
            if (System.getProperty("proxyPassword", "").length() > 0) {
                cEncode += ":" + System.getProperty("proxyPassword", "");
            }
            cEncode = new String(it.baccan.html2pop3.utils.Base64.encode(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        // Prendo i cookie
        processField(con);

        return con;
    }

    //protected void streamPageBody( HttpURLConnection con, OutputStream SO, int nLine, boolean bAll ) throws Throwable {
    /**
     *
     * @param con
     * @param SO
     * @param nLine
     * @param bAll
     * @throws Throwable
     */
    protected void streamPageBody(URLConnection con, OutputStream SO, int nLine, boolean bAll) throws Throwable {
        streamPageBody(con, SO, nLine, bAll, true);
    }

    //protected void streamPageBody( HttpURLConnection con, OutputStream SO, int nLine, boolean bAll, boolean bPoint ) throws Throwable {
    /**
     *
     * @param con
     * @param SO
     * @param nLine
     * @param bAll
     * @param bPoint
     * @throws Throwable
     */
    protected void streamPageBody(URLConnection con, OutputStream SO, int nLine, boolean bAll, boolean bPoint) throws Throwable {
        boolean endOfHdr_1stBlankLine = false;

        // Lettura risultato
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        htmlTool html = new htmlTool();

        int nPos = 0;
        while ((inputLine = in.readLine()) != null) {
            if (bPoint) {
                // 23:19:02 mercoledi' 04 agosto 2004
                // Stupidamente quando faccio lo stream dei messaggi
                // e la riga inizia con un . lo passo al client
                // questo comportamento interrompe la comunicazione su alcuni client
                // la procedura corretta e': se inizio la riga col punto, ne aggiungo
                // uno in testa.
                // Questra regola vale sempre non solo per le righe da 1 punto
                if (inputLine.startsWith(".")) {
                    inputLine = "." + inputLine;
                }
            }

            html.putData(SO, inputLine + (char) 13 + (char) 10);

            if (inputLine.length() == 0) {
                endOfHdr_1stBlankLine = true;
            }
            if (!bAll) {
                if (endOfHdr_1stBlankLine) {
                    if (nPos == nLine) {
                        break;
                    }
                    nPos++;
                }
            }
        }

        // NON va bene .. no no no .. non posso sconnettere col jdk Microsoft!! .. ma siamo fuori?
        //con.disconnect();
        // Questo invece non da errore, ma con Microsoft e' come se comunque scaricasse
        // la pagina .. mannaggia mannaggia
        con.getInputStream().close();
    }
    //*/

    /*
    // In lavorazione
    //
    protected StringBuffer getPage( String cUrl, String cCookie, int nLine, boolean bAll ) throws Throwable {
        // Informazioni sul proxy
        boolean bProxy = System.getProperty("http.proxySet","false").equalsIgnoreCase("true");
        String cProxy = "";
        int nProxyPort = 0;

        // URL remoto
        URL oUrl = new URL( cUrl );
        //URL urlObject = getNewUrl( cUrl );

        // cosa devo prendere?
        int    nRemotePort = oUrl.getPort();
        if( nRemotePort==-1 ) nRemotePort=80;
        String cRemoteHost = oUrl.getHost();
        String cFile       = oUrl.getFile();

        // Preparo la chiamata
        StringBuffer sb = new StringBuffer();
        if( bProxy ) {
           sb.append( "GET " + cUrl +" HTTP/1.1\r\n" );
        } else {
           sb.append( "GET " +cFile +" HTTP/1.1\r\n" );
        }
        sb.append( "User-Agent: " +getAgent() +"\r\n" );
        sb.append( "Pragma: no-cache\r\n" );
        sb.append( "Accept-Language: it\r\n" );
        sb.append( "Host: " +cRemoteHost +"\r\n" );
        if( cCookie!=null  ) sb.append( "Cookie: " +cCookie +"\r\n" );

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if( cEncode.length()>0 ){
           if( System.getProperty("proxyPassword", "").length()>0 )
              cEncode += ":" +System.getProperty("proxyPassword", "");
           cEncode = new String( Base64.encode( cEncode.getBytes() ) );
           sb.append( "Proxy-authorization: Basic " +cEncode +"\r\n" );
        }
        sb.append( "\r\n" );

        // Ora mi connetto e sparo la chiamata
        Socket socket = null;
        if( bProxy ) {
           cProxy = System.getProperty("http.proxyHost","");
           nProxyPort = Double.valueOf( System.getProperty("http.proxyPort","8080") ).intValue();
           socket = new Socket(cProxy, nProxyPort);
        } else {
           // Output
           socket = new Socket(cRemoteHost, nRemotePort);
        }

        // Scrivo il dato
        OutputStream os = socket.getOutputStream();
        htmlTool html = new htmlTool();
        html.putData( os, sb.toString() );

        sb = new StringBuffer();

        // Leggo il dato
        InputStream is = socket.getInputStream();

        // Leggo l'header
        String cRH = html.getHeader( is );
        sb.append( is );
        //sb.append( "\r\n" );

        cookieHeader             = html.setcookie(cRH);
        locationHeader           = html.location(cRH);
        contentDispositionHeader = html.contentDisposition(cRH);
        contentTypeHeader        = html.contentType(cRH);

        sb = new StringBuffer();
        int nRL = html.contentLength(cRH);
        if( nRL>0 ){
           sb.append( html.getData( is, nRL, bAll, nLine ) );
        } else if( html.isChunked( cRH ) ){
           ByteArrayOutputStream cRD = new ByteArrayOutputStream();
           readChunk( html, is, cRD, bAll, nLine );
           sb.append( cRD );
        } else if( is.available()>0 ){
           sb.append( html.getData( is, -1, bAll, nLine ) );
        }

        try {
            socket.close();
        } catch (Throwable e) { }


        return sb;
    }

    private void readChunk( htmlTool html, InputStream SI, OutputStream OO, boolean bAll, int nLine ) throws Throwable {

        ByteArrayOutputStream cRPD;
        int nLen = 0;
        do {
            String cLine = html.getLine( SI );
            nLen = Integer.parseInt( cLine.trim(), 16 );

            // read len + 2 = CRLF
            cRPD = html.getData( SI, nLen+2, bAll, nLine );

            // Put output
            //html.putData( OO, cLine );

            //log.info( cLine+cRPD );

            html.putData( OO, cRPD );

        } while ( nLen>0 );
    }
    //*/
    /**
     *
     * @param cUrl
     * @param cCookie
     * @param cPost
     * @return
     * @throws Exception
     */
    public StringBuffer postPage(String cUrl, String cCookie, String cPost) throws Exception {
        return postPage(cUrl, cCookie, cPost, "", "", "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param cPost
     * @param cRef
     * @return
     * @throws Exception
     */
    public StringBuffer postPage(String cUrl, String cCookie, String cPost, String cRef) throws Exception {
        return postPage(cUrl, cCookie, cPost, cRef, "", "");
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param cPost
     * @param cRef
     * @param cAuthorization
     * @param postMode
     * @return
     * @throws Exception
     */
    public StringBuffer postPage(String cUrl, String cCookie, String cPost, String cRef, String cAuthorization, String postMode) throws Exception {
        if (bDebug) {
            log.info("HTTP/POST: " + cUrl);
        }
        if (bDebug) {
            log.info("Cookie: " + cCookie);
        }

        // Preparo il post
        //log.info("POST:"+cUrl);
        URL urlObject = new URL(cUrl);
        //URL urlObject = getNewUrl( cUrl );
        //HttpURLConnection con = (HttpURLConnection)urlObject.openConnection();
        URLConnection con = (URLConnection) urlObject.openConnection();
        // 02/01/2007 11:44:50 [283384] java.lang.ClassCastException: com/ms/net/wininet/http/HttpURLConnection
        // Devo usare l'oggetto giusto senza CAST

        con.setDoOutput(true);
        con.setDoInput(true);
        try {
            //con.setRequestMethod("POST");
            // Chiedo il metodo setRequestMethod
            Method SetRequestMethod = con.getClass().getMethod("setRequestMethod", new Class[]{String.class});
            // Lo chiamo chiedendo un POST
            SetRequestMethod.invoke(con, new Object[]{"POST"});
        } catch (Exception e) {
            //log.info( con.getClass() );
            //log.error("Error",e);
            // ##Da verificare con e senza proxy
            // ##ad ora usata solo da gmail
            //log.info("POST "+urlObject.toString()+" HTTP/1.0");
            //con.setRequestProperty("POST "+urlObject.toString()+" HTTP/1.0", null );
            //con.setRequestProperty("POST /accounts/ServiceLoginAuth HTTP/1.0",null);
        }

        // Se usi WinINet (com.ms.net.wininet.http.HttpURLConnection) non ho il metodo
        if (con instanceof java.net.HttpURLConnection) {
            ((java.net.HttpURLConnection) con).setFollowRedirects(false);
        }

        setInstanceFollowRedirects(con);

        con.setUseCaches(false);

        // User agent random
        con.setRequestProperty("User-Agent", getAgent());

        // No cache
        con.setRequestProperty("Pragma", "no-cache");

        // Referer
        if (cRef.length() > 0) {
            con.setRequestProperty("Referer", cRef);
        }

        // Preparo il post in italiano
        // GMail FIX
        if (cUrl.toUpperCase().indexOf("HTTPS://") == -1) {
            con.setRequestProperty("Content-Length", "" + cPost.length());
        }
        con.setRequestProperty("Accept-Language", "it");

        if (postMode != null && postMode.equalsIgnoreCase("multipart/form-data")) {
            //getting the url posting and generating the right post
            final String LINE_SEPARATOR = String.valueOf((char) 13) + String.valueOf((char) 10);
            StringTokenizer st = new StringTokenizer(cPost, "&");
            StringBuffer s1 = new StringBuffer();
            String boundary = generateBoundary();

            while (st.hasMoreElements()) {
                String cTok = st.nextToken();

                String name = "";
                String value = "";

                int nEq = cTok.indexOf("=");

                if (nEq != -1) {
                    name = cTok.substring(0, nEq);
                    if (nEq < cTok.length()) {
                        value = cTok.substring(nEq + 1);
                    }
                }

                s1.append("--" + boundary + LINE_SEPARATOR
                        + "Content-Disposition: form-data; name=\"" + name + "\"" + LINE_SEPARATOR
                        + LINE_SEPARATOR + value + LINE_SEPARATOR);
            }
            s1.append(LINE_SEPARATOR + "--" + boundary + "--" + LINE_SEPARATOR);

            cPost = s1.toString();

            //log.info( cPost );
            con.setRequestProperty("Content-Type", postMode + "; boundary=" + boundary);
            con.setRequestProperty("Content-Length", "" + cPost.length());

        } else {
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }

        if (cCookie != null && cCookie.length() > 0) {
            con.setRequestProperty("Cookie", cCookie);
        }

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if (cEncode.length() > 0) {
            if (System.getProperty("proxyPassword", "").length() > 0) {
                cEncode += ":" + System.getProperty("proxyPassword", "");
            }
            cEncode = new String(it.baccan.html2pop3.utils.Base64.encode(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        if (cAuthorization.length() > 0) {
            //con.setRequestProperty( "Authorization", "Basic " +cEncode );
        }

        //PrintWriter out = new PrintWriter( con.getOutputStream() );
        //out.print( cPost );
        //out.flush();
        //out.close();
        OutputStream out = con.getOutputStream();
        out.write(cPost.getBytes());
        out.flush();
        out.close();

        StringBuffer sb = new StringBuffer();

        // Prima provo a leggere il dato corretto
        try {
            // Lettura risultato
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            // Prendo I cookie
            processField(con);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine).append((char) 13).append((char) 10);
            }

            in.close();

            // In caso d'errore, faccio un tentativo con l'errorStream
        } catch (Exception ex) {
            logErrorStream(con);
            throw ex;
        }
        return sb;
    }

    /**
     *
     * @param cUrl
     * @param cCookie
     * @param cPost
     * @param cRef
     * @param cAuthorization
     * @param postMode
     * @return
     * @throws Exception
     */
    public byte[] postPageBytes(String cUrl, String cCookie, String cPost, String cRef, String cAuthorization, String postMode) throws Exception {
        if (bDebug) {
            log.info("HTTP/POST: " + cUrl);
        }
        if (bDebug) {
            log.info("Cookie: " + cCookie);
        }

        // Preparo il post
        //log.info("POST:"+cUrl);
        URL urlObject = new URL(cUrl);
        //URL urlObject = getNewUrl( cUrl );
        //HttpURLConnection con = (HttpURLConnection)urlObject.openConnection();
        URLConnection con = (URLConnection) urlObject.openConnection();
        // 02/01/2007 11:44:50 [283384] java.lang.ClassCastException: com/ms/net/wininet/http/HttpURLConnection
        // Devo usare l'oggetto giusto senza CAST

        con.setDoOutput(true);
        con.setDoInput(true);
        try {
            //con.setRequestMethod("POST");
            // Chiedo il metodo setRequestMethod
            Method SetRequestMethod = con.getClass().getMethod("setRequestMethod", new Class[]{String.class});
            // Lo chiamo chiedendo un POST
            SetRequestMethod.invoke(con, new Object[]{"POST"});
        } catch (Exception e) {
            //log.info( con.getClass() );
            //log.error("Error",e);
            // ##Da verificare con e senza proxy
            // ##ad ora usata solo da gmail
            //log.info("POST "+urlObject.toString()+" HTTP/1.0");
            //con.setRequestProperty("POST "+urlObject.toString()+" HTTP/1.0", null );
            //con.setRequestProperty("POST /accounts/ServiceLoginAuth HTTP/1.0",null);
        }

        // Se usi WinINet (com.ms.net.wininet.http.HttpURLConnection) non ho il metodo
        if (con instanceof java.net.HttpURLConnection) {
            ((java.net.HttpURLConnection) con).setFollowRedirects(false);
        }

        setInstanceFollowRedirects(con);

        con.setUseCaches(false);

        // User agent random
        con.setRequestProperty("User-Agent", getAgent());

        // No cache
        con.setRequestProperty("Pragma", "no-cache");

        // Referer
        if (cRef.length() > 0) {
            con.setRequestProperty("Referer", cRef);
        }

        // Preparo il post in italiano
        // GMail FIX
        if (cUrl.toUpperCase().indexOf("HTTPS://") == -1) {
            con.setRequestProperty("Content-Length", "" + cPost.length());
        }
        con.setRequestProperty("Accept-Language", "it");

        if (postMode != null && postMode.equalsIgnoreCase("multipart/form-data")) {
            //getting the url posting and generating the right post
            final String LINE_SEPARATOR = String.valueOf((char) 13) + String.valueOf((char) 10);
            StringTokenizer st = new StringTokenizer(cPost, "&");
            StringBuffer s1 = new StringBuffer();
            String boundary = generateBoundary();

            while (st.hasMoreElements()) {
                String cTok = st.nextToken();

                String name = "";
                String value = "";

                int nEq = cTok.indexOf("=");

                if (nEq != -1) {
                    name = cTok.substring(0, nEq);
                    if (nEq < cTok.length()) {
                        value = cTok.substring(nEq + 1);
                    }
                }

                s1.append("--" + boundary + LINE_SEPARATOR
                        + "Content-Disposition: form-data; name=\"" + name + "\"" + LINE_SEPARATOR
                        + LINE_SEPARATOR + value + LINE_SEPARATOR);
            }
            s1.append(LINE_SEPARATOR + "--" + boundary + "--" + LINE_SEPARATOR);

            cPost = s1.toString();

            //log.info( cPost );
            con.setRequestProperty("Content-Type", postMode + "; boundary=" + boundary);
            con.setRequestProperty("Content-Length", "" + cPost.length());

        } else {
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }

        if (cCookie != null && cCookie.length() > 0) {
            con.setRequestProperty("Cookie", cCookie);
        }

        // Proxy user e password
        String cEncode = System.getProperty("proxyUser", "");
        if (cEncode.length() > 0) {
            if (System.getProperty("proxyPassword", "").length() > 0) {
                cEncode += ":" + System.getProperty("proxyPassword", "");
            }
            cEncode = new String(it.baccan.html2pop3.utils.Base64.encode(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        if (cAuthorization.length() > 0) {
            //con.setRequestProperty( "Authorization", "Basic " +cEncode );
        }

        //PrintWriter out = new PrintWriter( con.getOutputStream() );
        //out.print( cPost );
        //out.flush();
        //out.close();
        OutputStream out = con.getOutputStream();
        out.write(cPost.getBytes());
        out.flush();
        out.close();

        // Lettura risultato
        InputStream in = con.getInputStream();

        ByteArrayOutputStream cReply = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            cReply.write(c);
        }

        in.close();

        return cReply.toByteArray();
    }

    /**
     * @param con * @conditional (JVM14)
     */
    public void logErrorStream(URLConnection con) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(((java.net.HttpURLConnection) con).getErrorStream()));

            StringBuffer sbErr = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sbErr.append(inputLine).append((char) 13).append((char) 10);
            }

            in.close();

            // Lo stampo brutalmente, tanto mi serve solo per fare debug di situazioni d'errore
            log.error("ERROR STREAM: " + sbErr);
        } catch (Exception exx) {
            // Per ora ignoro l'errore
        }
    }

    /**
     *
     * @param sb
     */
    protected void putFile(String sb) {
        putFile(new StringBuffer(sb), "");
    }

    /**
     *
     * @param sb
     */
    protected void putFile(StringBuffer sb) {
        putFile(sb, "");
    }

    /**
     *
     * @param sb
     * @param cAdd
     */
    protected void putFile(String sb, String cAdd) {
        putFile(new StringBuffer(sb), cAdd);
    }

    /**
     *
     * @param sb
     * @param cAdd
     */
    protected void putFile(StringBuffer sb, String cAdd) {
        String cPath = "";
        try {
            FileWriter fw = new FileWriter(cPath + this.getClass().getName() + cAdd + ".txt");
            fw.write(sb.toString());
            fw.close();
        } catch (Exception ex) {
            log.error("Error", ex);
        }
    }

    /**
     *
     * @return
     */
    protected String getAgent() {
        String cAgent = "";
        // Millisecondi di sistema
        long nMill = System.currentTimeMillis();
        // Modulo 11
        nMill = nMill % 11;
        nMill = 5;

        switch ((int) nMill) {
            case 0:
                cAgent = "Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 5.1; Trident/4.0; GTB6.6; .NET CLR 1.1.4322; InfoPath.1; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
                break;
            case 1:
                cAgent = "Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
                break;
            case 2:
                cAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB6.6; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; eSobiSubscriber 2.0.4.16; .NET4.0C; BRI/2)";
                break;
            case 3:
                cAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; FunWebProducts; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
                break;
            case 4:
                cAgent = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0; BOIE9;ITIT)";
                break;
            case 5:
                cAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0";
                break;
            case 6:
                cAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; GTB6.6; InfoPath.2; .NET CLR 2.0.50727; AskTbCNB/5.9.1.14019)";
                break;
            case 7:
                cAgent = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; Tablet PC 2.0; InfoPath.2; .NET CLR 3.5.30729; .NET CLR 3.0.30618)";
                break;
            case 8:
                cAgent = "Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 5.1; Trident/4.0; GTB6.6; .NET CLR 1.1.4322; InfoPath.1; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)";
                break;
            case 9:
                cAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko) Chrome/10.0.648.204 Safari/534.16";
                break;
            case 10:
                cAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; it; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16";
                break;
        }
        return cAgent;
    }

    /**
     *
     * @param y
     * @return
     */
    protected String filterRem(String y) {
        return filter(y, "<!--", "-->");
    }

    /**
     *
     * @param y
     * @param cOn
     * @param cOff
     * @return
     */
    protected String filter(String y, String cOn, String cOff) {
        StringBuffer oBuf = new StringBuffer();

        String cUpper = y.toUpperCase();
        cOn = cOn.toUpperCase();
        cOff = cOff.toUpperCase();

        //boolean isTag = false;
        for (int nPos = 0; nPos < y.length(); nPos++) {
            while (cUpper.startsWith(cOn, nPos)) {
                int nLastPos = cUpper.indexOf(cOff, nPos + 1);
                if (nLastPos == -1) {
                    break;
                }
                nPos = nLastPos + cOff.length();
            }
            if (nPos >= 0 && nPos < y.length()) {
                oBuf.append(y.charAt(nPos));
            }
        }
        return oBuf.toString();
    }

    // Date
    /**
     *
     * @return
     */
    public String getCurDate() {
        java.util.Date d_oper = new java.util.Date();
        return formatDate(d_oper);
    }

    /**
     *
     * @param nYear
     * @param nMonth
     * @param nDay
     * @param nH
     * @param nM
     * @param nS
     * @return
     */
    public String formatDate(int nYear, int nMonth, int nDay, int nH, int nM, int nS) {
        Calendar date = Calendar.getInstance();
        date.set(nYear + 1900, nMonth, nDay, nH, nM, nS);
        return formatDate(date.getTime());
    }

    /**
     *
     * @param d_oper
     * @return
     */
    public String formatDate(java.util.Date d_oper) {
        //Date: Sat, 10 Jan 2004 1:06:23 +0100
        SimpleDateFormat oFormatDate;

        /*
       // Puo' andare, a patto che non ci sia l'ora legale, dove l'offset è +0200
       oFormatDate = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss"  , java.util.Locale.US );
       cRet = oFormatDate.format(d_oper);
       
       // TODO:
       // Non e' il modo giusto .. ma per ora facciamolo andar bene :)
       cRet += " +0100";
         */
        String cRet = "";

        // Nuovo algoritmo basato sull'offset corrente
        oFormatDate = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", java.util.Locale.US);
        cRet = oFormatDate.format(d_oper);

        // Forzo il GMT+1 = Italia
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        // Calcolo l'offset
        int value = (calendar.get(Calendar.ZONE_OFFSET)
                + calendar.get(Calendar.DST_OFFSET)) / 60000;

        int width = 4;
        String buffer = " ";
        if (value >= 0) {
            buffer += "+";
            if (value < 1000) {
                buffer += "0";
            }
        }

        int num = (value / 60) * 100 + (value % 60);
        buffer += "" + num;
        cRet += buffer;

        return cRet;
    }

    /**
     *
     * @param c
     * @return
     */
    public int month2num(String c) {
        int nNum = 0;
        if (c.equalsIgnoreCase("gen")) {
            nNum = 0;
        } else if (c.equalsIgnoreCase("feb")) {
            nNum = 1;
        } else if (c.equalsIgnoreCase("mar")) {
            nNum = 2;
        } else if (c.equalsIgnoreCase("apr")) {
            nNum = 3;
        } else if (c.equalsIgnoreCase("mag")) {
            nNum = 4;
        } else if (c.equalsIgnoreCase("giu")) {
            nNum = 5;
        } else if (c.equalsIgnoreCase("lug")) {
            nNum = 6;
        } else if (c.equalsIgnoreCase("ago")) {
            nNum = 7;
        } else if (c.equalsIgnoreCase("set")) {
            nNum = 8;
        } else if (c.equalsIgnoreCase("ott")) {
            nNum = 9;
        } else if (c.equalsIgnoreCase("nov")) {
            nNum = 10;
        } else if (c.equalsIgnoreCase("dic")) {
            nNum = 11;
        }
        return nNum;
    }

    /**
     *
     * @param c
     * @return
     */
    public int month2numEng(String c) {
        int nNum = 0;
        if (c.equalsIgnoreCase("jan")) {
            nNum = 0;
        } else if (c.equalsIgnoreCase("feb")) {
            nNum = 1;
        } else if (c.equalsIgnoreCase("mar")) {
            nNum = 2;
        } else if (c.equalsIgnoreCase("apr")) {
            nNum = 3;
        } else if (c.equalsIgnoreCase("may")) {
            nNum = 4;
        } else if (c.equalsIgnoreCase("jun")) {
            nNum = 5;
        } else if (c.equalsIgnoreCase("jul")) {
            nNum = 6;
        } else if (c.equalsIgnoreCase("aug")) {
            nNum = 7;
        } else if (c.equalsIgnoreCase("sep")) {
            nNum = 8;
        } else if (c.equalsIgnoreCase("oct")) {
            nNum = 9;
        } else if (c.equalsIgnoreCase("nov")) {
            nNum = 10;
        } else if (c.equalsIgnoreCase("dec")) {
            nNum = 11;
        }
        return nNum;
    }

    /**
     *
     * @param c
     * @return
     */
    public int month2numIta(String c) {
        int nNum = 0;
        if (c.equalsIgnoreCase("gen")) {
            nNum = 0;
        } else if (c.equalsIgnoreCase("feb")) {
            nNum = 1;
        } else if (c.equalsIgnoreCase("mar")) {
            nNum = 2;
        } else if (c.equalsIgnoreCase("apr")) {
            nNum = 3;
        } else if (c.equalsIgnoreCase("mag")) {
            nNum = 4;
        } else if (c.equalsIgnoreCase("giu")) {
            nNum = 5;
        } else if (c.equalsIgnoreCase("lug")) {
            nNum = 6;
        } else if (c.equalsIgnoreCase("ago")) {
            nNum = 7;
        } else if (c.equalsIgnoreCase("set")) {
            nNum = 8;
        } else if (c.equalsIgnoreCase("ott")) {
            nNum = 9;
        } else if (c.equalsIgnoreCase("nov")) {
            nNum = 10;
        } else if (c.equalsIgnoreCase("dic")) {
            nNum = 11;
        }
        return nNum;
    }

    /**
     *
     * @param c
     * @param cPar
     * @return
     */
    public String getPar(String c, String cPar) {
        return getPar(c, cPar, "");
    }

    /**
     *
     * @param c
     * @param cPar
     * @param cDef
     * @return
     */
    public String getPar(String c, String cPar, String cDef) {
        String cRet = cDef;

        int nPos = c.indexOf("?" + cPar + "=");
        if (nPos == -1) {
            nPos = c.indexOf("&" + cPar + "=");
        }

        if (nPos != -1) {
            int nEnd = c.indexOf("&", nPos + 1);
            if (nEnd == -1) {
                nEnd = c.length();
            }
            cRet = c.substring(nPos + 1 + cPar.length() + 1, nEnd);
        }
        return cRet;
    }

    /**
     *
     * @param s
     * @param s1
     * @param s2
     * @return
     */
    public static String replace(String s, String s1, String s2) {
        return string.replace(s, s1, s2);
    }

    /**
     * @return the generated boundary for posting in multipart/form-data
     */
    private String generateBoundary() {
        return "-----------------------------"
                + String.valueOf((int) (Math.random() * 10000))
                + String.valueOf((int) (Math.random() * 10000))
                + String.valueOf((int) (Math.random() * 10000));
    }

    //public String getPar( String cSea, String cPar ) {
    //String cRet = "";
    //int nPos            = cSea.toUpperCase().indexOf("?"+cPar.toUpperCase()+"=");
    //if( nPos==-1 ) nPos = cSea.toUpperCase().indexOf("&"+cPar.toUpperCase()+"=");
    //if( nPos!=-1 ){
    //int nLast = cSea.indexOf("&",nPos+1);
    //if( nLast==-1 ) nLast = cSea.length();
    //cRet = cSea.substring(nPos+1+cPar.length()+1,nLast);
    //}
    //return cRet;
    //}
    /**
     *
     * @param cFile
     * @return
     */
    public String readCache(String cFile) {
        String cRet = null;
        try {
            java.io.File oFile = new java.io.File(cFile);
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

    /**
     *
     * @param cFile
     * @param cData
     */
    public void writeCache(String cFile, String cData) {
        try {
            RandomAccessFile oCache = new RandomAccessFile(cFile, "rw");
            oCache.write(cData.getBytes(), 0, cData.getBytes().length);
            oCache.close();
        } catch (Throwable ex) {
            log.info("writeCache", ex);
        }
    }

    /**
     *
     * @param cCook
     * @param cSubCook
     * @return
     */
    public String getCook(String cCook, String cSubCook) {
        String cRet = "";

        int nT = cCook.lastIndexOf(cSubCook + "=");
        int nT2 = cCook.indexOf(";", nT);
        if (nT != -1 && nT2 != -1) {
            cRet = cCook.substring(nT + cSubCook.length() + 1, nT2);
        }

        return cRet;
    }

    /**
     *
     * @param cCook
     * @param cSubCook
     * @return
     */
    public String getFullCook(String cCook, String cSubCook) {
        String cRet = getCook(cCook, cSubCook);
        if (cRet.length() > 0) {
            cRet = cSubCook + "=" + cRet + "; ";
        }
        return cRet;
    }

    // Properties di sistema
    private boolean bDebug = false;

    /**
     *
     * @param p
     */
    public void setDebug(boolean p) {
        bDebug = p;
    }

    /**
     *
     * @return
     */
    public boolean getDebug() {
        return bDebug;
    }

}
