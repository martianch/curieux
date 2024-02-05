package com.github.martianch.curieux;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DebayerBicubicTest {
    static final boolean WRITE_IMAGES = false;

    @BeforeClass
    public static void setUp() throws Exception {
        Par.init();
    }
    @AfterClass
    public static void tearDown() throws Exception {
        Par.Configurator.shutdownPar();
    }

    @Test
    public void sameColorTest() throws Exception {
        var red0 = imageOfSameColor(0x700000);
        var green0 = imageOfSameColor(0x7000);
        var blue0 = imageOfSameColor(0x70);
        var red1 = bayered(red0);
        var green1 = bayered(green0);
        var blue1 = bayered(blue0);
        var red2 = DebayerBicubic.debayer_bicubic(red1);
        var green2 = DebayerBicubic.debayer_bicubic(green1);
        var blue2 = DebayerBicubic.debayer_bicubic(blue1);
        if (WRITE_IMAGES) {
            new File("testout/").mkdirs();
            ScreenshotSaver.writePng(new File("testout/cred0.png"), red0);
            ScreenshotSaver.writePng(new File("testout/cgreen0.png"), green0);
            ScreenshotSaver.writePng(new File("testout/cblue0.png"), blue0);
            ScreenshotSaver.writePng(new File("testout/cred1.png"), red1);
            ScreenshotSaver.writePng(new File("testout/cgreen1.png"), green1);
            ScreenshotSaver.writePng(new File("testout/cblue1.png"), blue1);
            ScreenshotSaver.writePng(new File("testout/cred2.png"), red2);
            ScreenshotSaver.writePng(new File("testout/cgreen2.png"), green2);
            ScreenshotSaver.writePng(new File("testout/cblue2.png"), blue2);
        }
        assertTrue(imagesCompare(4,1,red0, red2));
        assertTrue(imagesCompare(4,1,green0, green2));
        assertTrue(imagesCompare(4,1,blue0, blue2));
    }

    @Test
    public void hGradientTest() throws Exception {
        var red0 = imageWithHGradient(0x100000, 0xF00000);
        var green0 = imageWithHGradient(0x1000, 0xF000);
        var blue0 = imageWithHGradient(0x10, 0xF0);
        var red1 = bayered(red0);
        var green1 = bayered(green0);
        var blue1 = bayered(blue0);
        var red2 = DebayerBicubic.debayer_bicubic(red1);
        var green2 = DebayerBicubic.debayer_bicubic(green1);
        var blue2 = DebayerBicubic.debayer_bicubic(blue1);
        if (WRITE_IMAGES) {
            new File("testout/").mkdirs();
            ScreenshotSaver.writePng(new File("testout/hred0.png"), red0);
            ScreenshotSaver.writePng(new File("testout/hgreen0.png"), green0);
            ScreenshotSaver.writePng(new File("testout/hblue0.png"), blue0);
            ScreenshotSaver.writePng(new File("testout/hred1.png"), red1);
            ScreenshotSaver.writePng(new File("testout/hgreen1.png"), green1);
            ScreenshotSaver.writePng(new File("testout/hblue1.png"), blue1);
            ScreenshotSaver.writePng(new File("testout/hred2.png"), red2);
            ScreenshotSaver.writePng(new File("testout/hgreen2.png"), green2);
            ScreenshotSaver.writePng(new File("testout/hblue2.png"), blue2);
        }
        assertTrue(imagesCompare(4,1,red0, red2));
        assertTrue(imagesCompare(4,1,green0, green2));
        assertTrue(imagesCompare(4,1,blue0, blue2));
    }

    @Test
    public void vGradientTest() throws Exception {
        var red0 = imageWithVGradient(0x100000, 0xF00000);
        var green0 = imageWithVGradient(0x1000, 0xF000);
        var blue0 = imageWithVGradient(0x10, 0xF0);
        var red1 = bayered(red0);
        var green1 = bayered(green0);
        var blue1 = bayered(blue0);
        var red2 = DebayerBicubic.debayer_bicubic(red1);
        var green2 = DebayerBicubic.debayer_bicubic(green1);
        var blue2 = DebayerBicubic.debayer_bicubic(blue1);
        if (WRITE_IMAGES) {
            new File("testout/").mkdirs();
            ScreenshotSaver.writePng(new File("testout/vred0.png"), red0);
            ScreenshotSaver.writePng(new File("testout/vgreen0.png"), green0);
            ScreenshotSaver.writePng(new File("testout/vblue0.png"), blue0);
            ScreenshotSaver.writePng(new File("testout/vred1.png"), red1);
            ScreenshotSaver.writePng(new File("testout/vgreen1.png"), green1);
            ScreenshotSaver.writePng(new File("testout/vblue1.png"), blue1);
            ScreenshotSaver.writePng(new File("testout/vred2.png"), red2);
            ScreenshotSaver.writePng(new File("testout/vgreen2.png"), green2);
            ScreenshotSaver.writePng(new File("testout/vblue2.png"), blue2);
        }
        assertTrue(imagesCompare(4,1,red0, red2));
        assertTrue(imagesCompare(4,1,green0, green2));
        assertTrue(imagesCompare(4,1,blue0, blue2));
    }

    @Test
    public void btGradientTest() throws Exception {
        var red0 = imageWithBtGradient(0x100000, 0xF00000);
        var green0 = imageWithBtGradient(0x1000, 0xF000);
        var blue0 = imageWithBtGradient(0x10, 0xF0);
        var red1 = bayered(red0);
        var green1 = bayered(green0);
        var blue1 = bayered(blue0);
        var red2 = DebayerBicubic.debayer_bicubic(red1);
        var green2 = DebayerBicubic.debayer_bicubic(green1);
        var blue2 = DebayerBicubic.debayer_bicubic(blue1);
        if (WRITE_IMAGES) {
            new File("testout/").mkdirs();
            ScreenshotSaver.writePng(new File("testout/zred0.png"), red0);
            ScreenshotSaver.writePng(new File("testout/zgreen0.png"), green0);
            ScreenshotSaver.writePng(new File("testout/zblue0.png"), blue0);
            ScreenshotSaver.writePng(new File("testout/zred1.png"), red1);
            ScreenshotSaver.writePng(new File("testout/zgreen1.png"), green1);
            ScreenshotSaver.writePng(new File("testout/zblue1.png"), blue1);
            ScreenshotSaver.writePng(new File("testout/zred2.png"), red2);
            ScreenshotSaver.writePng(new File("testout/zgreen2.png"), green2);
            ScreenshotSaver.writePng(new File("testout/zblue2.png"), blue2);
        }
        assertTrue(imagesCompare(4,1,red0, red2));
        assertTrue(imagesCompare(4,1,green0, green2));
        assertTrue(imagesCompare(4,1,blue0, blue2));
    }

    @Test
    public void tbGradientTest() throws Exception {
        var red0 = imageWithTbGradient(0x100000, 0xF00000);
        var green0 = imageWithTbGradient(0x1000, 0xF000);
        var blue0 = imageWithTbGradient(0x10, 0xF0);
        var red1 = bayered(red0);
        var green1 = bayered(green0);
        var blue1 = bayered(blue0);
        var red2 = DebayerBicubic.debayer_bicubic(red1);
        var green2 = DebayerBicubic.debayer_bicubic(green1);
        var blue2 = DebayerBicubic.debayer_bicubic(blue1);
        if (WRITE_IMAGES) {
            new File("testout/").mkdirs();
            ScreenshotSaver.writePng(new File("testout/sred0.png"), red0);
            ScreenshotSaver.writePng(new File("testout/sgreen0.png"), green0);
            ScreenshotSaver.writePng(new File("testout/sblue0.png"), blue0);
            ScreenshotSaver.writePng(new File("testout/sred1.png"), red1);
            ScreenshotSaver.writePng(new File("testout/sgreen1.png"), green1);
            ScreenshotSaver.writePng(new File("testout/sblue1.png"), blue1);
            ScreenshotSaver.writePng(new File("testout/sred2.png"), red2);
            ScreenshotSaver.writePng(new File("testout/sgreen2.png"), green2);
            ScreenshotSaver.writePng(new File("testout/sblue2.png"), blue2);
        }
        assertTrue(imagesCompare(4,1,red0, red2));
        assertTrue(imagesCompare(4,1,green0, green2));
        assertTrue(imagesCompare(4,1,blue0, blue2));
    }


    BufferedImage imageOfSameColor(int rgb) {
        int width = 64;
        int height = 64;
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                res.setRGB(i,j,rgb);
            }
        }
        return res;
    }
    BufferedImage imageWithHGradient(int rgb0, int rgb1) {
        int width = 64;
        int height = 64;
        int r0 = r(rgb0), r1 = r(rgb1);
        int g0 = g(rgb0), g1 = g(rgb1);
        int b0 = b(rgb0), b1 = b(rgb1);
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                int rgb = rgb(
                        r0 + (r1-r0)*i/width,
                        g0 + (g1-g0)*i/width,
                        b0 + (b1-b0)*i/width
                );
                res.setRGB(i,j,rgb);
            }
        }
        return res;
    }
    BufferedImage imageWithVGradient(int rgb0, int rgb1) {
        int width = 64;
        int height = 64;
        int r0 = r(rgb0), r1 = r(rgb1);
        int g0 = g(rgb0), g1 = g(rgb1);
        int b0 = b(rgb0), b1 = b(rgb1);
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                int rgb = rgb(
                        r0 + (r1-r0)*j/height,
                        g0 + (g1-g0)*j/height,
                        b0 + (b1-b0)*j/height
                );
                res.setRGB(i,j,rgb);
            }
        }
        return res;
    }
    BufferedImage imageWithTbGradient(int rgb0, int rgb1) {
        int width = 64;
        int height = 64;
        int r0 = r(rgb0), r1 = r(rgb1);
        int g0 = g(rgb0), g1 = g(rgb1);
        int b0 = b(rgb0), b1 = b(rgb1);
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                int rgb = rgb(
                        r0 + (r1-r0)*(i+j)/(width+height),
                        g0 + (g1-g0)*(i+j)/(width+height),
                        b0 + (b1-b0)*(i+j)/(width+height)
                );
                res.setRGB(i,j,rgb);
            }
        }
        return res;
    }
    BufferedImage imageWithBtGradient(int rgb0, int rgb1) {
        int width = 64;
        int height = 64;
        int r0 = r(rgb0), r1 = r(rgb1);
        int g0 = g(rgb0), g1 = g(rgb1);
        int b0 = b(rgb0), b1 = b(rgb1);
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                int rgb = rgb(
                        r0 + (r1-r0)*(width-1-i+j)/(width+height),
                        g0 + (g1-g0)*(width-1-i+j)/(width+height),
                        b0 + (b1-b0)*(width-1-i+j)/(width+height)
                );
                res.setRGB(i,j,rgb);
            }
        }
        return res;
    }
    BufferedImage bayered(BufferedImage orig) {
        int width = orig.getWidth();
        int height = orig.getHeight();
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int j=0; j<height; j++) {
            for (int i=0; i<width; i++) {
                int type = (j&1)*2 + (i&1); // RGGB
                int c=0;
                switch (type) {
                    case 0: { // R
                        c = r(orig.getRGB(i,j));
                    } break;
                    case 1:   // Gr
                    case 2: { // Gb
                        c = g(orig.getRGB(i,j));
                    } break;
                    case 3: { // B
                        c = b(orig.getRGB(i,j));
                    } break;
//                    default: // stupid Java, this is impossible! type is 0..3!
                }
                res.setRGB(i,j,(c<<16)|(c<<8)|c);
            }
        }
        return res;
    }
    static int r(int argb) {
        return (argb>>16) & 0xff;
    }
    static int g(int argb) {
        return (argb>>8) & 0xff;
    }
    static int b(int argb) {
        return argb & 0xff;
    }
    static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | (b);
    }

    boolean imagesCompare(int margin, int d, BufferedImage first, BufferedImage second) {
        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(first.getHeight(), second.getHeight());
        int w = first.getWidth();
        int h = first.getHeight();
        boolean res = true;
        for (int j=0; j<h; j++) {
            for (int i=0; i<w; i++) {
                int rgb1 = first.getRGB(i,j);
                int rgb2 = first.getRGB(i,j);
                if (Math.abs(r(rgb1)-r(rgb2))>d
                 || Math.abs(g(rgb1)-g(rgb2))>d
                 || Math.abs(b(rgb1)-b(rgb2))>d
                ) {
                    res = false;
                    System.out.println(String.format("mismatch at (%d,%d): %06x vs %06x", i, j, rgb1, rgb2));
                }
            }
        }
        return res;
    }
}