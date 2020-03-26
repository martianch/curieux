package com.github.martianch.curieux;

import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

public class ImageAndPathTest {

    @Test
    public void isPathEqualTest() {
        BufferedImage dummy = ImageAndPath.dummyImage(Color.BLUE);
        BufferedImage nonDummy = ImageAndPath._dummyImage(Color.BLUE, ImageAndPath.DUMMY_SIZE+1, ImageAndPath.DUMMY_SIZE);
        ImageAndPath iap1 = new ImageAndPath(nonDummy,"/foo/bar");
        ImageAndPath iap2 = new ImageAndPath(dummy,"");
        ImageAndPath iap3 = new ImageAndPath(dummy,"...");
        assertTrue(iap1.isPathEqual("/foo/bar"));
        assertFalse(iap1.isPathEqual("/baz/qux"));
        assertFalse(iap1.isPathEqual(""));
        assertFalse(iap1.isPathEqual("..."));
        assertFalse(iap2.isPathEqual("/foo/bar"));
        assertFalse(iap2.isPathEqual("/baz/qux"));
        assertTrue(iap2.isPathEqual(""));
        assertFalse(iap2.isPathEqual("..."));
        assertFalse(iap3.isPathEqual("/foo/bar"));
        assertFalse(iap3.isPathEqual("/baz/qux"));
        assertFalse(iap3.isPathEqual(""));
        assertTrue(iap3.isPathEqual("..."));
    }

    @Test
    public void dummyImageTest() {
        BufferedImage image = ImageAndPath.dummyImage(Color.BLUE);
        assertTrue(image != null);
        assertTrue(image.getHeight() == ImageAndPath.DUMMY_SIZE);
        assertTrue(image.getWidth() == ImageAndPath.DUMMY_SIZE);
    }

    @Test
    public void isDummyImageTest() {
        {
            BufferedImage image = ImageAndPath.dummyImage(Color.BLUE);
            assertTrue(ImageAndPath.isDummyImage(image));
        }
        {
            BufferedImage image = ImageAndPath._dummyImage(Color.BLUE, ImageAndPath.DUMMY_SIZE+1,ImageAndPath.DUMMY_SIZE);
            assertFalse(ImageAndPath.isDummyImage(image));
        }
    }

    @Test
    public void isSpecialPathTest() {
        assertTrue(ImageAndPath.isSpecialPath(ImageAndPath.NO_PATH));
        assertTrue(ImageAndPath.isSpecialPath(ImageAndPath.ERROR_PATH));
        assertTrue(ImageAndPath.isSpecialPath(ImageAndPath.IN_PROGRESS_PATH));
        assertFalse(ImageAndPath.isSpecialPath("/foo/bar"));
        assertFalse(ImageAndPath.isSpecialPath("https://google.com/foo"));
    }
}