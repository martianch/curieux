package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConstantPolynomialTest {

    @Test
    public void ofTest() {
        var p = ConstantPolynomial.of(5);
        assertEquals(5, p.a, 0);
    }

    @Test
    public void fromParamStringTest() {
        var q = LinearPolynomial.of(10, 20);
        String params = q.parameterString();
        var p = LinearPolynomial.fromParamString(params);
        assertEquals(q.a, 10, 0);
        assertEquals(((LinearPolynomial) p.get()).a, 10, 0);
    }

    @Test
    public void fromParameterStringTest() {
        var q = LinearPolynomial.of(10, 20);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertEquals(q.a, 10, 0);
        assertEquals(((LinearPolynomial) p.get()).a, 10, 0);
    }

    @Test
    public void from1PointsTest() {
        ConstantPolynomial p = ConstantPolynomial.from1Points(10, 15);
        assertEquals(p.a, 15, 0);
    }

    @Test
    public void applyTest() {
        var p = ConstantPolynomial.of(5);
        assertEquals(5, p.apply(4), 0);
        assertEquals(5, p.apply(400), 0);
        assertEquals(5, p.apply(Double.NaN), 0);
    }

    @Test
    public void asFunctionTest() {
        var p = ConstantPolynomial.of(5);
        assertEquals(5, p.asFunction().applyAsDouble(4), 0);
        assertEquals(5, p.asFunction().applyAsDouble(400), 0);
        assertEquals(5, p.asFunction().applyAsDouble(Double.NaN), 0);
    }

    @Test
    public void asStringTest() {
        assertEquals(
                "0.06000",
                ConstantPolynomial.of(.06).asString()
        );
    }

    @Test
    public void parameterStringTest() {
        assertEquals(
                "P0 0.06",
                ConstantPolynomial.of(.06).parameterString()
        );
    }

    @Test
    public void maxInRangeTest() {
        var p = ConstantPolynomial.of(-2);
        assertEquals(-2, p.maxInRange(-1, 0), 0);
        assertEquals(-2, p.maxInRange(3, 4), 0);
        assertEquals(-2, p.maxInRange(-4, 4), 0);
    }

    @Test
    public void minInRangeTest() {
        var p = ConstantPolynomial.of(-2);
        assertEquals(-2, p.minInRange(-1, 0), 0);
        assertEquals(-2, p.minInRange(3, 4), 0);
        assertEquals(-2, p.minInRange(-4, 4), 0);
    }

    @Test
    public void mulTest() {
        var q = ConstantPolynomial.of(2);
        var p = q.mul(-5);
        assertEquals(2, q.apply(Double.NaN), 0);
        assertEquals(-10, p.apply(Double.NaN), 0);
    }

    @Test
    public void subTest() {
        var q = ConstantPolynomial.of(5);
        var p = q.sub(-5);
        assertEquals(5, q.apply(Double.NaN), 0);
        assertEquals(10, p.apply(Double.NaN), 0);
    }

    @Test
    public void addTest() {
        var q = ConstantPolynomial.of(5);
        var p = q.add(5);
        assertEquals(5, q.apply(Double.NaN), 0);
        assertEquals(10, p.apply(Double.NaN), 0);
    }

    @Test
    public void derivativeTest() {
        var q = ConstantPolynomial.of(5);
        var p = q.derivative();
        assertEquals(5, q.apply(Double.NaN), 0);
        assertEquals(0, p.apply(Double.NaN), 0);
        assertTrue(q instanceof ConstantPolynomial);
    }

    @Test
    public void findRootsTest() {
        var q = ConstantPolynomial.of(5);
        assertArrayEquals(new double[]{}, q.findRoots(), 0);
    }

    @Test
    public void findRootsInTest() {
        var q = ConstantPolynomial.of(5);
        assertArrayEquals(new double[]{}, q.findRootsIn(-5, 5), 0);
    }

    @Test
    public void findEqualInTest() {
        var q = ConstantPolynomial.of(5);
        assertArrayEquals(new double[]{}, q.findEqualIn(6, -5, 5), 0);
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "ConstantPolynomial{a=1.0, 1.000}"
                , ConstantPolynomial.of(1).toString()
        );
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(ConstantPolynomial.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}