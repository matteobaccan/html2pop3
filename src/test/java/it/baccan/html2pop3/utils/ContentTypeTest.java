/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.html2pop3.utils;

import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Matteo
 */
@Slf4j
public class ContentTypeTest {

    /**
     * Test of getInstance method, of class ContentType.
     */
    @Test
    public final void testGetInstance() {
        log.info("getInstance");
        ContentType result = ContentType.getInstance();
        assertNotEquals(null, result);
    }

    /**
     * Test of getFromExtension method, of class ContentType.
     */
    @Test
    public final void testGetFromExtension() {
        log.info("getFromExtension");
        ContentType instance = ContentType.getInstance();
        assertEquals("text/plain", instance.getFromExtension("txt"));
        assertEquals("text/html", instance.getFromExtension("html"));
    }

    /**
     * Test of getFromFilename method, of class ContentType.
     */
    @Test
    public final void testGetFromFilename() {
        log.info("getFromFilename");
        ContentType instance = ContentType.getInstance();
        assertEquals("text/html",
                instance.getFromFilename("/prova/blabla.txt/file.html"));
    }

}
