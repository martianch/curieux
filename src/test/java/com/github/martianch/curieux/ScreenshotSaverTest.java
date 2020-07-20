package com.github.martianch.curieux;

import junit.framework.TestCase;

public class ScreenshotSaverTest extends TestCase {

    public void testEndsWithIgnoreCase() {
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.png",".png"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.png",".PNG"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("FOO.PNG",".png"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.Png",".pnG"));
        assertEquals(false, ScreenshotSaver.endsWithIgnoreCase("foo.xpng",".png"));
        assertEquals(false, ScreenshotSaver.endsWithIgnoreCase("foo.jpg",".png"));
    }
}