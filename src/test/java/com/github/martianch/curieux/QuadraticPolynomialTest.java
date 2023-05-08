package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class QuadraticPolynomialTest {
    Map<String, Double> vars = new HashMap<>();

    @Test
    public void ofTest() {
        var q = QuadraticPolynomial.of(10, 20, 30);
        assertOf(q, 10, 20, 30);
    }

    @Test
    public void fromParamStringTest() {
        var q = QuadraticPolynomial.of(10, 20, 30);
        String params = q.parameterString();
        var p = QuadraticPolynomial.fromParamString(params, vars);
        assertOf(q, 10, 20, 30);
        assertOf((QuadraticPolynomial)p.get(), 10, 20, 30);
    }

    @Test
    public void fromParameterStringTest() {
        var q = QuadraticPolynomial.of(10, 20, 30);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params, vars);
        assertOf(q, 10, 20, 30);
        assertOf((QuadraticPolynomial)p.get(), 10, 20, 30);
    }

    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, QuadraticPolynomial.of(0, 0, 1).apply(3), 0);
        assertEquals(3, QuadraticPolynomial.of(0, 1, 0).apply(3), 0);
        assertEquals(9, QuadraticPolynomial.of(1, 0, 0).apply(3), 0);
        assertEquals(7, QuadraticPolynomial.of(1, -1, 1).apply(3), 0);
    }

    @Test
    public void applyMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, QuadraticPolynomial.of(0, 0, 1).applyMulX(3), 0);
        assertEquals(9, QuadraticPolynomial.of(0, 1, 0).applyMulX(3), 0);
        assertEquals(27, QuadraticPolynomial.of(1, 0, 0).applyMulX(3), 0);
        assertEquals(21, QuadraticPolynomial.of(1, -1, 1).applyMulX(3), 0);
    }

    @Test
    public void asFunctionTest() {
        // int math should be ok with delta=0
        assertEquals(1, QuadraticPolynomial.of(0, 0, 1).asFunction().applyAsDouble(3), 0);
        assertEquals(3, QuadraticPolynomial.of(0, 1, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(9, QuadraticPolynomial.of(1, 0, 0).asFunction().applyAsDouble(3), 0);
        assertEquals(7, QuadraticPolynomial.of(1, -1, 1).asFunction().applyAsDouble(3), 0);
        {
            var f = QuadraticPolynomial.of(1, 2, 3).asFunction();
            assertEquals(3, f.applyAsDouble(0), 0);
            assertEquals(6, f.applyAsDouble(1), 0);
            assertEquals(11, f.applyAsDouble(2), 0);
        }
    }

    @Test
    public void asFunctionMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, QuadraticPolynomial.of(0, 0, 1).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(9, QuadraticPolynomial.of(0, 1, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(27, QuadraticPolynomial.of(1, 0, 0).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(21, QuadraticPolynomial.of(1, -1, 1).asFunctionMulX().applyAsDouble(3), 0);
        {
            var f = QuadraticPolynomial.of(1, 2, 3).asFunctionMulX();
            assertEquals(0, f.applyAsDouble(0), 0);
            assertEquals(6, f.applyAsDouble(1), 0);
            assertEquals(22, f.applyAsDouble(2), 0);
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
            assertEquals(-2, f.applyAsDouble(1), 0);
            assertEquals(1, f.applyAsDouble(2), 0);
            assertEquals(2, f.applyAsDouble(3), 0);
            assertEquals(1, f.applyAsDouble(4), 0);
            assertEquals(-2, f.applyAsDouble(5), 0);
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
            assertEquals(2, f.applyAsDouble(1), 0);
            assertEquals(-1, f.applyAsDouble(2), 0);
            assertEquals(-2, f.applyAsDouble(3), 0);
            assertEquals(-1, f.applyAsDouble(4), 0);
            assertEquals(2, f.applyAsDouble(5), 0);
        }
        assertEquals( 2, p.minInRange(0, 1), 0);
        assertEquals(-1, p.minInRange(0, 2), 0);
        assertEquals(-2, p.minInRange(0, 3), 0);
        assertEquals(-2, p.minInRange(0, 4), 0);
        assertEquals(-2, p.minInRange(3, 4), 0);
        assertEquals(-1, p.minInRange(4, 5), 0);
    }

    @Test
    public void from3PointsTest() {
        var p = QuadraticPolynomial.of(4, 3, 2);
        double[] x = new double[]{10, 140, 280};
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
    public void parameterStringTest() {
        assertEquals(
                "P2 -3.0 0.06 0.2",
                QuadraticPolynomial.of(-3, .06, .2).parameterString()
        );
    }

    @Test
    public void mulTest() {
        var p = QuadraticPolynomial.of(1, 2, 3).mul(-3);
        assertEquals(-3, p.a, 0);
        assertEquals(-6, p.b, 0);
        assertEquals(-9, p.c, 0);
    }

    @Test
    public void subTest() {
        var q = QuadraticPolynomial.of(3, 4, 5);
        var p = q.sub(-5);
        assertOf(q, 3, 4, 5);
        assertOf(p, 3, 4, 10);
    }

    @Test
    public void addTest() {
        var q = QuadraticPolynomial.of(3, 4, 5);
        var p = q.add(5);
        assertOf(q, 3, 4, 5);
        assertOf(p, 3, 4, 10);
    }

    @Test
    public void derivativeTest() {
        QuadraticPolynomial q = QuadraticPolynomial.of(30, 4, 5);
        var p = q.derivative();
        assertOf(q, 30, 4, 5);
        assertTrue(p instanceof LinearPolynomial);
        assertEquals(60, p.a, 0);
        assertEquals(4, p.b, 0);
    }

    @Test
    public void findRootsTest() {
        {
            var q = QuadraticPolynomial.of(1, -4, 3);
            assertArrayEquals(new double[]{1, 3}, q.findRoots(), 0);
        }
        {
            var q = QuadraticPolynomial.of(1, 0, 1);
            assertArrayEquals(new double[]{}, q.findRoots(), 0);
        }
        {
            var q = QuadraticPolynomial.of(1, -2, 1);
            assertArrayEquals(new double[]{1}, q.findRoots(), 0);
        }
    }

    @Test
    public void findRootsInTest() {
        var q = QuadraticPolynomial.of(1, -4, 3);
        assertArrayEquals(new double[]{1, 3}, q.findRootsIn(-4, 4), 0.01);
        assertArrayEquals(new double[]{1, 3}, q.findRootsIn(1, 3), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(-4, 0), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(1.5, 2.5), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(4, 10), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(0, 2), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(1, 2), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(0, 1), 0.01);
        assertArrayEquals(new double[]{3}, q.findRootsIn(2, 4), 0.01);
        assertArrayEquals(new double[]{3}, q.findRootsIn(2, 3), 0.01);
        assertArrayEquals(new double[]{3}, q.findRootsIn(3, 4), 0.01);
        assertOf(q, 1, -4, 3);
    }

    @Test
    public void findEqualInTest() {
        var q = QuadraticPolynomial.of(1, 0, 1);
        assertArrayEquals(new double[]{-1, 1}, q.findEqualIn(2, -4, 4), 0.01);
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "QuadraticPolynomial{a=1.0, b=-1.0, c=-9.0, 1.000*x^2 + -1.000*x + -9.000}"
                , QuadraticPolynomial.of(1, -1, -9).toString()
        );
    }

    static void assertOf(QuadraticPolynomial p, double a, double b, double c) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(QuadraticPolynomial.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}
