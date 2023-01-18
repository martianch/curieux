package com.github.martianch.curieux;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ScreenshotSaverTest {

    @Test
    public void testEndsWithIgnoreCase() {
        assertEquals(true, SaverBase.endsWithIgnoreCase("foo.png",".png"));
        assertEquals(true, SaverBase.endsWithIgnoreCase("foo.png",".PNG"));
        assertEquals(true, SaverBase.endsWithIgnoreCase("FOO.PNG",".png"));
        assertEquals(true, SaverBase.endsWithIgnoreCase("foo.Png",".pnG"));
        assertEquals(false, SaverBase.endsWithIgnoreCase("foo.xpng",".png"));
        assertEquals(false, SaverBase.endsWithIgnoreCase("foo.jpg",".png"));
    }

    @Test
    public void toLrTest() {
        String origPath = "foo/bar/baz/qux.png".replace('/', File.separatorChar);
        String resultPath = "foo/bar/baz/lr/qux.png".replace('/', File.separatorChar);
        File orig = new File(origPath);
        assertEquals(new File(resultPath), ScreenshotSaver.toLr(orig));
    }
}