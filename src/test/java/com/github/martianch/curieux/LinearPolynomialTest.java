package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class LinearPolynomialTest {

    @Test
    public void ofTest() {
        var q = LinearPolynomial.of(10, 20);
        assertEquals(10, q.a, 0);
        assertEquals(20, q.b, 0);
    }

    @Test
    public void fromParamStringTest() {
        var q = LinearPolynomial.of(10, 20);
        String params = q.parameterString();
        var p = LinearPolynomial.fromParamString(params);
        assertOf(q, 10, 20);
        assertOf((LinearPolynomial)p.get(), 10, 20);
    }

    @Test
    public void fromParameterStringTest() {
        var q = LinearPolynomial.of(10, 20);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertOf(q, 10, 20);
        assertOf((LinearPolynomial)p.get(), 10, 20);
    }

    @Test
    public void from2PointsTest() {
        var q = LinearPolynomial.from2Points(10, 0, 0, 20);
        assertEquals(-2, q.a, 0);
        assertEquals(20, q.b, 0);
    }

    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, LinearPolynomial.of(0, 1).apply(3), 0);
        assertEquals(3, LinearPolynomial.of(1, 0).apply(3), 0);
        assertEquals(2, LinearPolynomial.of(1, -1).apply(3), 0);
    }

    @Test
    public void applyMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, LinearPolynomial.of(0, 1).applyMulX(3), 0);
        assertEquals(9, LinearPolynomial.of(1, 0).applyMulX(3), 0);
        assertEquals(6, LinearPolynomial.of(1, -1).applyMulX(3), 0);
    }

    @Test
    public void asFunctionTest() {
        assertEquals(1, LinearPolynomial.of(0, 1).asFunction().applyAsDouble(3), 0);
        assertEquals(3, LinearPolynomial.of(1, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(2, LinearPolynomial.of(1, -1).asFunction().applyAsDouble(3), 0);
    }

    @Test
    public void asFunctionMulXTest() {
        assertEquals(3, LinearPolynomial.of(0, 1).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(9, LinearPolynomial.of(1, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(6, LinearPolynomial.of(1, -1).asFunctionMulX().applyAsDouble(3), 0);
    }

    @Test
    public void asStringTest() {
        assertEquals(
                "0.06000*x + 0.2000",
                LinearPolynomial.of(.06, .2).asString()
        );
    }

    @Test
    public void parameterStringTest() {
        assertEquals(
                "P1 0.06 0.2",
                LinearPolynomial.of(.06, .2).parameterString()
        );
    }

    @Test
    public void maxInRangeTest() {
        var p = LinearPolynomial.of(-2, 3);
        assertEquals(5, p.maxInRange(-1, 0), 0);
        assertEquals(-3, p.maxInRange(3, 4), 0);
        assertEquals(11, p.maxInRange(-4, 4), 0);
    }

    @Test
    public void minInRangeTest() {
        var p = LinearPolynomial.of(-2, 3);
        assertEquals(3, p.minInRange(-1, 0), 0);
        assertEquals(-5, p.minInRange(3, 4), 0);
        assertEquals(-5, p.minInRange(-4, 4), 0);
    }

    @Test
    public void mulTest() {
        var q = LinearPolynomial.of(2, 3);
        var p = q.mul(-5);
        assertOf(q, 2, 3);
        assertOf(p, -10, -15);
    }

    @Test
    public void subTest() {
        var q = LinearPolynomial.of(4, 5);
        var p = q.sub(-5);
        assertOf(q, 4, 5);
        assertOf(p, 4, 10);
    }

    @Test
    public void addTest() {
        var q = LinearPolynomial.of(4, 5);
        var p = (LinearPolynomial) q.add(5); // TODO do we need to declare the real return type?
        assertOf(q, 4, 5);
        assertOf(p, 4, 10);
    }

    @Test
    public void derivativeTest() {
        var q = LinearPolynomial.of(4, 5);
        var p = q.derivative();
        assertOf(q, 4, 5);
        assertTrue(p instanceof ConstantPolynomial);
        assertEquals(4, p.a, 0);
    }

    @Test
    public void findRootsTest() {
        var q = LinearPolynomial.of(-0.5, 1);
        assertArrayEquals(new double[]{2}, q.findRoots(), 0);
    }

    @Test
    public void findRootsInTest() {
        var q = LinearPolynomial.of(-0.5, 1);
        assertArrayEquals(new double[]{2}, q.findRootsIn(1, 4), 0);
        assertArrayEquals(new double[]{}, q.findRootsIn(0, 1), 0);
    }

    @Test
    public void findEqualInTest() {
        var q = LinearPolynomial.of(-0.5, 1);
        assertArrayEquals(new double[]{}, q.findEqualIn(3, 1, 4), 0);
        assertArrayEquals(new double[]{-4}, q.findEqualIn(3, -4, 4), 0);
        assertArrayEquals(new double[]{-4}, q.findEqualIn(3, -5, 4), 0);
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "LinearPolynomial{a=1.0, b=-1.0, 1.000*x + -1.000}"
                , LinearPolynomial.of(1, -1).toString()
        );
    }

    void assertOf(LinearPolynomial p, double a, double b) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(CubicPolynomial.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }

}