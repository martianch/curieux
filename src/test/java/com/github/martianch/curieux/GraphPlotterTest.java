package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class GraphPlotterTest {

//    @Test
//    public void plotGraph() {
//    }

    @Test
    public void deltaYBetweenLinesTest() {
        assertEquals(.1, GraphPlotter.deltaYBetweenLines(.3), 0);
        assertEquals(.2, GraphPlotter.deltaYBetweenLines(1), 0);
        assertEquals(1, GraphPlotter.deltaYBetweenLines(1.1), 0);
        assertEquals(1, GraphPlotter.deltaYBetweenLines(3.5), 0);
        assertEquals(2, GraphPlotter.deltaYBetweenLines(9), 0);
        assertEquals(2, GraphPlotter.deltaYBetweenLines(10), 0);
        assertEquals(10, GraphPlotter.deltaYBetweenLines(11), 0);

        assertEquals(10, GraphPlotter.deltaYBetweenLines(30), 0);
        assertEquals(20, GraphPlotter.deltaYBetweenLines(90), 0);
        assertEquals(20, GraphPlotter.deltaYBetweenLines(100), 0);
        assertEquals(100, GraphPlotter.deltaYBetweenLines(110), 0);
        assertEquals(.1, GraphPlotter.deltaYBetweenLines(.3), 0);
        assertEquals(.2, GraphPlotter.deltaYBetweenLines(.9), 0);
        assertEquals(.01, GraphPlotter.deltaYBetweenLines(.03), 0);
        assertEquals(.02, GraphPlotter.deltaYBetweenLines(.10), 0);
        assertEquals(.1, GraphPlotter.deltaYBetweenLines(.11), 0);
        assertEquals(.02, GraphPlotter.deltaYBetweenLines(.09), 0);
    }
    @Test
    public void graphMaxYTest() {
        assertEquals(1, GraphPlotter.graphMaxY(1), 0);
        assertEquals(2, GraphPlotter.graphMaxY(1.1), 0);
        assertEquals(2, GraphPlotter.graphMaxY(2), 0);
        assertEquals(3, GraphPlotter.graphMaxY(2.1), 0);
        assertEquals(10, GraphPlotter.graphMaxY(9.1), 0);
        assertEquals(10, GraphPlotter.graphMaxY(10), 0);
        assertEquals(20, GraphPlotter.graphMaxY(10.1), 0);
        assertEquals(20, GraphPlotter.graphMaxY(12), 0);

        assertEquals(1, GraphPlotter.graphMaxY(1), 0);
        assertEquals(10, GraphPlotter.graphMaxY(10), 0);
    }
}