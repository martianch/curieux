package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class CubicPolynomialTest {

    @Test
    public void ofTest() {
        var q = CubicPolynomial.of(10, 20, 30, 40);
        assertOf(q, 10, 20, 30, 40);
    }

    @Test
    public void fromParamStringTest() {
        var q = CubicPolynomial.of(10, 20, 30, 40);
        String params = q.parameterString();
        var p = CubicPolynomial.fromParamString(params);
        assertOf(q, 10, 20, 30, 40);
        assertOf((CubicPolynomial)p.get(), 10, 20, 30, 40);
    }

    @Test
    public void fromParameterStringTest() {
        var q = CubicPolynomial.of(10, 20, 30, 40);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertOf(q, 10, 20, 30, 40);
        assertOf((CubicPolynomial)p.get(), 10, 20, 30, 40);
    }

    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, CubicPolynomial.of(0, 0, 0, 1).apply(3), 0);
        assertEquals(3, CubicPolynomial.of(0, 0, 1, 0).apply(3), 0);
        assertEquals(9, CubicPolynomial.of(0, 1, 0, 0).apply(3), 0);
        assertEquals(27, CubicPolynomial.of(1, 0, 0, 0).apply(3), 0);
        assertEquals(20, CubicPolynomial.of(1, -1, 1, -1).apply(3), 0);
    }

    @Test
    public void applyMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, CubicPolynomial.of(0, 0, 0, 1).applyMulX(3), 0);
        assertEquals(9, CubicPolynomial.of(0, 0, 1, 0).applyMulX(3), 0);
        assertEquals(27, CubicPolynomial.of(0, 1, 0, 0).applyMulX(3), 0);
        assertEquals(81, CubicPolynomial.of(1, 0, 0, 0).applyMulX(3), 0);
        assertEquals(60, CubicPolynomial.of(1, -1, 1, -1).applyMulX(3), 0);
    }

    @Test
    public void asFunctionTest() {
        assertEquals(1, CubicPolynomial.of(0, 0, 0, 1).asFunction().applyAsDouble(3), 0);
        assertEquals(3, CubicPolynomial.of(0, 0, 1, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(9, CubicPolynomial.of(0, 1, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(27, CubicPolynomial.of(1, 0, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(20, CubicPolynomial.of(1, -1, 1, -1).asFunction().applyAsDouble(3), 0);
        {
            var f = CubicPolynomial.of(1, 2, 3, 4).asFunction();
            assertEquals(4, f.applyAsDouble(0), 0);
            assertEquals(10, f.applyAsDouble(1), 0);
            assertEquals(26, f.applyAsDouble(2), 0);
        }
    }

    @Test
    public void asFunctionMulXTest() {
        assertEquals(3, CubicPolynomial.of(0, 0, 0, 1).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(9, CubicPolynomial.of(0, 0, 1, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(27, CubicPolynomial.of(0, 1, 0, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(81, CubicPolynomial.of(1, 0, 0, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(60, CubicPolynomial.of(1, -1, 1, -1).asFunctionMulX().applyAsDouble(3), 0);
        {
            var f = CubicPolynomial.of(1, 2, 3, 4).asFunctionMulX();
            assertEquals(0, f.applyAsDouble(0), 0);
            assertEquals(10, f.applyAsDouble(1), 0);
            assertEquals(52, f.applyAsDouble(2), 0);
        }
    }

    @Test
    public void asStringTest() {
        assertEquals(
            "0.06000*x^3 + 0.2000*x^2 + -0.3000*x + 0.4000",
            CubicPolynomial.of(.06, .2, -.3, .4).asString()
        );
        assertEquals(
                "0.06000*r^3 + 0.2000*r^2 + -0.3000*r + 0.4000",
                CubicPolynomial.of(.06, .2, -.3, .4).asString("r")
        );
    }

    @Test
    public void parameterStringTest() {
        assertEquals("P3 0.06 0.2 -0.3 0.4", CubicPolynomial.of(.06, .2, -.3, .4).parameterString());
    }

    @Test
    public void mulTest() {
        var q = CubicPolynomial.of(2, 3, 4, 5);
        var p = q.mul(-5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, -10, -15, -20, -25);
    }

    @Test
    public void subTest() {
        var q = CubicPolynomial.of(2, 3, 4, 5);
        var p = q.sub(-5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, 2, 3, 4, 10);
    }

    @Test
    public void addTest() {
        var q = CubicPolynomial.of(2, 3, 4, 5);
        var p = q.add(5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, 2, 3, 4, 10);
    }

    @Test
    public void derivativeTest() {
        CubicPolynomial q = CubicPolynomial.of(2, 30, 4, 5);
        var p = q.derivative();
        assertOf(q, 2, 30, 4, 5);
        assertTrue(p instanceof QuadraticPolynomial);
        assertEquals(6, p.a, 0);
        assertEquals(60, p.b, 0);
        assertEquals(4, p.c, 0);
    }

    @Test
    public void maxInRangeTest() {
        var p = CubicPolynomial.of(1, -1, -9, 9); // roots: -3 1 3
        assertEquals(16, p.maxInRange(-1, 0), 0);
        assertEquals(21, p.maxInRange(3, 4), 0);
        assertEquals(21, p.maxInRange(-4, 4), 0);
        assertEquals(16.9, p.maxInRange(-2, -1), 0.01);
        assertEquals(16.9, p.maxInRange(-2, 1), 0.01);
        assertEquals(16.9, p.maxInRange(-4, 3.5), 0.01);
    }

    @Test
    public void minInRangeTest() {
        var p = CubicPolynomial.of(1, -1, -9, 9); // roots: -3 1 3
        assertEquals(9, p.minInRange(-1, 0), 0);
        assertEquals(0, p.minInRange(3, 4), 0);
        assertEquals(-5.05, p.minInRange(1, 3), 0.01);
        assertEquals(-5.05, p.minInRange(-3.1, 4), 0.01);
        assertEquals(-35, p.minInRange(-4, 4), 0);
    }

    @Test
    public void findRootsInTest() {
        var q = CubicPolynomial.of(1, -1, -9, 9); // roots: -3 1 3
        assertArrayEquals(new double[]{-3, 1, 3}, q.findRootsIn(-10, 10), 0.01);
        assertArrayEquals(new double[]{-3, 1}, q.findRootsIn(-10, 2), 0.01);
        assertArrayEquals(new double[]{-3}, q.findRootsIn(-10, 0), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(0, 2), 0.01);
        assertArrayEquals(new double[]{1, 3}, q.findRootsIn(0, 10), 0.01);
        assertArrayEquals(new double[]{3}, q.findRootsIn(2, 10), 0.01);
        // TODO: there are problems when root==limit, an FP number may be included or not included in the result list
    }

    @Test
    public void findRootsTest() {
        var q = CubicPolynomial.of(1, -1, -9, 9); // roots: -3 1 3
        assertArrayEquals(new double[]{-3, 1, 3}, q.findRoots(), 0.01);
    }

    @Test
    public void findEqualInTest() {
        var q = CubicPolynomial.of(1, -1, -9, 12); // roots: -3 1 3
        assertArrayEquals(new double[]{-3, 1, 3}, q.findEqualIn(3, -10, 10), 0.01);
    }

    @Test
    public void toStringTest() {
        assertEquals(
            "CubicPolynomial{a=1.0, b=-1.0, c=-9.0, d=9.0, 1.000*x^3 + -1.000*x^2 + -9.000*x + 9.000}"
            , CubicPolynomial.of(1, -1, -9, 9).toString()
        );
    }

    void assertOf(CubicPolynomial p, double a, double b, double c, double d) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
        assertEquals(d, p.d, 0);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(CubicPolynomial.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}