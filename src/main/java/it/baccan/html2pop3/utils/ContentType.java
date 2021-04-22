/**
 * Title:        Libero HTML2POP3
 * Description:  Calcola il content-type in base al nome del file
 * Copyright:    Copyright (c) 2004
 * Company:
 *
 * @author Stefano Picerno
 * @version 1.0
 */
package it.baccan.html2pop3.utils;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author matteo
 */
@Slf4j
public class ContentType {

    static private ContentType instance = null;

    private Map<String, String> map;
    private String defaultContentType;

    /**
     *
     * @return
     */
    static public ContentType getInstance() {
        if (instance == null) {
            instance = new ContentType();
        }
        return instance;
    }

    private ContentType() {
        addMapping("jpg", "image/jpeg");
        addMapping("jpeg", "image/jpeg");
        addMapping("aif", "audio/x-aiff");
        addMapping("aifc", "audio/x-aiff");
        addMapping("aiff", "audio/x-aiff");
        addMapping("asc", "text/plain");
        addMapping("au", "audio/basic");
        addMapping("avi", "video/x-msvideo");
        addMapping("c", "text/plain");
        addMapping("cc", "text/plain");
        addMapping("cdf", "application/x-netcdf");
        addMapping("gif", "image/gif");
        addMapping("gtar", "application/x-gtar");
        addMapping("gz", "application/x-gzip");
        addMapping("h", "text/plain");
        addMapping("hdf", "application/x-hdf");
        addMapping("hh", "text/plain");
        addMapping("hqx", "application/mac-binhex40");
        addMapping("htm", "text/html");
        addMapping("html", "text/html");
        addMapping("ice", "x-conference/x-cooltalk");
        addMapping("ief", "image/ief");
        addMapping("iges", "model/iges");
        addMapping("igs", "model/iges");
        addMapping("ips", "application/x-ipscript");
        addMapping("ipx", "application/x-ipix");
        addMapping("jpe", "image/jpeg");
        addMapping("jpeg", "image/jpeg");
        addMapping("jpg", "image/jpeg");
        addMapping("js", "application/x-javascript");
        addMapping("kar", "audio/midi");
        addMapping("latex", "application/x-latex");
        addMapping("lha", "application/octet-stream");
        addMapping("lsp", "application/x-lisp");
        addMapping("lzh", "application/octet-stream");
        addMapping("m", "text/plain");
        addMapping("man", "application/x-troff-man");
        addMapping("me", "application/x-troff-me");
        addMapping("mesh", "model/mesh");
        addMapping("mid", "audio/midi");
        addMapping("midi", "audio/midi");
        addMapping("mif", "application/vnd.mif");
        addMapping("mime", "www/mime");
        addMapping("mov", "video/quicktime");
        addMapping("movie", "video/x-sgi-movie");
        addMapping("mp2", "audio/mpeg");
        addMapping("mp3", "audio/mpeg");
        addMapping("mpe", "video/mpeg");
        addMapping("mpeg", "video/mpeg");
        addMapping("mpg", "video/mpeg");
        addMapping("mpga", "audio/mpeg");
        addMapping("ms", "application/x-troff-ms");
        addMapping("msh", "model/mesh");
        addMapping("nc", "application/x-netcdf");
        addMapping("oda", "application/oda");
        addMapping("pbm", "image/x-portable-bitmap");
        addMapping("pdb", "chemical/x-pdb");
        addMapping("pdf", "application/pdf");
        addMapping("pgm", "image/x-portable-graymap");
        addMapping("pgn", "application/x-chess-pgn");
        addMapping("png", "image/png");
        addMapping("pnm", "image/x-portable-anymap");
        addMapping("pot", "application/mspowerpoint");
        addMapping("ppm", "image/x-portable-pixmap");
        addMapping("pps", "application/mspowerpoint");
        addMapping("ppt", "application/mspowerpoint");
        addMapping("ppz", "application/mspowerpoint");
        addMapping("pre", "application/x-freelance");
        addMapping("prt", "application/pro_eng");
        addMapping("ps", "application/postscript");
        addMapping("qt", "video/quicktime");
        addMapping("ra", "audio/x-realaudio");
        addMapping("ram", "audio/x-pn-realaudio");
        addMapping("ras", "image/cmu-raster");
        addMapping("rgb", "image/x-rgb");
        addMapping("rm", "audio/x-pn-realaudio");
        addMapping("roff", "application/x-troff");
        addMapping("rpm", "audio/x-pn-realaudio-plugin");
        addMapping("rtf", "text/rtf");
        addMapping("rtx", "text/richtext");
        addMapping("scm", "application/x-lotusscreencam");
        addMapping("set", "application/set");
        addMapping("sgm", "text/sgml");
        addMapping("sgml", "text/sgml");
        addMapping("sh", "application/x-sh");
        addMapping("shar", "application/x-shar");
        addMapping("silo", "model/mesh");
        addMapping("sit", "application/x-stuffit");
        addMapping("skd", "application/x-koan");
        addMapping("skm", "application/x-koan");
        addMapping("skp", "application/x-koan");
        addMapping("skt", "application/x-koan");
        addMapping("smi", "application/smil");
        addMapping("smil", "application/smil");
        addMapping("snd", "audio/basic");
        addMapping("sol", "application/solids");
        addMapping("spl", "application/x-futuresplash");
        addMapping("src", "application/x-wais-source");
        addMapping("step", "application/STEP");
        addMapping("stl", "application/SLA");
        addMapping("stp", "application/STEP");
        addMapping("sv4cpio", "application/x-sv4cpio");
        addMapping("sv4crc", "application/x-sv4crc");
        addMapping("swf", "application/x-shockwave-flash");
        addMapping("t", "application/x-troff");
        addMapping("tar", "application/x-tar");
        addMapping("tcl", "application/x-tcl");
        addMapping("tex", "application/x-tex");
        addMapping("texi", "application/x-texinfo");
        addMapping("texinfo", "application/x-texinfo");
        addMapping("tif", "image/tiff");
        addMapping("tiff", "image/tiff");
        addMapping("tr", "application/x-troff");
        addMapping("tsi", "audio/TSP-audio");
        addMapping("tsp", "application/dsptype");
        addMapping("tsv", "text/tab-separated-values");
        addMapping("txt", "text/plain");
        addMapping("unv", "application/i-deas");
        addMapping("ustar", "application/x-ustar");
        addMapping("vcd", "application/x-cdlink");
        addMapping("vda", "application/vda");
        addMapping("viv", "video/vnd.vivo");
        addMapping("vivo", "video/vnd.vivo");
        addMapping("vrml", "model/vrml");
        addMapping("wav", "audio/x-wav");
        addMapping("wrl", "model/vrml");
        addMapping("xbm", "image/x-xbitmap");
        addMapping("xlc", "application/vnd.ms-excel");
        addMapping("xll", "application/vnd.ms-excel");
        addMapping("xlm", "application/vnd.ms-excel");
        addMapping("xls", "application/vnd.ms-excel");
        addMapping("xlw", "application/vnd.ms-excel");
        addMapping("xml", "text/xml");
        addMapping("xpm", "image/x-xpixmap");
        addMapping("xwd", "image/x-xwindowdump");
        addMapping("xyz", "chemical/x-pdb");
        addMapping("zip", "application/zip");
        setDefault("application/octet-stream");
    }

    private void setDefault(String type) {
        defaultContentType = type;
    }

    private void addMapping(String ext, String type) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(ext, type);
    }

    /**
     *
     * @param ext
     * @return
     */
    public String getFromExtension(String ext) {
        Object value = map.get(ext);
        if (value == null) {
            return defaultContentType;
        }
        return (String) value;
    }

    /**
     *
     * @param filename
     * @return
     */
    public String getFromFilename(String filename) {
        int dotPos = filename.lastIndexOf('.');

        if (dotPos < 0) {
            return defaultContentType;
        }

        return getFromExtension(filename.substring(dotPos + 1));
    }

}
