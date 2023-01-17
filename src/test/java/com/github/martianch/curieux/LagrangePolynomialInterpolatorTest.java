package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;

import static com.github.martianch.curieux.LagrangePolynomialInterpolator.applyPolynomial;
import static com.github.martianch.curieux.LagrangePolynomialInterpolator.findPolynomialWith0At0;
import static org.junit.Assert.*;

public class LagrangePolynomialInterpolatorTest {
    @Test
    public void calculateTest() {
        assertEquals(27., applyPolynomial(new double[]{0,0,0,1}, 3),0);
        assertEquals(9., applyPolynomial(new double[]{0,0,1,0}, 3), 0);
        assertEquals(3., applyPolynomial(new double[]{0,1,0,0}, 3), 0);
        assertEquals(1., applyPolynomial(new double[]{1,0,0,0}, 3), 0);
        assertEquals(40., applyPolynomial(new double[]{1,1,1,1}, 3), 0);
    }
    @Test
    public void interpolateTest() {
        double[] ks = new double[] {0, 2, 3, 4};
        double[] x = new double[] {10, 140, 280};
        double[] y = Arrays.stream(x).map(xx -> applyPolynomial(ks, xx)).toArray();
        assertArrayEquals(ks, findPolynomialWith0At0(x[0],y[0],x[1],y[1],x[2],y[2]), 1e-9);
        assertArrayEquals(ks, findPolynomialWith0At0(x[1],y[1],x[2],y[2],x[0],y[0]), 1e-9);
        assertArrayEquals(ks, findPolynomialWith0At0(x[2],y[2],x[1],y[1],x[0],y[0]), 1e-9);
    }
}

