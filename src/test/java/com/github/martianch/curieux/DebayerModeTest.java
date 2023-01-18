package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class DebayerModeTest {

    @Test
    public void effectNameTest() {
        assertEquals("as is", DebayerMode.NEVER.effectName());
        assertEquals("cubic/bicubic Bayer pattern demosaicing", DebayerMode.AUTO5.effectName());
    }

    @Test
    public void effectShortNameTest() {
        assertEquals("", DebayerMode.NEVER.effectShortName());
        assertEquals("dm5", DebayerMode.AUTO5.effectShortName());
    }

    @Test
    public void notNothingForTest() {
        assertFalse(DebayerMode.NEVER.notNothingFor("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00676/ids/edr/browse/zcam/ZL0_0676_0726953206_785ECM_N0320774ZCAM03518_1100LMJ01.png"));
        assertTrue(DebayerMode.AUTO5.notNothingFor("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00676/ids/edr/browse/zcam/ZL0_0676_0726953206_785ECM_N0320774ZCAM03518_1100LMJ01.png"));
        assertFalse(DebayerMode.AUTO5.notNothingFor("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00676/ids/edr/browse/zcam/ZR0_0676_0726953200_738EBY_N0320774ZCAM03518_1100LMJ01.png"));
        // diff: --------------------------------------------------------------------------------------------------------------------------------------------------------^^^
        assertFalse(DebayerMode.NEVER.notNothingFor("ZL0_0676_0726953206_785ECM_N0320774ZCAM03518_1100LMJ01.png"));
        assertTrue(DebayerMode.AUTO5.notNothingFor("ZL0_0676_0726953206_785ECM_N0320774ZCAM03518_1100LMJ01.png"));
        assertFalse(DebayerMode.AUTO5.notNothingFor("ZR0_0676_0726953200_738EBY_N0320774ZCAM03518_1100LMJ01.png"));
        // diff: ----------------------------------------------------------------^^^
    }

    @Test
    public void notNothingTest() {
        assertFalse(DebayerMode.NEVER.notNothing());
        assertTrue(DebayerMode.AUTO0.notNothing());
    }
}
