package com.github.martianch.curieux;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ScreenshotSaverTest {
    @Test
    public void toLrTest() {
        String origPath = "foo/bar/baz/qux.png".replace('/', File.separatorChar);
        String resultPath = "foo/bar/baz/lr/qux.png".replace('/', File.separatorChar);
        File orig = new File(origPath);
        assertEquals(new File(resultPath), ScreenshotSaver.toLr(orig));
    }
}