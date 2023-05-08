package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DoubleCalculatorTest {
    Map<String, Double> vars = new HashMap<>();
    {
        vars.put("W", 800.);
        vars.put("H", 600.);
    }
    @Test
    void parseDoubleTest() {
        assertEquals(12345, DoubleCalculator.parseDouble("12345", vars), 0);
        assertEquals(123.45, DoubleCalculator.parseDouble("123.45", vars), 0);
        assertEquals(6000, DoubleCalculator.parseDouble("10*H", vars));
        assertEquals(8000, DoubleCalculator.parseDouble("W*10", vars));
        assertEquals(1400, DoubleCalculator.parseDouble("H+W", vars));
    }
}