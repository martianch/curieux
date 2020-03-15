package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZoomTest {

    double eps = 0.0000001;

    @Test
    public void x2zoomTest() {
        assertEquals(4.,ZoomFactorWrapper.x2zoom(4),eps);
        assertEquals(3.,ZoomFactorWrapper.x2zoom(3),eps);
        assertEquals(2.,ZoomFactorWrapper.x2zoom(2),eps);
        assertEquals(1.,ZoomFactorWrapper.x2zoom(1),eps);
        assertEquals(.5,ZoomFactorWrapper.x2zoom(0),eps);
        assertEquals(.25,ZoomFactorWrapper.x2zoom(-1),eps);
        assertEquals(.125,ZoomFactorWrapper.x2zoom(-2),eps);
        assertEquals(.0625,ZoomFactorWrapper.x2zoom(-3),eps);

        assertEquals(.75,ZoomFactorWrapper.x2zoom(.5),eps);
        assertEquals(.625,ZoomFactorWrapper.x2zoom(.25),eps);
        assertEquals(.375,ZoomFactorWrapper.x2zoom(-.5),eps);
        assertEquals(.15625,ZoomFactorWrapper.x2zoom(-1.75),eps);
    }

    @Test
    public void zoom2xTest() {
        assertEquals(4.,ZoomFactorWrapper.zoom2x(4.),eps);
        assertEquals(3.,ZoomFactorWrapper.zoom2x(3.),eps);
        assertEquals(2.,ZoomFactorWrapper.zoom2x(2.),eps);
        assertEquals(1.,ZoomFactorWrapper.zoom2x(1.),eps);
        assertEquals(0,ZoomFactorWrapper.zoom2x(.5),eps);
        assertEquals(-1,ZoomFactorWrapper.zoom2x(.25),eps);
        assertEquals(-2,ZoomFactorWrapper.zoom2x(.125),eps);
        assertEquals(-3,ZoomFactorWrapper.zoom2x(.0625),eps);

        assertEquals(.5,ZoomFactorWrapper.zoom2x(.75),eps);
        assertEquals(.25,ZoomFactorWrapper.zoom2x(.625),eps);
        assertEquals(-.5,ZoomFactorWrapper.zoom2x(.375),eps);
        assertEquals(-1.75,ZoomFactorWrapper.zoom2x(.15625),eps);
    }
}