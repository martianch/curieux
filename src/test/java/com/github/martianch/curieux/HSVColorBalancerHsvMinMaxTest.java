package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HSVColorBalancerHsvMinMaxTest {
    @Test
    void updateTest() {
        {
            var h = new HSVColorBalancer.HsvMinMax(0);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hAround, 0.f, 0.f);
            assertEquals(0.3f, h.minH, 1e-4f);
            assertEquals(0.9f, h.maxH, 1e-4f);
        }
        {
            var h = new HSVColorBalancer.HsvMinMax(90);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hAround, 0.25f, 0.f);
            assertEquals(0.05f, h.minH, 1e-4f);
            assertEquals(0.65f, h.maxH, 1e-4f);
        }
        {
            var h = new HSVColorBalancer.HsvMinMax(180);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hAround, 0.5f, 0.f);
            assertEquals(0.4f, h.minH, 1e-4f);
            assertEquals(0.8f, h.maxH, 1e-4f);
        }
    }
    @Test
    void scaleHTest() {
        var h = new HSVColorBalancer.HsvMinMax(0);
        h.update(new float[]{0.9f, 1f, 1f});
        h.update(new float[]{0.4f, 1f, 1f});
        var d = h.maxH - h.minH;
        assertEquals(0.5f,d,1e-5f);
        assertEquals(0.49f, h.scaleH(0.89f, 1f), 1e-4f); // dh=1, subtract minH
        assertEquals(0.0f, h.scaleH(0.4f, 1f), 1e-4f); // dh=1, subtract minH
        assertEquals(0.f, h.scaleH(0.4f, d), 1e-4f);
        assertEquals(0.98f, h.scaleH(0.89f, d), 1e-4f);
    }
}