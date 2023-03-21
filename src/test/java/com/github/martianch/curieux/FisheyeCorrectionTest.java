package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class FisheyeCorrectionTest {
    static final DistortionCenterLocation CENTER_HV = DistortionCenterLocation.of(DistortionCenterStationing.CENTER, DistortionCenterStationing.TOP_EDGE);

    @Test
    public void ofTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
    }

    @Test
    public void defaultValueTest() {
        var fc = FisheyeCorrection.defaultValue();
        assertTrue(Double.isNaN(fc.func.apply(1)));
        assertEquals(FisheyeCorrectionAlgo.NONE, fc.algo);
        assertEquals(DistortionCenterStationing.CENTER, fc.distortionCenterLocation.getH());
        assertEquals(DistortionCenterStationing.CENTER, fc.distortionCenterLocation.getV());
    }

    @Test
    public void withAlgoTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        FisheyeCorrection fc2 = fc.withAlgo(FisheyeCorrectionAlgo.NONE);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        assertOf(fc2, FisheyeCorrectionAlgo.NONE, p, CENTER_HV, 3.);
    }

    @Test
    public void withFuncTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        LinearPolynomial q = LinearPolynomial.of(20, 10);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        FisheyeCorrection fc2 = fc.withFunc(q);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        assertOf(fc2, FisheyeCorrectionAlgo.UNFISH1, q, CENTER_HV, 3.);
    }

    @Test
    public void withCenterTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        FisheyeCorrection fc2 = fc.withCenterH(DistortionCenterStationing.LEFT_EDGE).withCenterV(DistortionCenterStationing.BOTTOM_EDGE);
        assertOf(fc, FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        assertOf(fc2, FisheyeCorrectionAlgo.UNFISH1, p, DistortionCenterStationing.LEFT_EDGE, DistortionCenterStationing.BOTTOM_EDGE, 3.);
    }

//    @Test
//    public void doFisheyeCorrectionTest() {
//    }

    @Test
    public void parametersToStringTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 4.);
        assertEquals("UNFISH1 CENTER TOP_EDGE P1 2.0 1.0 : 4.0 # ", fc.parametersToString());
    }

    @Test
    public void fromParameterStringTest() {
        {
            String s = "UNFISH1 CENTER TOP_EDGE P2 5.886203652143178E-7 -0.0014907041790304925 1.4752316265370897 : 4.0 # ";
            var fc = FisheyeCorrection.fromParameterString(s);
            assertNotNull(fc);
            assertEquals(s, fc.parametersToString());
        }
    }

    @Test
    public void toStringTest() {
        LinearPolynomial p = LinearPolynomial.of(2, 1);
        FisheyeCorrection fc = FisheyeCorrection.of(FisheyeCorrectionAlgo.UNFISH1, p, CENTER_HV, 3.);
        assertEquals("FisheyeCorrection{" +
                "algo=UNFISH1," +
                " distortionCenterLocation=(CENTER, TOP_EDGE)," +
                " func=LinearPolynomial{a=2.0, b=1.0, 2.000*x + 1.000}," +
                " sizeChange=3.0}" +
                "@" + Integer.toHexString(fc.hashCode()),
                fc.toString());
    }

    void assertOf(FisheyeCorrection fc, FisheyeCorrectionAlgo algo, HumanVisibleMathFunction func, DistortionCenterLocation center, double sizeChange) {
        assertEquals(algo, fc.algo);
        assertSame(func, fc.func);
        assertEquals(fc.distortionCenterLocation, center);
        assertEquals(fc.sizeChange, sizeChange, 0);
    }

    void assertOf(FisheyeCorrection fc, FisheyeCorrectionAlgo algo, HumanVisibleMathFunction func, DistortionCenterStationing centerH, DistortionCenterStationing centerV, double sizeChange) {
        assertEquals(algo, fc.algo);
        assertSame(func, fc.func);
        assertEquals(fc.distortionCenterLocation.getH(), centerH);
        assertEquals(fc.distortionCenterLocation.getV(), centerV);
        assertEquals(fc.sizeChange, sizeChange, 0);
    }
}