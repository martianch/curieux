package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class QuadraticPolynomialTest {
    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, QuadraticPolynomial.of(0, 0, 1).apply(3), 0);
        assertEquals(3, QuadraticPolynomial.of(0, 1, 0).apply(3), 0);
        assertEquals(9, QuadraticPolynomial.of(1, 0, 0).apply(3), 0);
        assertEquals(7, QuadraticPolynomial.of(1, -1, 1).apply(3), 0);
    }

    @Test
    public void asFunctionTest() {
        // int math should be ok with delta=0
        assertEquals(1, QuadraticPolynomial.of(0, 0, 1).asFunction().apply(3), 0);
        assertEquals(3, QuadraticPolynomial.of(0, 1, 0).asFunction().apply(3), 0);
        assertEquals(9, QuadraticPolynomial.of(1, 0, 0).asFunction().apply(3), 0);
        assertEquals(7, QuadraticPolynomial.of(1, -1, 1).asFunction().apply(3), 0);
        {
            var f = QuadraticPolynomial.of(1, 2, 3).asFunction();
            assertEquals(3, f.apply(0), 0);
            assertEquals(6, f.apply(1), 0);
            assertEquals(11, f.apply(2), 0);
        }
    }

    @Test
    public void xOfExtremumTest() {
        assertEquals(2, QuadraticPolynomial.of(1, -4, 5).xOfExtremum(), 0);
        assertEquals(3, QuadraticPolynomial.of(-1, 6, 7).xOfExtremum(), 0);
    }

    @Test
    public void maxInRangeTest() {
        var p = QuadraticPolynomial.of(-1, 6, -7);
        var f = p.asFunction();
        {
            assertEquals(-2, f.apply(1), 0);
            assertEquals(1, f.apply(2), 0);
            assertEquals(2, f.apply(3), 0);
            assertEquals(1, f.apply(4), 0);
            assertEquals(-2, f.apply(5), 0);
        }
        assertEquals(-2, p.maxInRange(0, 1), 0);
        assertEquals(1, p.maxInRange(0, 2), 0);
        assertEquals(2, p.maxInRange(0, 3), 0);
        assertEquals(2, p.maxInRange(0, 4), 0);
        assertEquals(2, p.maxInRange(3, 4), 0);
        assertEquals(1, p.maxInRange(4, 5), 0);
    }
    @Test
    public void minInRangeTest() {
        var p = QuadraticPolynomial.of(1, -6, 7);
        var f = p.asFunction();
        {
            assertEquals(2, f.apply(1), 0);
            assertEquals(-1, f.apply(2), 0);
            assertEquals(-2, f.apply(3), 0);
            assertEquals(-1, f.apply(4), 0);
            assertEquals(2, f.apply(5), 0);
        }
        assertEquals( 2, p.minInRange(0, 1), 0);
        assertEquals(-1, p.minInRange(0, 2), 0);
        assertEquals(-2, p.minInRange(0, 3), 0);
        assertEquals(-2, p.minInRange(0, 4), 0);
        assertEquals(-2, p.minInRange(3, 4), 0);
        assertEquals(-1, p.minInRange(4, 5), 0);
    }
    @Test
    public void isBetweenTest() {
        assertTrue(QuadraticPolynomial.isBetween(1, 2, 3));
        assertTrue(QuadraticPolynomial.isBetween(3, 2, 1));
        assertTrue(QuadraticPolynomial.isBetween(-1, -2, -3));
        assertTrue(QuadraticPolynomial.isBetween(-3, -2, -1));
        assertFalse(QuadraticPolynomial.isBetween(-1, 2, -3));
        assertFalse(QuadraticPolynomial.isBetween(-3, 2, -1));
        assertFalse(QuadraticPolynomial.isBetween(-1, -4, -3));
        assertFalse(QuadraticPolynomial.isBetween(-3, -4, -1));
        assertFalse(QuadraticPolynomial.isBetween(1, -2, 3));
        assertFalse(QuadraticPolynomial.isBetween(3, -2, 1));
        assertFalse(QuadraticPolynomial.isBetween(1, 4, 3));
        assertFalse(QuadraticPolynomial.isBetween(3, 4, 1));
        assertFalse(QuadraticPolynomial.isBetween(1, 4/0., 3));
        assertEquals(Double.POSITIVE_INFINITY, 4/0., 0); // FYI
        assertFalse(QuadraticPolynomial.isBetween(1, .0/0., 3));
        assertEquals(Double.NaN, .0/0., 0); // FYI
        assertNotEquals(Double.NaN, 1, 0); // FYI
        assertNotEquals(Double.NaN, Double.POSITIVE_INFINITY, 0); // FYI
        assertFalse(QuadraticPolynomial.isBetween(1, Math.sqrt(-4), 3));
        assertEquals(Double.NaN, Math.sqrt(-4)+1, 0); // FYI
        assertFalse(QuadraticPolynomial.isBetween(1, Double.NaN, 3));
        assertFalse(QuadraticPolynomial.isBetween(1, Double.NEGATIVE_INFINITY, 3));
        assertFalse(QuadraticPolynomial.isBetween(1, Double.POSITIVE_INFINITY, 3));
    }

    @Test
    public void from3PointsTest() {
        var p = QuadraticPolynomial.of(4, 3, 2);
        double[] x = new double[] {10, 140, 280};
        double[] y = Arrays.stream(x).map(p::apply).toArray();
        {
            var q = QuadraticPolynomial.from3Points(x[0],y[0], x[1],y[1], x[2],y[2]);
            assertEquals(p.a, q.a, 1e-9);
            assertEquals(p.b, q.b, 1e-9);
            assertEquals(p.c, q.c, 1e-9);
        }
        {
            var q = QuadraticPolynomial.from3Points(x[1],y[1], x[2],y[2], x[0],y[0]);
            assertEquals(p.a, q.a, 1e-9);
            assertEquals(p.b, q.b, 1e-9);
            assertEquals(p.c, q.c, 1e-9);
        }
        {
            var q = QuadraticPolynomial.from3Points(x[2],y[2], x[1],y[1], x[0],y[0]);
            assertEquals(p.a, q.a, 1e-9);
            assertEquals(p.b, q.b, 1e-9);
            assertEquals(p.c, q.c, 1e-9);
        }
        {
            var q = QuadraticPolynomial.from3Points(x[0],2*y[0], x[1],2*y[1], x[2],2*y[2]);
            assertEquals(2*p.a, q.a, 1e-9);
            assertEquals(2*p.b, q.b, 1e-9);
            assertEquals(2*p.c, q.c, 1e-9);
        }
    }
    @Test
    public void asStringTest() {
        assertEquals("0.2000*x^2 + -0.3000*x + 0.4000", QuadraticPolynomial.of(.2,-.3,.4).asString());
        assertEquals("2.000*x^2 + -3.000*x + 4.000", QuadraticPolynomial.of(2,-3,4).asString());
        assertEquals("2.000e+05*x^2 + 3.000e-05*x + -4.000e-05", QuadraticPolynomial.of(2e5,3e-5,-4e-5).asString());
        assertEquals("NaN*x^2 + NaN*x + NaN", QuadraticPolynomial.of(Double.NaN,Double.NaN,Double.NaN).asString());
        assertEquals("0.3333*x^2 + 0.1429*x + 0.2500", QuadraticPolynomial.of(1./3,1./7,1./4).asString());
    }
    @Test
    public void mulTest() {
        var p = QuadraticPolynomial.of(1, 2, 3).mul(-3);
        assertEquals(-3, p.a, 0);
        assertEquals(-6, p.b, 0);
        assertEquals(-9, p.c, 0);
    }
}