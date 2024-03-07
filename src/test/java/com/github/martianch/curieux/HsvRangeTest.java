package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HsvRangeTest {
    @Test
    void updateTest() {
        {
            var h = HsvRange.newEmptyRangeFrom(0);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hLower, 0.f, 0.f);
            assertEquals(0.3f, h.minH, 1e-4f);
            assertEquals(0.9f, h.maxH, 1e-4f);
        }
        {
            var h = HsvRange.newEmptyRangeFrom(90/360.);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hLower, 0.25f, 0.f);
            assertEquals(0.3f, h.minH, 1e-4f);
            assertEquals(0.9f, h.maxH, 1e-4f);
        }
        {
            var h = HsvRange.newEmptyRangeFrom(180/360.);
            h.update(new float[]{0.9f, 1f, 1f});
            h.update(new float[]{0.3f, 1f, 1f});
            assertEquals(h.hLower, 0.5f, 0.f);
            assertEquals(0.9f, h.minH, 1e-4f);
            assertEquals(1.3f, h.maxH, 1e-4f);
        }
    }
}