package com.github.martianch.curieux;

import org.junit.Test;

import static com.github.martianch.curieux.MeasurementPanel.dist3d;
import static org.junit.Assert.*;

public class MeasurementPanelTest {

    @Test
    public void sqrTest() {
        assertEquals(4.0,MeasurementPanel.sqr(2.0),0.0000001);
        assertEquals(2.0,MeasurementPanel.sqr(Math.sqrt(2.0)),0.0000001);
    }

    // uncomment the next line to run this test
    //@Test
    public void calibrate7Test() {
        StereoPairParameters spp =
                new CustomStereoPairParameters("",0,0,0,3.7E3)
                        .assignFrom(StereoPairParameters.MastCam);
        MeasurementStatus ms = new MeasurementStatus();
        //https://mars.nasa.gov/msl-raw-images/msss/02950/mcam/2950ML0153990041103780C00_DXXX.jpg
        //https://mars.nasa.gov/msl-raw-images/msss/02950/mcam/2950MR0153990041302126C00_DXXX.jpg
        //1: 137 219  213 576
        //2: 482 217  1200 586
        //3: 393 141  948 360
        ms.left.x1 = 137;
        ms.left.y1 = 219;
        ms.left.x2 = 482;
        ms.left.y2 = 217;
        ms.left.x3 = 393;
        ms.left.y3 = 141;
        ms.left.w = 1328;
        ms.left.h = 1184;
        ms.left.ifov = spp.ifovL;
        ms.right.x1 = 213;
        ms.right.y1 = 576;
        ms.right.x2 = 1200;
        ms.right.y2 = 586;
        ms.right.x3 = 948;
        ms.right.y3 = 360;
        ms.right.w = 1152;
        ms.right.h = 432;
        ms.right.ifov = spp.ifovR;
        ms.stereoPairParameters = spp;
        var correction = MeasurementPanel.calibrate7(ms.right, ms.left, spp);
        System.out.println(correction);
//        System.out.println(spp.toStringDetailed());
    }


}