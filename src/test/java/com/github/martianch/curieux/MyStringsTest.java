package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyStringsTest {

    @Test
    public void testEndsWithIgnoreCase() {
        assertEquals(true, MyStrings.endsWithIgnoreCase("foo.png",".png"));
        assertEquals(true, MyStrings.endsWithIgnoreCase("foo.png",".PNG"));
        assertEquals(true, MyStrings.endsWithIgnoreCase("FOO.PNG",".png"));
        assertEquals(true, MyStrings.endsWithIgnoreCase("foo.Png",".pnG"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("foo.xpng",".png"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("foo.jpg",".png"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("",".png"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("g",".png"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("f.jpg",".jpppg"));
        assertEquals(false, MyStrings.endsWithIgnoreCase("f.jpg",".jppg"));
    }

}