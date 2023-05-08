package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BiquadraticPolynomialTest {
    Map<String, Double> vars = new HashMap<>();

    @Test
    public void from3PointsTest() {
        var p = BiquadraticPolynomial.of(QuadraticPolynomial.of(4, 3, 2));
        double[] x = new double[]{10, 140, 280};
        double[] y = Arrays.stream(x).map(p::apply).toArray();
        {
            var q = BiquadraticPolynomial.from3Points(x[0],y[0], x[1],y[1], x[2],y[2]);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            var q = BiquadraticPolynomial.from3Points(x[1],y[1], x[2],y[2], x[0],y[0]);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            var q = BiquadraticPolynomial.from3Points(x[2],y[2], x[1],y[1], x[0],y[0]);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            var q = BiquadraticPolynomial.from3Points(x[0],2*y[0], x[1],2*y[1], x[2],2*y[2]);
            assertEquals(2*p.f.a, q.f.a, 1e-9);
            assertEquals(2*p.f.b, q.f.b, 1e-9);
            assertEquals(2*p.f.c, q.f.c, 1e-9);
        }
    }

    @Test
    public void ofTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(10, 20, 30));
        assertOf(q, 10, 20, 30);
    }

    @Test
    public void fromParamStringTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(10, 20, 30));
        String params = q.parameterString();
        System.out.println(params);
        var p = BiquadraticPolynomial.fromParamString(params, vars);
        assertOf(q, 10, 20, 30);
        System.out.println("p="+p);
        assertTrue(p.get() instanceof BiquadraticPolynomial);
        assertOf((BiquadraticPolynomial)p.get(), 10, 20, 30);
    }

    @Test
    public void fromParameterStringTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(10, 20, 30));
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params, vars);
        assertOf(q, 10, 20, 30);
        assertOf((BiquadraticPolynomial)p.get(), 10, 20, 30);
    }

    @Test
    public void applyTest() {
        // int math should be ok with delta=0
        assertEquals(1, OfXSquared.of(QuadraticPolynomial.of(0, 0, 1)).apply(3), 0);
        assertEquals(9, OfXSquared.of(QuadraticPolynomial.of(0, 1, 0)).apply(3), 0);
        assertEquals(81, OfXSquared.of(QuadraticPolynomial.of(1, 0, 0)).apply(3), 0);
        assertEquals(73, OfXSquared.of(QuadraticPolynomial.of(1, -1, 1)).apply(3), 0);
    }

    @Test
    public void applyMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, OfXSquared.of(QuadraticPolynomial.of(0, 0, 1)).applyMulX(3), 0);
        assertEquals(27, OfXSquared.of(QuadraticPolynomial.of(0, 1, 0)).applyMulX(3), 0);
        assertEquals(243, OfXSquared.of(QuadraticPolynomial.of(1, 0, 0)).applyMulX(3), 0);
        assertEquals(219, OfXSquared.of(QuadraticPolynomial.of(1, -1, 1)).applyMulX(3), 0);
    }

    @Test
    public void asFunctionTest() {
        // int math should be ok with delta=0
        assertEquals(1, OfXSquared.of(QuadraticPolynomial.of(0, 0, 1)).asFunction().applyAsDouble(3), 0);
        assertEquals(9, OfXSquared.of(QuadraticPolynomial.of(0, 1, 0)).asFunction().applyAsDouble(3), 0);
        assertEquals(81, OfXSquared.of(QuadraticPolynomial.of(1, 0, 0)).asFunction().applyAsDouble(3), 0);
        assertEquals(73, OfXSquared.of(QuadraticPolynomial.of(1, -1, 1)).asFunction().applyAsDouble(3), 0);
        {
            var f = OfXSquared.of(QuadraticPolynomial.of(1, 2, 3)).asFunction();
            assertEquals(3, f.applyAsDouble(0), 0);
            assertEquals(6, f.applyAsDouble(1), 0);
            assertEquals(11, f.applyAsDouble(Math.sqrt(2)), 1e-14);
        }
    }

    @Test
    public void asFunctionMulXTest() {
        // int math should be ok with delta=0
        assertEquals(3, OfXSquared.of(QuadraticPolynomial.of(0, 0, 1)).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(27, OfXSquared.of(QuadraticPolynomial.of(0, 1, 0)).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(243, OfXSquared.of(QuadraticPolynomial.of(1, 0, 0)).asFunctionMulX().applyAsDouble(3), 0);
        assertEquals(219, OfXSquared.of(QuadraticPolynomial.of(1, -1, 1)).asFunctionMulX().applyAsDouble(3), 0);
        {
            var f = OfXSquared.of(QuadraticPolynomial.of(1, 2, 3)).asFunctionMulX();
            assertEquals(0, f.applyAsDouble(0), 0);
            assertEquals(6, f.applyAsDouble(1), 0);
            assertEquals(11*Math.sqrt(2), f.applyAsDouble(Math.sqrt(2)), 1e-14);
        }
    }

    @Test
    public void maxInRangeTest() {
        var p = OfXSquared.of(QuadraticPolynomial.of(-1, 6, -7));
        var f = p.asFunction();
        {
            assertEquals(-2, f.applyAsDouble(1), 0);
            assertEquals(1, f.applyAsDouble(Math.sqrt(2)), 1e-14);
            assertEquals(2, f.applyAsDouble(Math.sqrt(3)), 1e-14);
            assertEquals(1, f.applyAsDouble(2), 0);
            assertEquals(-2, f.applyAsDouble(Math.sqrt(5)), 1e-14);
        }
        assertEquals(-2, p.maxInRange(0, 1), 0);
        assertEquals(1, p.maxInRange(0, Math.sqrt(2)), 1e-14);
        assertEquals(2, p.maxInRange(0, Math.sqrt(3)), 1e-14);
        assertEquals(2, p.maxInRange(0, 2), 0);
        assertEquals(2, p.maxInRange(Math.sqrt(3), 2), 0);
        assertEquals(1, p.maxInRange(2, Math.sqrt(5)), 1e-14);
    }

    @Test
    public void minInRangeTest() {
        var p = OfXSquared.of(QuadraticPolynomial.of(1, -6, 7));
        var f = p.asFunction();
        {
            assertEquals(2, f.applyAsDouble(1), 0);
            assertEquals(-1, f.applyAsDouble(Math.sqrt(2)), 1e-14);
            assertEquals(-2, f.applyAsDouble(Math.sqrt(3)), 1e-14);
            assertEquals(-1, f.applyAsDouble(2), 0);
            assertEquals(2, f.applyAsDouble(Math.sqrt(5)), 1e-14);
        }
        assertEquals( 2, p.minInRange(0, 1), 0);
        assertEquals(-1, p.minInRange(0, Math.sqrt(2)), 1e-14);
        assertEquals(-2, p.minInRange(0, Math.sqrt(3)), 1e-14);
        assertEquals(-2, p.minInRange(0, 2), 0);
        assertEquals(-2, p.minInRange(Math.sqrt(3), 2), 1e-14);
        assertEquals(-1, p.minInRange(2, Math.sqrt(5)), 1e-14);
    }

    @Test
    public void asStringTest() {
        assertEquals("0.2000*(x^2)^2 + -0.3000*(x^2) + 0.4000", OfXSquared.of(QuadraticPolynomial.of(.2,-.3,.4)).asString());
        assertEquals("2.000*(x^2)^2 + -3.000*(x^2) + 4.000", OfXSquared.of(QuadraticPolynomial.of(2,-3,4)).asString());
        assertEquals("2.000e+05*(x^2)^2 + 3.000e-05*(x^2) + -4.000e-05", OfXSquared.of(QuadraticPolynomial.of(2e5,3e-5,-4e-5)).asString());
        assertEquals("NaN*(x^2)^2 + NaN*(x^2) + NaN", OfXSquared.of(QuadraticPolynomial.of(Double.NaN,Double.NaN,Double.NaN)).asString());
        assertEquals("0.3333*(x^2)^2 + 0.1429*(x^2) + 0.2500", OfXSquared.of(QuadraticPolynomial.of(1./3,1./7,1./4)).asString());
    }

    @Test
    public void parameterStringTest() {
        assertEquals(
                "OFSQR P2 -3.0 0.06 0.2",
                OfXSquared.of(QuadraticPolynomial.of(-3, .06, .2)).parameterString()
        );
    }

    @Test
    public void mulTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(1, 2, 3));
        var p = q.mul(-3);
        assertTrue(p.f instanceof QuadraticPolynomial); // check of the unchecked cast inside mul()
        assertEquals(-3, p.f.a, 0);
        assertEquals(-6, p.f.b, 0);
        assertEquals(-9, p.f.c, 0);
        assertEquals(27, q.apply(2), 0);
        assertEquals(-81, p.apply(2), 0);
    }

    @Test
    public void subTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(3, 4, 5));
        var p = q.sub(-5);
        assertTrue(p.f instanceof QuadraticPolynomial); // check of the unchecked cast inside sub()
        assertOf(q, 3, 4, 5);
        assertOf(p, 3, 4, 10);
        assertEquals(69, q.apply(2), 0);
        assertEquals(74, p.apply(2), 0);
    }

    @Test
    public void addTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(3, 4, 5));
        var p = (BiquadraticPolynomial) q.add(5);
        assertOf(q, 3, 4, 5);
        assertOf(p, 3, 4, 10);
        assertEquals(69, q.apply(2), 0);
        assertEquals(74, p.apply(2), 0);
    }

    @Test
    public void derivativeTest() {
        var q = OfXSquared.of(QuadraticPolynomial.of(30, 4, 5));
        try {
            var p = q.derivative();
            fail("expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void findRootsTest() {
        {
            var q = OfXSquared.of(QuadraticPolynomial.of(1, -4, 3));
            assertArrayEquals(new double[]{-Math.sqrt(3), -1, 1, Math.sqrt(3)}, q.findRoots(), 0);
        }
        {
            var q = OfXSquared.of(QuadraticPolynomial.of(1, 0, 1));
            assertArrayEquals(new double[]{}, q.findRoots(), 0);
        }
        {
            var q = OfXSquared.of(QuadraticPolynomial.of(1, -2, 1));
            assertArrayEquals(new double[]{-1, 1}, q.findRoots(), 0);
        }
    }

    @Test
    public void findRootsInTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(1, -4, 3));
        assertArrayEquals(new double[]{-Math.sqrt(3), -1, 1, Math.sqrt(3)}, q.findRootsIn(-4, 4), 0.01);
        assertArrayEquals(new double[]{-Math.sqrt(3), -1, 1, Math.sqrt(3)}, q.findRootsIn(-2, 2), 0.01);
        assertArrayEquals(new double[]{1, Math.sqrt(3)}, q.findRootsIn(1, 3), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(-4, -2), 0.01);
        assertArrayEquals(new double[]{-Math.sqrt(3), -1}, q.findRootsIn(-4, 0), 0.01);
        assertArrayEquals(new double[]{Math.sqrt(3)}, q.findRootsIn(1.5, 2.5), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(1.8, 2.5), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(4, 10), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(0, 1.5), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(1, 1.5), 0.01);
        assertArrayEquals(new double[]{1}, q.findRootsIn(0, 1), 0.01);
        assertArrayEquals(new double[]{Math.sqrt(3)}, q.findRootsIn(1.5, 4), 0.01);
        assertArrayEquals(new double[]{Math.sqrt(3)}, q.findRootsIn(1.5, Math.sqrt(3)+1e-15), 0.01);
        assertArrayEquals(new double[]{Math.sqrt(3)}, q.findRootsIn(Math.sqrt(3), 2), 0.01);
        assertOf(q, 1, -4, 3);
    }

    @Test
    public void findEqualInTest() {
        {
            var q = OfXSquared.of(QuadraticPolynomial.of(1, 0, 1));
            assertArrayEquals(new double[]{-1, 1}, q.findEqualIn(2, -4, 4), 0.01);
        }
        {
            var q = OfXSquared.of(QuadraticPolynomial.of(1, 0, -16));
            assertArrayEquals(new double[]{-3, 3}, q.findEqualIn(65, -4, 4), 0.01);
        }
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "OfXSquared{QuadraticPolynomial{a=1.0, b=-1.0, c=-9.0, 1.000*x^2 + -1.000*x + -9.000}}"
                , OfXSquared.of(QuadraticPolynomial.of(1, -1, -9)).toString()
        );
    }

    @Test
    public void toQuarticTest() {
        var q = BiquadraticPolynomial.of(QuadraticPolynomial.of(10, 20, 30));
        var p = q.toQuartic();
        assertOf(q, 10, 20, 30);
        assertOf(p, 10, 0, 20, 0, 30);
    }

    @Test
    public void v_ofTest() {
        OfXSquared<QuadraticPolynomial> q = BiquadraticPolynomial.of(QuadraticPolynomial.of(10, 20, 30));
        var p = q.v_of(QuadraticPolynomial.of(1,2,3));
        assertTrue(p instanceof BiquadraticPolynomial);
        assertTrue(q instanceof BiquadraticPolynomial);
    }

    static void assertOf(QuadraticPolynomial p, double a, double b, double c) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
    }

    static void assertOf(BiquadraticPolynomial q, double a, double b, double c) {
        var p = q.f;
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
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
        TestCoverage.check(OfXSquared.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}
