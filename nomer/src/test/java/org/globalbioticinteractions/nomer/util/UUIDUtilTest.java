package org.globalbioticinteractions.nomer.util;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UUIDUtilTest {

    @Test
    public void validUUID() {
        assertTrue(UUIDUtil.isaUUID(UUID.randomUUID().toString()));
    }

    @Test
    public void invalidUUID() {
        assertFalse(UUIDUtil.isaUUID("donald duck"));
    }

}