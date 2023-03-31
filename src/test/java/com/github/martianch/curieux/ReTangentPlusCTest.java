package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReTangentPlusCTest {

    @Test
    public void ofTest() {
        {
            var q = ReTangentPlusC.of(10, 20, 30, 40);
            assertOf(q, 10, 20, 30, 40);
            assertEquals(q.getClass(), ReTangentPlusC.class);
        }
        {
            var q = ReTangentPlusC.of(10, 20, 30, 0);
            assertOf(q, 10, 20, 30, 0);
            assertEquals(q.getClass(), ReTangentPlusC.ReTangentPlusC0.class);
        }
    }

    @Test
    public void ofTest2() {
        var q = ReTangentPlusC.of(10, 20);
        assertOf(q, 10, 20, 1, 0);
        assertEquals(q.getClass(), ReTangentPlusC.ReTangentPlusC0.class);
    }

    @Test
    public void fromParamStringTest() {
        var q = ReTangentPlusC.of(10, 20, 30, 40);
        String params = q.parameterString();
        var p = ReTangentPlusC.fromParamString(params);
        assertOf(q, 10, 20, 30, 40);
        assertOf((ReTangentPlusC)p.get(), 10, 20, 30, 40);
    }

    @Test
    public void fromParameterStringTest() {
        var q = ReTangentPlusC.of(10, 20, 30, 40);
        String params = q.parameterString();
        var p = HumanVisibleMathFunction.fromParameterString(params);
        assertOf(q, 10, 20, 30, 40);
        assertOf((ReTangentPlusC)p.get(), 10, 20, 30, 40);
    }

    @Test
    public void applyTest() {
        {
            var f = ReTangentPlusC.of(2, 1);
            assertEquals(1.5, f.apply(1./Math.sqrt(3)), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(2 / 3., 1000000);
            assertEquals(Math.sqrt(3)/2, f.apply(1000000), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 1, Math.sqrt(3), 5);
            assertEquals(7.25, f.apply(1), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 10, Math.sqrt(3), 5);
            assertEquals(7.25, f.apply(10), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(0.3, 10);
            assertEquals(1, f.apply(0), 0);
            assertEquals(1, f.apply(0.1), 1e-4);
        }
    }

    @Test
    public void xapplyTest() {
        {
            var f = ReTangentPlusC.of(2, 1);
            assertEquals(Math.sqrt(3)/2, f.xapply(1./Math.sqrt(3)), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(2 / 3., 1000000);
            assertEquals(Math.sqrt(3)*1000000/2, f.xapply(1000000), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 1, Math.sqrt(3), 5);
            assertEquals(7.25, f.xapply(1), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 10, Math.sqrt(3), 5);
            assertEquals(72.5, f.xapply(10), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(0.3, 10);
            assertEquals(0., f.xapply(0), 0);
            assertEquals(0.1, f.xapply(0.1), 1e-4);
        }
    }

    @Test
    public void asFunctionTest() {
        {
            var f = ReTangentPlusC.of(2, 1).asFunction();
            assertEquals(1.5, f.applyAsDouble(1./Math.sqrt(3)), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(2 / 3., 1000000).asFunction();
            assertEquals(Math.sqrt(3)/2, f.applyAsDouble(1000000), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 1, Math.sqrt(3), 5).asFunction();
            assertEquals(7.25, f.applyAsDouble(1), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 10, Math.sqrt(3), 5).asFunction();
            assertEquals(7.25, f.applyAsDouble(10), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(0.3, 10).asFunction();
            assertEquals(1, f.applyAsDouble(0), 0);
            assertEquals(1, f.applyAsDouble(0.1), 1e-4);
        }
    }

    @Test
    public void asFunctionXTest() {
        {
            var f = ReTangentPlusC.of(2, 1).asFunctionX();
            assertEquals(Math.sqrt(3)/2, f.applyAsDouble(1./Math.sqrt(3)), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(2 / 3., 1000000).asFunctionX();
            assertEquals(Math.sqrt(3)*1000000/2, f.applyAsDouble(1000000), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 1, Math.sqrt(3), 5).asFunctionX();
            assertEquals(7.25, f.applyAsDouble(1), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(4 / 3., 10, Math.sqrt(3), 5).asFunctionX();
            assertEquals(72.5, f.applyAsDouble(10), 1e-9);
        }
        {
            var f = ReTangentPlusC.of(0.3, 10).asFunctionX();
            assertEquals(0., f.applyAsDouble(0), 0);
            assertEquals(0.1, f.applyAsDouble(0.1), 1e-4);
        }
    }

    @Test
    public void asStringTest() {
        assertEquals(
                "(-0.3000*0.2000/0.06000)*tan(0.06000*atan(x/0.2000)) + 0.4000",
                ReTangentPlusC.of(.06, .2, -.3, .4).asString()
        );
        assertEquals(
                "(-0.3000*0.2000/0.06000)*tan(0.06000*atan(r/0.2000)) + 0.4000",
                ReTangentPlusC.of(.06, .2, -.3, .4).asString("r")
        );    }

    @Test
    public void parameterStringTest() {
        assertEquals("RETAN 0.06 0.2 -0.3 0.4", ReTangentPlusC.of(.06, .2, -.3, .4).parameterString());
    }

    @Test
    public void mulTest() {
        var q = ReTangentPlusC.of(2, 3, 4, 5);
        var p = q.mul(-5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, 2, 3, -20, -25);
    }

    @Test
    public void subTest() {
        var q = ReTangentPlusC.of(2, 3, 4, 5);
        var p = q.sub(-5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, 2, 3, 4, 10);
    }

    @Test
    public void addTest() {
        var q = ReTangentPlusC.of(2, 3, 4, 5);
        var p = q.add(5);
        assertOf(q, 2, 3, 4, 5);
        assertOf(p, 2, 3, 4, 10);
    }


    @Test
    public void derivativeTest() {
        // TODO
    }

    @Test
    public void maxInRangeTest() {
        {
            var q = ReTangentPlusC.of(0.9, 10, 18, 5);
            assertEquals(23, q.maxInRange(-10, 10), 0);
            assertEquals(23, q.maxInRange(0, 10), 0);
            assertEquals(22.082, q.maxInRange(10, 20), 1e-3);
            assertEquals(22.082, q.maxInRange(-20, -10), 1e-3);
        }
        {
            var q = ReTangentPlusC.of(1, 10, 20, 5);
            assertEquals(25, q.maxInRange(-10, 10), 0);
            assertEquals(25, q.maxInRange(0, 10), 0);
            assertEquals(25, q.maxInRange(10, 20), 1e-3);
            assertEquals(25, q.maxInRange(-20, -10), 1e-3);
        }
        {
            var q = ReTangentPlusC.of(0.9, 10, -18, -5);
            assertEquals(-22.082, q.maxInRange(-10, 10), 1e-3);
            assertEquals(-22.082, q.maxInRange(0, 10), 1e-3);
            assertEquals(-20.453, q.maxInRange(10, 20), 1e-3);
            assertEquals(-20.453, q.maxInRange(-20, -10), 1e-3);
        }
    }

    @Test
    public void minInRangeTest() {
        {
            var q = ReTangentPlusC.of(0.9, 10, -18, -5);
            assertEquals(-23, q.minInRange(-10, 10), 0);
            assertEquals(-23, q.minInRange(0, 10), 0);
            assertEquals(-22.082, q.minInRange(10, 20), 1e-3);
            assertEquals(-22.082, q.minInRange(-20, -10), 1e-3);
        }
        {
            var q = ReTangentPlusC.of(0.9, 10, 18, 5);
            assertEquals(22.082, q.minInRange(-10, 10), 1e-3);
            assertEquals(22.082, q.minInRange(0, 10), 1e-3);
            assertEquals(20.453, q.minInRange(10, 20), 1e-3);
            assertEquals(20.453, q.minInRange(-20, -10), 1e-3);
        }
        {
            var q = ReTangentPlusC.of(1, 10, 20, 5);
            assertEquals(25, q.minInRange(-10, 10), 0);
            assertEquals(25, q.minInRange(0, 10), 0);
            assertEquals(25, q.minInRange(10, 20), 1e-3);
            assertEquals(25, q.minInRange(-20, -10), 1e-3);
        }
    }

    @Test
    public void findRootsInTest() {
        var q = ReTangentPlusC.of(0.9, 10, 18, -17); // roots: +-10.533
        assertEquals(0, q.apply(10.533), 1e-3);
        assertArrayEquals(new double[]{-10.533, 10.533}, q.findRootsIn(-15, 15), 0.01);
        assertArrayEquals(new double[]{-10.533}, q.findRootsIn(-15, -2), 0.01);
        assertArrayEquals(new double[]{-10.533}, q.findRootsIn(-15, 0), 0.01);
        assertArrayEquals(new double[]{-10.533}, q.findRootsIn(-15, 2), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(1, 2), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(0, 2), 0.01);
        assertArrayEquals(new double[]{}, q.findRootsIn(-2, 2), 0.01);
        assertArrayEquals(new double[]{10.533}, q.findRootsIn(0, 15), 0.01);
        assertArrayEquals(new double[]{10.533}, q.findRootsIn(2, 15), 0.01);
        // TODO: there are problems when root==limit, an FP number may be included or not included in the result list
    }

    @Test
    public void pointsAroundIntervalsOfMonotonicityTest() {
        var q = ReTangentPlusC.of(0.9, 10, 20, -17); // roots: +-10.533
        assertArrayEquals(new double[]{-100, 0, 100}, q.pointsAroundIntervalsOfMonotonicity(-100, 100), 0.01);
        // TODO other values of k
    }

    @Test
    public void findRootsTest() {
        var q = ReTangentPlusC.of(0.9, 10, 20*0.9, -17); // roots: +-10.533
        assertArrayEquals(new double[]{-10.533, 10.533}, q.findRoots(), 0.01);
    }

    @Test
    public void findEqualInTest() {
        var q = ReTangentPlusC.of(0.9, 10, 20*0.9, 5);
        assertArrayEquals(new double[]{-193.9, 193.9}, q.findEqualIn(10, -1000, 1000), 0.1);
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "CubicPolynomial{k=0.9, q=10.0, a=20.0, c=5.0, a*q/k=222.22222222222223, "
                + "(20.00*10.00/0.9000)*tan(0.9000*atan(x/10.00)) + 5.000}"
                , ReTangentPlusC.of(0.9, 10, 20, 5).toString()
        );
    }

    void assertOf(ReTangentPlusC f, double k, double q, double a, double c) {
        assertEquals(k, f.k, 0);
        assertEquals(q, f.q, 0);
        assertEquals(a, f.a, 0);
        assertEquals(c, f.c, 0);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunction.class, getClass());
        TestCoverage.check(ReTangentPlusC.class, getClass());
        TestCoverage.checkNames(getClass(), "toString");
    }
}