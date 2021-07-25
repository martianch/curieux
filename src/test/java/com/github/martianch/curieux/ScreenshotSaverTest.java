package com.github.martianch.curieux;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ScreenshotSaverTest {

    @Test
    public void testEndsWithIgnoreCase() {
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.png",".png"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.png",".PNG"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("FOO.PNG",".png"));
        assertEquals(true, ScreenshotSaver.endsWithIgnoreCase("foo.Png",".pnG"));
        assertEquals(false, ScreenshotSaver.endsWithIgnoreCase("foo.xpng",".png"));
        assertEquals(false, ScreenshotSaver.endsWithIgnoreCase("foo.jpg",".png"));
    }

    @Test
    public void toLrTest() {
        String origPath = "foo/bar/baz/qux.png".replace('/', File.separatorChar);
        String resultPath = "foo/bar/baz/lr/qux.png".replace('/', File.separatorChar);
        File orig = new File(origPath);
        assertEquals(new File(resultPath), ScreenshotSaver.toLr(orig));
    }
}