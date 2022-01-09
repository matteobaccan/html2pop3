/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
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

import it.baccan.html2pop3.utils.HTMLTool;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public abstract class PluginBase {

    @Getter
    @Setter
    private String cookie = "";
    @Getter
    @Setter
    private String location = "";
    @Getter
    @Setter
    private String contentDisposition = "";
    @Getter
    @Setter
    private String contentType = "";
    @Getter
    @Setter
    private boolean debug = false;

    /**
     * @conditional (JVM14)
     */
    private void setInstanceFollowRedirects(URLConnection con) {
        ((HttpURLConnection) con).setInstanceFollowRedirects(false);
    }

    /**
     *
     * @param con
     */
    protected void processField(URLConnection con) {
        cookie = "";
        location = "";
        contentDisposition = "";
        contentType = "";

        // I cookie possono essere N .. provo a collezionarli
        int n = 1; // n=0 has no key, and the HTTP return status in the value field
        boolean done = false;
        try {
            while (!done) {
                String headerKey = con.getHeaderFieldKey(n);
                String headerVal = con.getHeaderField(n);

                if (debug) {
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
                        cookie += headerVal + " ";
                    } else if (headerKey.equalsIgnoreCase("location")) {
                        location = headerVal;
                    } else if (headerKey.equalsIgnoreCase("Content-Disposition")) {
                        contentDisposition = headerVal;
                    } else if (headerKey.equalsIgnoreCase("Content-Type")) {
                        contentType = headerVal;
                    }
                }
                n++;
            }
        } catch (Throwable e) {
            log.error("Errore su codifica cookie " + n);
        }
        cookie = cookie.trim();
        location = location.trim();

        //if( cookieHeader.endsWith(";") ) cookieHeader = cookieHeader.substring(0,cookieHeader.length()-1);
    }

    /**
     *
     * @return
     */
    protected String getContentTypeCharset() {
        String cRet = "";
        int nPos = contentType.indexOf("charset=");
        if (nPos != -1) {
            cRet = contentType.substring(nPos + 8);
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

        if (debug) {
            log.info("HTTP/GET: " + cUrl);
        }
        if (debug) {
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
            cEncode = new String(Base64.getEncoder().encodeToString(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        if (cAuthorization.length() > 0) {
            String cEncodeA = new String(Base64.getEncoder().encodeToString(cAuthorization.getBytes()));
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
            if (!bAll && endOfHdr_1stBlankLine) {
                if (nPos == nLine) {
                    break;
                }
                nPos++;
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
        if (debug) {
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
            cEncode = new String(Base64.getEncoder().encodeToString(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        // Prendo I cookie
        processField(con);

        // Lettura risultato
        ByteArrayOutputStream cReply = new ByteArrayOutputStream();
        try (InputStream in = con.getInputStream()) {
            int c;
            while ((c = in.read()) != -1) {
                cReply.write(c);
            }
        }

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
        if (debug) {
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
            cEncode = new String(Base64.getEncoder().encodeToString(cEncode.getBytes()));
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
        HTMLTool html = new HTMLTool();

        int nPos = 0;
        while ((inputLine = in.readLine()) != null) {
            // 23:19:02 mercoledi' 04 agosto 2004
            // Stupidamente quando faccio lo stream dei messaggi
            // e la riga inizia con un . lo passo al client
            // questo comportamento interrompe la comunicazione su alcuni client
            // la procedura corretta e': se inizio la riga col punto, ne aggiungo
            // uno in testa.
            // Questra regola vale sempre non solo per le righe da 1 punto
            if (bPoint && inputLine.startsWith(".")) {
                inputLine = "." + inputLine;
            }

            html.putData(SO, inputLine + (char) 13 + (char) 10);

            if (inputLine.length() == 0) {
                endOfHdr_1stBlankLine = true;
            }
            if (!bAll && endOfHdr_1stBlankLine) {
                if (nPos == nLine) {
                    break;
                }
                nPos++;
            }

        }

        // NON va bene .. no no no .. non posso sconnettere col jdk Microsoft!! .. ma siamo fuori?
        //con.disconnect();
        // Questo invece non da errore, ma con Microsoft e' come se comunque scaricasse
        // la pagina .. mannaggia mannaggia
        con.getInputStream().close();
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
     * @param cPostParam
     * @param cRef
     * @param cAuthorization
     * @param postMode
     * @return
     * @throws Exception
     */
    public StringBuffer postPage(final String cUrl, final String cCookie, final String cPostParam, final String cRef, final String cAuthorization, final String postMode) throws Exception {
        String cPost = cPostParam;
        if (debug) {
            log.info("HTTP/POST: " + cUrl);
        }
        if (debug) {
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
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).setFollowRedirects(false);
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
            cEncode = new String(Base64.getEncoder().encodeToString(cEncode.getBytes()));
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
     * @param cPostParam
     * @param cRef
     * @param cAuthorization
     * @param postMode
     * @return
     * @throws Exception
     */
    public byte[] postPageBytes(final String cUrl, final String cCookie, final String cPostParam, final String cRef, final String cAuthorization, final String postMode) throws Exception {
        String cPost = cPostParam;
        if (debug) {
            log.info("HTTP/POST: " + cUrl);
        }
        if (debug) {
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
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).setFollowRedirects(false);
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
            StringBuilder s1 = new StringBuilder();
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

                s1.append("--").append(boundary).append(LINE_SEPARATOR).append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE_SEPARATOR).append(LINE_SEPARATOR).append(value).append(LINE_SEPARATOR);
            }
            s1.append(LINE_SEPARATOR).append("--").append(boundary).append("--").append(LINE_SEPARATOR);

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
            cEncode = new String(Base64.getEncoder().encodeToString(cEncode.getBytes()));
            con.setRequestProperty("Proxy-authorization", "Basic " + cEncode);
        }

        if (cAuthorization.length() > 0) {
            //con.setRequestProperty( "Authorization", "Basic " +cEncode );
        }

        try (OutputStream out = con.getOutputStream()) {
            out.write(cPost.getBytes());
            out.flush();
        }

        ByteArrayOutputStream cReply = new ByteArrayOutputStream();
        // Lettura risultato
        try (InputStream in = con.getInputStream()) {
            int c;
            while ((c = in.read()) != -1) {
                cReply.write(c);
            }
        }

        return cReply.toByteArray();
    }

    /**
     * @param con * @conditional (JVM14)
     */
    public void logErrorStream(URLConnection con) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(((HttpURLConnection) con).getErrorStream()));

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
            try (FileWriter fw = new FileWriter(cPath + this.getClass().getName() + cAdd + ".txt")) {
                fw.write(sb.toString());
            }
        } catch (Exception ex) {
            log.error("Error", ex);
        }
    }

    /**
     * Firefox simulation.
     *
     * @return
     */
    protected String getAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:66.0) Gecko/20100101 Firefox/66.0";
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
     * @param cOnParam
     * @param cOffParam
     * @return
     */
    protected String filter(final String y, final String cOnParam, final String cOffParam) {
        StringBuilder oBuf = new StringBuilder();

        String cUpper = y.toUpperCase();
        String cOn = cOnParam.toUpperCase();
        String cOff = cOffParam.toUpperCase();

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

        String cRet = "";

        // Nuovo algoritmo basato sull'offset corrente
        oFormatDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", java.util.Locale.US);
        cRet = oFormatDate.format(d_oper);

        // Forzo il GMT+1 = Italia
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        // Calcolo l'offset
        int value = (calendar.get(Calendar.ZONE_OFFSET)
                + calendar.get(Calendar.DST_OFFSET)) / 60000;

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
        return s.replace(s1, s2);
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
            File oFile = new File(cFile);
            if (oFile.exists()) {
                byte[] bufferSO;
                try (FileInputStream fInput = new FileInputStream(cFile)) {
                    bufferSO = new byte[fInput.available()];
                    fInput.read(bufferSO);
                }
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
            try (RandomAccessFile oCache = new RandomAccessFile(cFile, "rw")) {
                oCache.write(cData.getBytes(), 0, cData.getBytes().length);
            }
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

}
