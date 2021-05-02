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
import org.junit.jupiter.api.Test;

/**
 *
 * @author Matteo
 */
@Slf4j
public class FilterTest {

    /**
     * Test of add method, of class Filter.
     */
    @Test
    public void testAdd() {
        log.info("add");
        Filter f = new Filter();
        assertEquals(true, f.add("allow", new String[]{"all", "192.168", "pippo"}));
        assertEquals(true, f.add("allow", new String[]{"pluto"}));
        assertEquals(true, f.add("deny", new String[]{"all"}));

    }

    /**
     * Test of isAllow method, of class Filter.
     */
    @Test
    public void testIsAllow() {
        log.info("isAllow");

        Filter f = new Filter();
        assertEquals(true, f.add("allow", new String[]{"all", "192.168", "pippo"}));
        assertEquals(true, f.add("allow", new String[]{"pluto"}));
        assertEquals(true, f.add("deny", new String[]{"all"}));

        assertEquals(false, f.isAllow(new String[]{"pippo", "127.0.0.1"}));
        assertEquals(true, f.isAllow(new String[]{"pippo", "192.168.0.1"}));
        assertEquals(true, f.isAllow(new String[]{"pluto", "192.168.0.1"}));
        assertEquals(false, f.isAllow(new String[]{"xxxxx", "192.168.0.1", ""}));
        assertEquals(false, f.isAllow(new String[]{"xxxxx", "192.168.0.1", "x"}));
        assertEquals(false, f.isAllow(new String[]{"xxxxx", "127.0.0.1"}));
    }

}
