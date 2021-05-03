package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class RemoteFileNavigatorV2Test {

    @Test
    public void getSolPrefixTest() {
        assertEquals("Sol-00071", RemoteFileNavigatorV2.getSolPrefix("Sol-00071M06:55:21.349^WSM_0071_0673222759_000ECM_N0032208MEDA03345_0000LUJ"));
    }
}