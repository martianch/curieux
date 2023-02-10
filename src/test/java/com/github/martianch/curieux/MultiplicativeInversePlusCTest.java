package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class MultiplicativeInversePlusCTest {

    @Test
    public void ofTest() {
        var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1);
        assertEquals(1, p.c, 0);
        assertEquals(1, p.f.a, 0);
        assertEquals(0, p.f.b, 0);
    }

    @Test
    public void from3PointsTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), 0);
        double[] x = new double[]{4, 60, 10};
        double[] y = Arrays.stream(x).map(p::apply).toArray();
        {
            MultiplicativeInversePlusC<QuadraticPolynomial> q = MultiplicativeInversePlusC.from3Points(x[0],y[0], x[1],y[1], x[2],y[2], QuadraticPolynomial::from3Points);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            MultiplicativeInversePlusC<QuadraticPolynomial> q = MultiplicativeInversePlusC.from3Points(x[1],y[1], x[2],y[2], x[0],y[0], QuadraticPolynomial::from3Points);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            MultiplicativeInversePlusC<QuadraticPolynomial> q = MultiplicativeInversePlusC.from3Points(x[2],y[2], x[1],y[1], x[0],y[0], QuadraticPolynomial::from3Points);
            assertEquals(p.f.a, q.f.a, 1e-9);
            assertEquals(p.f.b, q.f.b, 1e-9);
            assertEquals(p.f.c, q.f.c, 1e-9);
        }
        {
            MultiplicativeInversePlusC<QuadraticPolynomial> q = MultiplicativeInversePlusC.from3Points(x[0],2*y[0], x[1],2*y[1], x[2],2*y[2], QuadraticPolynomial::from3Points);
            assertEquals(0.5*p.f.a, q.f.a, 1e-9);
            assertEquals(0.5*p.f.b, q.f.b, 1e-9);
            assertEquals(0.5*p.f.c, q.f.c, 1e-9);
        }

    }

    @Test
    public void fromParamStringTest() {
        var q = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(10, 20, 30), 14);
        String params = q.parameterString();
        System.out.println(params);
        var p = MultiplicativeInversePlusC.fromParamString(params);
        assertOf(q.f, 10, 20, 30);
        System.out.println("p="+p);
        assertTrue(p.get() instanceof MultiplicativeInversePlusC);
        MultiplicativeInversePlusC m = (MultiplicativeInversePlusC) p.get();
        assertOf((QuadraticPolynomial) m.f, 10, 20, 30);
        assertEquals(m.c, 14, 0);
    }

    @Test
    public void fromParameterStringTest() {
        var q = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(10, 20, 30), 14);
        String params = q.parameterString();
        System.out.println(params);
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertOf(q.f, 10, 20, 30);
        System.out.println("p="+p);
        assertTrue(p.get() instanceof MultiplicativeInversePlusC);
        MultiplicativeInversePlusC m = (MultiplicativeInversePlusC) p.get();
        assertOf((QuadraticPolynomial) m.f, 10, 20, 30);
        assertEquals(m.c, 14, 0);
    }

    @Test
    public void applyTest() {
        {
            var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.5, p.apply(2), 0);
            assertEquals(3, p.apply(0.5), 0);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 1);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.25, p.apply(2), 0);
            assertEquals(5, p.apply(0.5), 0);
        }
    }

    @Test
    public void asFunctionTest() {
        {
            var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1).asFunction();
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.5, p.apply(2), 0);
            assertEquals(3, p.apply(0.5), 0);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 1).asFunction();
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.25, p.apply(2), 0);
            assertEquals(5, p.apply(0.5), 0);
        }
    }

    @Test
    public void asStringTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(2, 3, 4), 5);
        assertEquals("5.000 + 1/( 2.000*x^2 + 3.000*x + 4.000 )", p.asString());
    }

    @Test
    public void parameterStringTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(2, 3, 4), 5);
        assertEquals("C+1/ 5.0 P2 2.0 3.0 4.0", p.parameterString());
    }

    @Test
    public void maxInRangeTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), 1);
        assertFalse(Double.isFinite(p.maxInRange(2, 5)));
        assertFalse(Double.isFinite(p.maxInRange(0, 2)));
        assertFalse(Double.isFinite(p.maxInRange(0, 5)));
        assertEquals(0, p.maxInRange(1.5, 2.5), 0);
        assertEquals(1.8, p.maxInRange(3.5, 7.5), 0);
        assertEquals(1.8, p.maxInRange(-5, 0.5), 0);
    }

    @Test
    public void minInRangeTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), 1);
        assertFalse(Double.isFinite(p.minInRange(2, 5)));
        assertFalse(Double.isFinite(p.minInRange(0, 2)));
        assertFalse(Double.isFinite(p.minInRange(0, 5)));
        assertEquals(-0.333333, p.minInRange(1.5, 2.5), 1e-5);
        assertEquals(1.021, p.minInRange(3.5, 9), 1e-3);
        assertEquals(1.021, p.minInRange(-5, 0.5), 1e-3);
    }

    @Test
    public void mulTest() {
        {
            var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1);
            var q = p.mul(10);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.5, p.apply(2), 0);
            assertEquals(3, p.apply(0.5), 0);
            assertEquals(20, q.apply(1), 0);
            assertEquals(15, q.apply(2), 0);
            assertEquals(30, q.apply(0.5), 0);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 1);
            var q = p.mul(20);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.25, p.apply(2), 0);
            assertEquals(5, p.apply(0.5), 0);
            assertEquals(40, q.apply(1), 0);
            assertEquals(25, q.apply(2), 0);
            assertEquals(100, q.apply(0.5), 0);
        }
    }

    @Test
    public void subTest() {
        {
            var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1);
            var q = p.sub(-100);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.5, p.apply(2), 0);
            assertEquals(3, p.apply(0.5), 0);
            assertEquals(102, q.apply(1), 0);
            assertEquals(101.5, q.apply(2), 0);
            assertEquals(103, q.apply(0.5), 0);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 1);
            var q = p.sub(2);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.25, p.apply(2), 0);
            assertEquals(5, p.apply(0.5), 0);
            assertEquals(2-2, q.apply(1), 0);
            assertEquals(1.25-2, q.apply(2), 0);
            assertEquals(5-2, q.apply(0.5), 0);
        }
    }

    @Test
    public void addTest() {
        {
            var p = MultiplicativeInversePlusC.of(LinearPolynomial.of(1, 0), 1);
            var q = p.add(100);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.5, p.apply(2), 0);
            assertEquals(3, p.apply(0.5), 0);
            assertEquals(102, q.apply(1), 0);
            assertEquals(101.5, q.apply(2), 0);
            assertEquals(103, q.apply(0.5), 0);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 1);
            var q = p.add(-2);
            assertEquals(2, p.apply(1), 0);
            assertEquals(1.25, p.apply(2), 0);
            assertEquals(5, p.apply(0.5), 0);
            assertEquals(2-2, q.apply(1), 0);
            assertEquals(1.25-2, q.apply(2), 0);
            assertEquals(5-2, q.apply(0.5), 0);
        }
    }

    @Test
    public void derivativeTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 0);
        try {
            var d = p.derivative();
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void findRootsTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), -1);
        double[] roots = p.findRoots();
        assertArrayEquals(new double[]{0.586, 3.414}, roots, 0.001);
    }
    @Test
    public void findRootsInTest() {
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), -1);
            assertArrayEquals(new double[]{0.586, 3.414}, p.findRootsIn(-2, 8), 0.001);
            assertArrayEquals(new double[]{0.586}, p.findRootsIn(0, 2), 0.001);
            assertArrayEquals(new double[]{0.586}, p.findRootsIn(0, 3), 0.001);
            assertArrayEquals(new double[]{3.414}, p.findRootsIn(2, 4), 0.001);
            assertArrayEquals(new double[]{}, p.findRootsIn(-4, 0), 0.001);
            assertArrayEquals(new double[]{}, p.findRootsIn(1, 3), 0.001);
        }
        {
            var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, -4, 3), 1);
            assertArrayEquals(new double[]{2}, p.findRootsIn(1, 3), 0.001);
        }
    }

    @Test
    public void findEqualInTest() {
        var p = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(1, 0, 0), 0);
        assertArrayEquals(new double[]{-1, 1}, p.findEqualIn(1, -100,100), 0);
    }

    @Test
    public void toStringTest() {
        var q = MultiplicativeInversePlusC.of(QuadraticPolynomial.of(10, 20, 30), 14);
        assertEquals(
                "MultiplicativeInversePlusC"
                + "{f=QuadraticPolynomial{a=10.0, b=20.0, c=30.0, 10.00*x^2 + 20.00*x + 30.00}"
                + ", c=14.0"
                + ", 14.00 + 1/( 10.00*x^2 + 20.00*x + 30.00 )"
                + "}",
                q.toString()
        );
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(MultiplicativeInversePlusC.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }

    static void assertOf(QuadraticPolynomial p, double a, double b, double c) {
        assertEquals(a, p.a, 0);
        assertEquals(b, p.b, 0);
        assertEquals(c, p.c, 0);
    }
}
