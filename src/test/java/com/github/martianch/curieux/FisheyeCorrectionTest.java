package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class FisheyeCorrectionTest {

    @Test
    public void ofTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
    }

    @Test
    public void defaultValueTest() {
        var fc = FisheyeCorrection.defaultValue();
        assertTrue(Double.isNaN(fc.func.apply(1)));
        assertEquals(FisheyeCorrectionAlgo.NONE, fc.algo);
        assertEquals(DistortionCenterLocation.IN_CENTER_1X1, fc.distortionCenterLocation);
    }

    @Test
    public void withAlgoTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        FisheyeCorrection fc2 = fc.withAlgo(FisheyeCorrectionAlgo.NONE);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertOf(fc2, FisheyeCorrectionAlgo.NONE, p, DistortionCenterLocation.IN_CENTER_1X1);
    }

    @Test
    public void withFuncTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        LinearPolynomial q = LinearPolynomial.of(20, 10);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        FisheyeCorrection fc2 = fc.withFunc(q);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertOf(fc2, FisheyeCorrectionAlgo.UNFISH1, q, DistortionCenterLocation.IN_CENTER_1X1);
    }

    @Test
    public void withCenterTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        FisheyeCorrection fc2 = fc.withCenter(null);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertOf(fc2, FisheyeCorrectionAlgo.UNFISH1, p, null);
    }

//    @Test
//    public void doFisheyeCorrectionTest() {
//    }

    @Test
    public void parametersToStringTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertEquals("UNFISH1 IN_CENTER_1X1 P1 2.0 1.0", fc.parametersToString());
    }

    @Test
    public void fromParameterStringTest() {
        {
            String s = "UNFISH1 IN_CENTER_1X1 P2 5.886203652143178E-7 -0.0014907041790304925 1.4752316265370897";
            var fc = FisheyeCorrection.fromParameterString(s);
            assertNotNull(fc);
            assertEquals(s, fc.parametersToString());
        }
    }

    @Test
    public void toStringTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterLocation.IN_CENTER_1X1);
        assertEquals("FisheyeCorrection{" +
                "algo=UNFISH1," +
                " distortionCenterLocation=IN_CENTER_1X1," +
                " func=LinearPolynomial{a=2.0, b=1.0, 2.000*x + 1.000}}" +
                "@" + Integer.toHexString(fc.hashCode()),
                fc.toString());
    }

    void assertOf(FisheyeCorrection fc, FisheyeCorrectionAlgo algo, HumanVisibleMathFunction func, DistortionCenterLocation center) {
        assertEquals(algo, fc.algo);
        assertSame(func, fc.func);
        assertEquals(fc.distortionCenterLocation, center);
    }
}