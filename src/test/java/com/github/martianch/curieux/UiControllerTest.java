package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiControllerTest {
    @Test
    public void test_z1z1() {
        DisplayParameters dp = new DisplayParameters();
        dp.zoomL=1.;
        dp.zoomR=1.;
        dp.offsetY = dp.offsetX = 0;
        DisplayParameters dps = new DisplayParameters();
        dps.zoomL=dp.zoomR;
        dps.zoomR=dp.zoomL;
        dps.offsetY = dps.offsetX = 0;
        PanelMeasurementStatus pl = new PanelMeasurementStatus();
        pl.x1 = 743;
        pl.y1 = 1173;
        pl.centeringDX = 0;
        pl.centeringDY = 0;
        PanelMeasurementStatus pr = new PanelMeasurementStatus();
        pr.x1 = 347;
        pr.y1 = 424;
        pr.centeringDX = 88;
        pr.centeringDY = 376;

        MeasurementStatus ms = new MeasurementStatus(pl, pr);
        UiController.doAdjustOffsets(1, dp, ms);
        assertEquals(-308, dp.offsetX);
        assertEquals(-373, dp.offsetY);

        MeasurementStatus mss = new MeasurementStatus(pr, pl); // swapped!
        UiController.doAdjustOffsets(1, dps, mss);
        assertEquals(308, dps.offsetX);
        assertEquals(373, dps.offsetY);
    }

    @Test
    public void test_z2z2() {
        DisplayParameters dp = new DisplayParameters();
        dp.zoomL=2.;
        dp.zoomR=2.;
        dp.offsetY = dp.offsetX = 0;
        DisplayParameters dps = new DisplayParameters();
        dps.zoomL=dp.zoomR;
        dps.zoomR=dp.zoomL;
        dps.offsetY = dps.offsetX = 0;
        PanelMeasurementStatus pl = new PanelMeasurementStatus();
        pl.x1 = 743;
        pl.y1 = 1173;
        pl.centeringDX = 0;
        pl.centeringDY = 0;
        PanelMeasurementStatus pr = new PanelMeasurementStatus();
        pr.x1 = 347;
        pr.y1 = 424;
        pr.centeringDX = 88;
        pr.centeringDY = 376;

        MeasurementStatus ms = new MeasurementStatus(pl, pr);
        UiController.doAdjustOffsets(1, dp, ms);
        assertEquals(-308, dp.offsetX);
        assertEquals(-373, dp.offsetY);

        MeasurementStatus mss = new MeasurementStatus(pr, pl); // swapped!
        UiController.doAdjustOffsets(1, dps, mss);
        assertEquals(308, dps.offsetX);
        assertEquals(373, dps.offsetY);
    }

    @Test
    public void test_z2z1() {
        DisplayParameters dp = new DisplayParameters();
        dp.zoomL=2.;
        dp.zoomR=1.;
        dp.offsetY = dp.offsetX = 0;
        DisplayParameters dps = new DisplayParameters();
        dps.zoomL=dp.zoomR;
        dps.zoomR=dp.zoomL;
        dps.offsetY = dps.offsetX = 0;
        PanelMeasurementStatus pl = new PanelMeasurementStatus();
        pl.x1 = 743;
        pl.y1 = 1173;
        pl.centeringDX = 0;
        pl.centeringDY = 0;
        PanelMeasurementStatus pr = new PanelMeasurementStatus();
        pr.x1 = 347;
        pr.y1 = 424;
        pr.centeringDX = 752;
        pr.centeringDY = 968;

        MeasurementStatus ms = new MeasurementStatus(pl, pr);
        UiController.doAdjustOffsets(1, dp, ms);
        assertEquals(-387, dp.offsetX);
        assertEquals(-954, dp.offsetY);

        MeasurementStatus mss = new MeasurementStatus(pr, pl); // swapped!
        UiController.doAdjustOffsets(1, dps, mss);
        assertEquals(387, dps.offsetX);
        assertEquals(954, dps.offsetY);
    }

    @Test
    public void test_z1z2() {
        DisplayParameters dp = new DisplayParameters();
        dp.zoomL=1.;
        dp.zoomR=2.;
        dp.offsetY = dp.offsetX = 0;
        DisplayParameters dps = new DisplayParameters();
        dps.zoomL=dp.zoomR;
        dps.zoomR=dp.zoomL;
        dps.offsetY = dps.offsetX = 0;
        PanelMeasurementStatus pl = new PanelMeasurementStatus();
        pl.x1 = 743;
        pl.y1 = 1173;
        pl.centeringDX = 488;
        pl.centeringDY = 0;
        PanelMeasurementStatus pr = new PanelMeasurementStatus();
        pr.x1 = 347;
        pr.y1 = 424;
        pr.centeringDX = 0;
        pr.centeringDY = 80;

        MeasurementStatus ms = new MeasurementStatus(pl, pr);
        UiController.doAdjustOffsets(1, dp, ms);
        assertEquals(-512, dp.offsetX);
        assertEquals(-82, dp.offsetY);

        MeasurementStatus mss = new MeasurementStatus(pr, pl); // swapped!
        UiController.doAdjustOffsets(1, dps, mss);
        assertEquals(512, dps.offsetX);
        assertEquals(82, dps.offsetY);
    }

    @Test
    public void test_z1z3() {
        DisplayParameters dp = new DisplayParameters();
        dp.zoomL=1.;
        dp.zoomR=3.;
        dp.offsetY = dp.offsetX = 0;
        DisplayParameters dps = new DisplayParameters();
        dps.zoomL=dp.zoomR;
        dps.zoomR=dp.zoomL;
        dps.offsetY = dps.offsetX = 0;
        PanelMeasurementStatus pl = new PanelMeasurementStatus();
        pl.x1 = 743;
        pl.y1 = 1173;
        pl.centeringDX = 1064;
        pl.centeringDY = 56;
        PanelMeasurementStatus pr = new PanelMeasurementStatus();
        pr.x1 = 347;
        pr.y1 = 424;
        pr.centeringDX = 0;
        pr.centeringDY = 0;

        MeasurementStatus ms = new MeasurementStatus(pl, pr);
        UiController.doAdjustOffsets(1, dp, ms);
        assertEquals(-766, dp.offsetX);
        assertEquals(43, dp.offsetY);

        MeasurementStatus mss = new MeasurementStatus(pr, pl); // swapped!
        UiController.doAdjustOffsets(1, dps, mss);
        assertEquals(766, dps.offsetX);
        assertEquals(-43, dps.offsetY);
    }

}