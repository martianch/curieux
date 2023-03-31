package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

public class HumanVisibleMathFunctionBaseTest {

    @Test
    public void isBetweenTest() {
        assertTrue(HumanVisibleMathFunctionBase.isBetween(1, 2, 3));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(3, 2, 1));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(1, 1, 3));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(3, 1, 1));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(1, 3, 3));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(3, 3, 1));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(-1, -2, -3));
        assertTrue(HumanVisibleMathFunctionBase.isBetween(-3, -2, -1));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(-1, 2, -3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(-3, 2, -1));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(-1, -4, -3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(-3, -4, -1));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, -2, 3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(3, -2, 1));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, 4, 3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(3, 4, 1));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, 4 / 0., 3));
        assertEquals(Double.POSITIVE_INFINITY, 4 / 0., 0); // FYI
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, .0 / 0., 3));
        assertEquals(Double.NaN, .0 / 0., 0); // FYI
        assertNotEquals(Double.NaN, 1, 0); // FYI
        assertNotEquals(Double.NaN, Double.POSITIVE_INFINITY, 0); // FYI
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, Math.sqrt(-4), 3));
        assertEquals(Double.NaN, Math.sqrt(-4) + 1, 0); // FYI
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, Double.NaN, 3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, Double.NEGATIVE_INFINITY, 3));
        assertFalse(HumanVisibleMathFunctionBase.isBetween(1, Double.POSITIVE_INFINITY, 3));
    }

    @Test
    public void pointsAndLimitsTest() {
        {
            var res = HumanVisibleMathFunctionBase.pointsAndLimits(new double[]{1, 2, 3, 3, 3, 4, 5, 5, 6, 6, 7}, 2, 5);
            assertArrayEquals(new double[]{2, 3, 4, 5}, res, 0);
        }
        {
            var res = HumanVisibleMathFunctionBase.pointsAndLimits(new double[]{-1, 2, 3, 3, 3, 4, 5, 5, 6, 6, 7}, -2, 15);
            assertArrayEquals(new double[]{-2, -1, 2, 3, 4, 5, 6, 7, 15}, res, 0);
        }
        {
            var res = HumanVisibleMathFunctionBase.pointsAndLimits(new double[]{3, 7, 6, 2, 1, 4, 5, 1, 2, 5, 6}, 2, 5);
            assertArrayEquals(new double[]{2, 3, 4, 5}, res, 0);
        }
        {
            var NaN = Double.NaN;
            var res = HumanVisibleMathFunctionBase.pointsAndLimits(new double[]{NaN, 3, 7, 6, 2, 1, NaN, 4, 5, 1, 2, 5, 6, NaN}, 2, 5);
            assertArrayEquals(new double[]{2, 3, 4, 5}, res, 0);
        }
    }

    @Test
    public void removeDuplicatesInSortedTest() {
        {
            var res = HumanVisibleMathFunctionBase.removeDuplicatesInSorted(new double[]{1, 2, 3, 4, 5, 6, 7});
            assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7}, res, 0);
        }
        {
            var res = HumanVisibleMathFunctionBase.removeDuplicatesInSorted(new double[]{1, 2, 3, 4, 5, 6, 7, 8}, 7);
            assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7}, res, 0);
        }
        {
            var res = HumanVisibleMathFunctionBase.removeDuplicatesInSorted(new double[]{1, 2, 3, 3, 3, 4, 5, 5, 6, 6, 7});
            assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7}, res, 0);
        }
        {
            var res = HumanVisibleMathFunctionBase.removeDuplicatesInSorted(new double[]{1, 1, 1, 1, 2, 3, 3, 3, 4, 5, 5, 6, 6, 7, 7, 7, 7});
            assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7}, res, 0);
        }
    }

    @Test
    public void findRootWhenSafeTest() {
        {
            var r = HumanVisibleMathFunctionBase.findRootWhenSafe(LinearPolynomial.of(1, -5).asFunction(), -1000, 3000);
            assertEquals(5, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
        {
            var r = HumanVisibleMathFunctionBase.findRootWhenSafe(LinearPolynomial.of(1, -5).asFunction(), -1000, 5);
            assertEquals(5, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
        {
            var r = HumanVisibleMathFunctionBase.findRootWhenSafe(LinearPolynomial.of(1, -5).asFunction(), 5, 3000);
            assertEquals(5, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
    }

    @Test
    public void haveRootsTest() {
        assertTrue(LinearPolynomial.of(1, -5).haveRoots(-1000, 3000));
        assertFalse(LinearPolynomial.of(1, -5).haveRoots(-1000, 3));
        assertTrue(LinearPolynomial.of(1, -5).haveRoots(-1000, 5));
        assertTrue(LinearPolynomial.of(1, -5).haveRoots(3, 5));
        assertTrue(LinearPolynomial.of(1, -5).haveRoots(7, 5));
    }

    @Test
    public void findRootsInTest() {
        {
            var r = LinearPolynomial.of(1, -5).findRootsIn(-1000, 3000);
            assertArrayEquals(new double[]{5}, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
        {
            var r = QuadraticPolynomial.of(1, -7, 10).findRootsIn(-1000, 3000);
            assertArrayEquals(new double[]{2, 5}, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
        {
            var r = CubicPolynomial.of(1, -10, 31, -30).findRootsIn(-1000, 3000);
            assertArrayEquals(new double[]{2, 3, 5}, r, HumanVisibleMathFunctionBase.ROOT_PREC);
        }
    }

    @Test
    public void pointsAroundIntervalsOfMonotonicityTest() {
        var q = QuadraticPolynomial.of(1, -2, 1); // (x-1)^2
        assertArrayEquals(new double[]{-100, 1, 100}, q.pointsAroundIntervalsOfMonotonicity(-100, 100), 0);
    }

    @Test
    public void findRootsIn_via_findRootsTest() {
        var r = TestPolynomial.of(Double.NaN, Double.NaN).findRootsIn(20, 50);
        assertArrayEquals(new double[]{20, 30, 50}, r, 0);
    }

    static class TestPolynomial extends LinearPolynomial {
        public TestPolynomial(double a, double b) {
            super(a, b);
        }

        public static TestPolynomial of(double a, double b) {
            return new TestPolynomial(a, b);
        }

        @Override
        public double[] findRoots() {
            return new double[]{10, 20, 30, 50, 100};
        }
    }

    @Test
    public void parseExpectTest() {
        HumanVisibleMathFunctionBase.parseExpect("PZ", "PZ");
        try {
            HumanVisibleMathFunctionBase.parseExpect("PX", "PZ");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
        try {
            HumanVisibleMathFunctionBase.parseExpect("aPZ", "PZ");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void parseSuffixTest() {
        var ss = Arrays.asList("foo","bar","baz");
        {
            var it = ss.iterator();
            it.next();
            try {
                HumanVisibleMathFunctionBase.parseSuffix(it);
                fail("IllegalArgumentException expected");
            } catch (IllegalArgumentException e) {}
        }
        {
            var it = ss.iterator();
            it.next();
            it.next();
            it.next();
            HumanVisibleMathFunctionBase.parseSuffix(it); // no exception
        }
    }

    @Test
    public void doFromParamStringTest() {
        assertEquals(
                Optional.empty(),
                HumanVisibleMathFunctionBase.doFromParamString(
                        "AB",
                        "CD",
                        i -> HumanVisibleMathFunction.NO_FUNCTION
                )
        );
        assertEquals(
                Optional.of(HumanVisibleMathFunction.NO_FUNCTION),
                HumanVisibleMathFunctionBase.doFromParamString(
                        "AB",
                        "AB",
                        i -> HumanVisibleMathFunction.NO_FUNCTION
                )
        );
        assertEquals(
                Optional.empty(),//of(HumanVisibleMathFunction.NO_FUNCTION),
                HumanVisibleMathFunctionBase.doFromParamString(
                        "AB 1",
                        "AB",
                        i -> HumanVisibleMathFunction.NO_FUNCTION
                )
        );
        assertEquals(
                Optional.of(HumanVisibleMathFunction.NO_FUNCTION),
                HumanVisibleMathFunctionBase.doFromParamString(
                        "AB 1",
                        "AB",
                        i -> { i.next(); return HumanVisibleMathFunction.NO_FUNCTION; }
                )
        );
    }

    @Test
    public void ifParamStringPrefixTest() {
        String [] holder = new String[] {""};
        var p = ConstantPolynomial.of(1);
        var res = HumanVisibleMathFunctionBase.ifParamStringPrefix(
                "foo",
                "foo bar baz qux",
                s -> {
                    holder[0] = s;
                    return Optional.of(p);
        });
        assertSame(p, res.get());
        assertEquals("bar baz qux", holder[0]);
    }

    @Test
    public void coverageTest() {
        TestCoverage.check(HumanVisibleMathFunctionBase.class, getClass());
    }

}

