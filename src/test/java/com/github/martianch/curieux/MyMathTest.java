package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import static com.github.martianch.curieux.MyMath.frac;
import static com.github.martianch.curieux.MyMath.saturate;
import static com.github.martianch.curieux.MyMath.saturate01;
import static com.github.martianch.curieux.MyMath.unfrac;
import static org.junit.jupiter.api.Assertions.*;

class MyMathTest {

    @Test
    void fracTest() {
        assertEquals(0., frac(1.));
        assertEquals(0., frac(0.));
        assertEquals(0., frac(3.));
        assertEquals(0.001, frac(1.001), 1e-6);
        assertEquals(0.999, frac(-1.001), 1e-6);
    }

    @Test
    void unfracTest() {
        assertEquals(0.25, unfrac(0., 0.25));
        assertEquals(0.75, unfrac(0., -0.25));
        assertEquals(1.25, unfrac(0.5, 0.25));
    }

    @Test
    void saturateTest() {
        assertEquals(3., saturate(3., 6., 1.));
        assertEquals(3., saturate(3., 6., -1.));
        assertEquals(3., saturate(3., 6., Float.NEGATIVE_INFINITY));
        assertEquals(3., saturate(3., 6., Double.NEGATIVE_INFINITY));
        assertEquals(3.125, saturate(3., 6., 3.125));
        assertEquals(6., saturate(3., 6., 10.));
        assertEquals(6., saturate(3., 6., 6.0001));
        assertEquals(5.999, saturate(3., 6., 5.999));
        assertEquals(6., saturate(3., 6., Float.POSITIVE_INFINITY));
        assertEquals(6., saturate(3., 6., Double.POSITIVE_INFINITY));
    }

    @Test
    void saturate01Test() {
        assertEquals(0., saturate01(-0.01));
        assertEquals(0., saturate01(-0.));
        assertEquals(0., saturate01(Float.NEGATIVE_INFINITY));
        assertEquals(0., saturate01(Double.NEGATIVE_INFINITY));
        assertEquals(1., saturate01(Float.POSITIVE_INFINITY));
        assertEquals(1., saturate01(Double.POSITIVE_INFINITY));
        assertEquals(0.125, saturate01(0.125));
        assertEquals(1., saturate01(1.));
        assertEquals(1., saturate01(1.125));
    }
}