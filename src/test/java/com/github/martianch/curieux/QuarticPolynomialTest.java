package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class QuarticPolynomialTest {

    @Test
    public void ofTest() {
        var q = QuarticPolynomial.of(10, 20, 30, 40, 50);
        assertOf(q, 10, 20, 30, 40, 50);
    }

    @Test
    public void fromParamStringTest() {
        var q = QuarticPolynomial.of(10, 20, 30, 40, 50);
        String params = q.parameterString();
        var p = QuarticPolynomial.fromParamString(params);
        assertOf(q, 10, 20, 30, 40, 50);
        assertOf((QuarticPolynomial) p.get(), 10, 20, 30, 40, 50);
    }

    @Test
    public void fromParameterStringTest() {
        var q = QuarticPolynomial.of(10, 20, 30, 40, 50);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertOf(q, 10, 20, 30, 40, 50);
        assertOf((QuarticPolynomial) p.get(), 10, 20, 30, 40, 50);
    }

    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, QuarticPolynomial.of(0, 0, 0, 0, 1).apply(3), 0);
        assertEquals(3, QuarticPolynomial.of(0, 0, 0, 1, 0).apply(3), 0);
        assertEquals(9, QuarticPolynomial.of(0, 0, 1, 0, 0).apply(3), 0);
        assertEquals(27, QuarticPolynomial.of(0, 1, 0, 0, 0).apply(3), 0);
        assertEquals(81, QuarticPolynomial.of(1, 0, 0, 0, 0).apply(3), 0);
        assertEquals(101, QuarticPolynomial.of(1, 1, -1, 1, -1).apply(3), 0);
    }

    @Test
    public void xapplyTest() {
        // int math should be ok with delta=0
        assertEquals(3, QuarticPolynomial.of(0, 0, 0, 0, 1).xapply(3), 0);
        assertEquals(9, QuarticPolynomial.of(0, 0, 0, 1, 0).xapply(3), 0);
        assertEquals(27, QuarticPolynomial.of(0, 0, 1, 0, 0).xapply(3), 0);
        assertEquals(81, QuarticPolynomial.of(0, 1, 0, 0, 0).xapply(3), 0);
        assertEquals(243, QuarticPolynomial.of(1, 0, 0, 0, 0).xapply(3), 0);
        assertEquals(303, QuarticPolynomial.of(1, 1, -1, 1, -1).xapply(3), 0);
    }

    @Test
    public void asFunctionTest() {
        assertEquals(1, QuarticPolynomial.of(0, 0, 0, 0, 1).asFunction().applyAsDouble(3), 0);
        assertEquals(3, QuarticPolynomial.of(0, 0, 0, 1, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(9, QuarticPolynomial.of(0, 0, 1, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(27, QuarticPolynomial.of(0, 1, 0, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(81, QuarticPolynomial.of(1, 0, 0, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(101, QuarticPolynomial.of(1, 1, -1, 1, -1).asFunction().applyAsDouble(3), 0);
    }

    @Test
    public void asFunctionXTest() {
        assertEquals(3, QuarticPolynomial.of(0, 0, 0, 0, 1).asFunctionX().applyAsDouble(3), 0);
        assertEquals(9, QuarticPolynomial.of(0, 0, 0, 1, 0).asFunctionX().applyAsDouble(3), 0);
        assertEquals(27, QuarticPolynomial.of(0, 0, 1, 0, 0).asFunctionX().applyAsDouble(3), 0);
        assertEquals(81, QuarticPolynomial.of(0, 1, 0, 0, 0).asFunctionX().applyAsDouble(3), 0);
        assertEquals(243, QuarticPolynomial.of(1, 0, 0, 0, 0).asFunctionX().applyAsDouble(3), 0);
        assertEquals(303, QuarticPolynomial.of(1, 1, -1, 1, -1).asFunctionX().applyAsDouble(3), 0);
    }

    @Test
    public void asStringTest() {
        assertEquals(
                "0.5000*x^4 + 0.06000*x^3 + 0.2000*x^2 + -0.3000*x + 0.4000",
                QuarticPolynomial.of(.5, .06, .2, -.3, .4).asString()
        );

    }

    @Test
    public void parameterStringTest() {
        assertEquals("P4 0.5 0.06 0.2 -0.3 0.4", QuarticPolynomial.of(.5, .06, .2, -.3, .4).parameterString());
    }

    @Test
    public void mulTest() {
        var p = QuarticPolynomial.of(2, 3, 4, 5, 6).mul(-5);
        assertOf(p, -10, -15, -20, -25, -30);
    }

    @Test
    public void subTest() {
        var p = QuarticPolynomial.of(2, 3, 4, 5, 6).sub(-5);
        assertOf(p, 2, 3, 4, 5, 11);
    }

    @Test
    public void addTest() {
        var p = QuarticPolynomial.of(2, 3, 4, 5, 6).add(5);
        assertOf(p, 2, 3, 4, 5, 11);
    }

    @Test
    public void derivativeTest() {
        QuarticPolynomial q = QuarticPolynomial.of(2, 30, 4, 5, 6);
        CubicPolynomial p = q.derivative();
        assertEquals(8, p.a, 0);
        assertEquals(90, p.b, 0);
        assertEquals(8, p.c, 0);
        assertEquals(5, p.d, 0);
        assertOf(q, 2, 30, 4, 5, 6);
    }

    @Test
    public void maxInRangeTest() {
        // (x-1)(x-2)(x-3)(x-4); min: (1.382, -1) (3.618, -1) max: (2.5, 0.5625)
        var p = QuarticPolynomial.of(1, -10, 35, -50, 24);
        assertEquals(0.5625, p.maxInRange(2, 3), 0.01);
        assertEquals(1, p.maxInRange(2, 4.133), 0.01);
    }

    @Test
    public void minInRangeTest() {
        // (x-1)(x-2)(x-3)(x-4); min: (1.382, -1) (3.618, -1) max: (2.5, 0.5625)
        var p = QuarticPolynomial.of(1, -10, 35, -50, 24);
        assertEquals(-1, p.minInRange(-10, 10), 0.01);
        assertEquals(-0.8, p.minInRange(0, 1.2), 0.01);
    }

    @Test
    public void findRootsTest() {
        // (x-1)(x-2)(x-3)(x-4); min: (1.382, -1) (3.618, -1) max: (2.5, 0.5625)
        var p = QuarticPolynomial.of(1, -10, 35, -50, 24);
        assertArrayEquals(new double[]{1, 2, 3, 4}, p.findRoots(), 0.01);
    }

    @Test
    public void findRootsInTest() {
        // (x-1)(x-2)(x-3)(x-4); min: (1.382, -1) (3.618, -1) max: (2.5, 0.5625)
        var p = QuarticPolynomial.of(1, -10, 35, -50, 24);
        assertArrayEquals(new double[]{1, 2, 3, 4}, p.findRootsIn(-10, 10), 0.01);
        assertArrayEquals(new double[]{1}, p.findRootsIn(0, 1.5), 0.01);
        assertArrayEquals(new double[]{2}, p.findRootsIn(1.5, 2.5), 0.01);
        assertArrayEquals(new double[]{1, 2}, p.findRootsIn(0, 2.5), 0.01);
        assertArrayEquals(new double[]{3, 4}, p.findRootsIn(2.9, 10), 0.01);
        assertArrayEquals(new double[]{4}, p.findRootsIn(3.1, 10), 0.01);
    }

    @Test
    public void findEqualInTest() {
        // (x-1)(x-2)(x-3)(x-4); min: (1.382, -1) (3.618, -1) max: (2.5, 0.5625)
        var p = QuarticPolynomial.of(1, -10, 35, -50, 24);
        assertArrayEquals(new double[]{1.10, 1.76, 3.24, 3.9}, p.findEqualIn(-0.5, -10, 10), 0.01);
        //assertArrayEquals(new double[]{1.382,3.618}, p.findEqualIn(-1, -10, 10), 0.01);
        assertArrayEquals(new double[]{1.2, 1.6, 3.4, 3.8}, p.findEqualIn(-0.8, -10, 10), 0.01);
    }

    @Test
    public void toStringTest() {
        assertEquals(
            "QuarticPolynomial{a=1.0, b=-10.0, c=35.0, d=-50.0, e=24.0, 1.000*x^4 + -10.00*x^3 + 35.00*x^2 + -50.00*x + 24.00}"
            , QuarticPolynomial.of(1, -10, 35, -50, 24).toString()
        );

    }

    void assertOf(QuarticPolynomial p, double a, double b, double c, double d, double e) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
        assertEquals(d, p.d, 0);
        assertEquals(e, p.e, 0);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(QuarticPolynomial.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}