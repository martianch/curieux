package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

class RetangentWithFuncOfAnglePlusCTest {
    Map<String, Double> vars = new HashMap<>();
    @Test
    void ofTest() {
        var p = RetangentWithFuncOfAnglePlusC.of(1, 2, 3, 4, LinearPolynomial.of(5, 6));
        assertOf(p, 1, 2, 3, 4, LinearPolynomial.of(5, 6));
        assertTrue(p instanceof RetangentWithFuncOfAnglePlusC);
        assertFalse(p instanceof RetangentWithFuncOfAnglePlusC.RetangentWithFuncOfAnglePlusC0);
    }

    @Test
    void ofTest2() {
        var p = RetangentWithFuncOfAnglePlusC.of(1, 2, 3, 0, LinearPolynomial.of(5, 6));
        assertOf(p, 1, 2, 3, 0, LinearPolynomial.of(5, 6));
        assertTrue(p instanceof RetangentWithFuncOfAnglePlusC.RetangentWithFuncOfAnglePlusC0);
    }

    @Test
    void fromParamStringTest() {
        {
            var p = RetangentWithFuncOfAnglePlusC.fromParamString("RETANF 1.0 2.0 3.0 4.0 P1 5.0 6.0", vars);
            assertOf((RetangentWithFuncOfAnglePlusC) p.get(), 1, 2, 3, 4, LinearPolynomial.of(5, 6));
        }
        {
            var p = RetangentWithFuncOfAnglePlusC.fromParamString("RETANF 1.0 2.0 3.0 0 P1 5.0 6.0", vars);
            assertOf((RetangentWithFuncOfAnglePlusC) p.get(), 1, 2, 3, 0, LinearPolynomial.of(5, 6));
        }
        {
            var p = RetangentWithFuncOfAnglePlusC.fromParamString("RETANF 1+2 2*3 3/2 4^3 P1 1+2*5^2 6.0", vars);
            assertOf((RetangentWithFuncOfAnglePlusC) p.get(), 3, 6, 1.5, 64, LinearPolynomial.of(51, 6));
        }
    }

    @Test
    void applyTest() {
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(1,p.apply(0), 0);
            assertEquals(0.9342,p.apply(1), 1e-3);
            assertEquals(0.4025,p.apply(10), 1e-3);
        }
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, 10, 0, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(10,p.apply(0), 0);
            assertEquals(9.342,p.apply(1), 1e-3);
            assertEquals(4.025,p.apply(10), 1e-3);
        }
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, 10, 4, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(14,p.apply(0), 0);
            assertEquals(4+9.342,p.apply(1), 1e-3);
            assertEquals(4+4.025,p.apply(10), 1e-3);
        }
    }

    @Test
    void applyMulXTest() {
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(0,p.applyMulX(0), 0);
            assertEquals(0.9342,p.applyMulX(1), 1e-3);
            assertEquals(4.025,p.applyMulX(10), 1e-3);
        }
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, 10, 0, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(0,p.applyMulX(0), 0);
            assertEquals(9.342,p.applyMulX(1), 1e-3);
            assertEquals(40.25,p.applyMulX(10), 1e-2);
        }
        {
            RetangentWithFuncOfAnglePlusC p = RetangentWithFuncOfAnglePlusC.of(0.9, 2, 10, 4, QuadraticPolynomial.of(-0.1, 1, 0));
            assertEquals(0,p.applyMulX(0), 0);
            assertEquals(4+9.342,p.applyMulX(1), 1e-3);
            assertEquals(40+40.25,p.applyMulX(10), 1e-2);
        }
    }
//    @Test
//    public void asFunctionTest() {
//
//    }
//    @Test
//    public void asFunctionMulXTest() {
//    }

    @Test
    void asStringTest() {
        assertEquals(
                "(-0.3000*0.2000/0.06000)*tan(0.06000*f(atan(x/0.2000))) + 0.4000, where f(y) = -0.1000*y^2 + 1.000*y + 0.000",
                RetangentWithFuncOfAnglePlusC.of(.06, .2, -.3, .4, QuadraticPolynomial.of(-0.1, 1, 0)).asString()
        );
        assertEquals(
                "(-0.3000*0.2000/0.06000)*tan(0.06000*f(atan(r/0.2000))) + 0.4000, where f(y) = -0.1000*y^2 + 1.000*y + 0.000",
                RetangentWithFuncOfAnglePlusC.of(.06, .2, -.3, .4, QuadraticPolynomial.of(-0.1, 1, 0)).asString("r")
        );
    }

    @Test
    void parameterStringTest() {
        {
            var p = RetangentWithFuncOfAnglePlusC.of(1, 2, 3, 4, LinearPolynomial.of(5, 6));
            assertEquals("RETANF 1.0 2.0 3.0 4.0 P1 5.0 6.0", p.parameterString());
        }
        {
            var p = RetangentWithFuncOfAnglePlusC.of(1, 2, 3, 0, LinearPolynomial.of(5, 6));
            assertEquals("RETANF 1.0 2.0 3.0 0.0 P1 5.0 6.0", p.parameterString());
        }
    }

    @Test
    void maxInRangeTest() {
    }

    @Test
    void minInRangeTest() {
    }

    @Test
    void pointsAroundIntervalsOfMonotonicityTest() {
    }

    @Test
    void mulTest() {
    }

    @Test
    void subTest() {
    }

    @Test
    void addTest() {
    }

    @Test
    void derivativeTest() {
    }

    @Test
    void toStringTest() {
        assertEquals(
                "RetangentWithFuncOfAnglePlusC" +
                        "{k=0.06," +
                        " q=0.2," +
                        " a=-0.3," +
                        " c=0.4," +
                        " a*q/k=-1.0," +
                        " f=QuadraticPolynomial{a=-0.1, b=1.0, c=0.0, -0.1000*x^2 + 1.000*x + 0.000}," +
                        " a*df/dx=-0.3," +
                        " (-0.3000*0.2000/0.06000)*tan(0.06000*f(atan(x/0.2000))) + 0.4000," +
                        " where f(y) = -0.1000*y^2 + 1.000*y + 0.000" +
                        "}",
                RetangentWithFuncOfAnglePlusC.of(.06, .2, -.3, .4,
                        QuadraticPolynomial.of(-0.1, 1, 0)
                ).toString()
        );
    }

    static void assertOf(RetangentWithFuncOfAnglePlusC p, double k, double q, double a, double c, HumanVisibleMathFunction f) {
        assertEquals(k, p.k, 0);
        assertEquals(q, p.q, 0);
        assertEquals(a, p.a, 0);
        assertEquals(c, p.c, 0);
        assertEquals(f.parameterString(), p.f.parameterString());
    }
}