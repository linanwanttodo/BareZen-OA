package com.linan.core.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DsContextHolder 单元测试
 */
class DsContextHolderTest {

    @Test
    void testDefaultIsMaster() {
        assertEquals("master", DsContextHolder.get());
    }

    @Test
    void testSetAndGet() {
        DsContextHolder.set("slave");
        assertEquals("slave", DsContextHolder.get());
        DsContextHolder.clear();
    }

    @Test
    void testClearRestoresDefault() {
        DsContextHolder.set("slave");
        DsContextHolder.clear();
        assertEquals("master", DsContextHolder.get());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        DsContextHolder.set("master");
        Thread t = new Thread(() -> {
            DsContextHolder.set("slave");
            assertEquals("slave", DsContextHolder.get());
            DsContextHolder.clear();
        });
        t.start();
        t.join();
        assertEquals("master", DsContextHolder.get());
    }
}
