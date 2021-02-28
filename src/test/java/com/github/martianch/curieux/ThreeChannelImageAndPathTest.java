package com.github.martianch.curieux;

import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static com.github.martianch.curieux.ImageAndPath._dummyImage;
import static com.github.martianch.curieux.ImageAndPath.dummyImage;
import static org.junit.Assert.*;

public class ThreeChannelImageAndPathTest {

    @Test
    public void combinedWidthTest() {
        assertEquals(11,ThreeChannelImageAndPath.combinedWidth(11,11,11));
        assertEquals(20003,ThreeChannelImageAndPath.combinedWidth(20001,20002,20003));
        assertEquals(20004,ThreeChannelImageAndPath.combinedWidth(20004,20002,20003));
        assertEquals(20005,ThreeChannelImageAndPath.combinedWidth(20004,20005,20003));
    }
    @Test
    public void combinedHeightTest() {
        assertEquals(11,ThreeChannelImageAndPath.combinedHeight(11,11,11));
        assertEquals(20003,ThreeChannelImageAndPath.combinedHeight(20001,20002,20003));
        assertEquals(20004,ThreeChannelImageAndPath.combinedHeight(20004,20002,20003));
        assertEquals(20005,ThreeChannelImageAndPath.combinedHeight(20004,20005,20003));
    }
    @Test
    public void combineChannelsTestSize() {
        int w1=11, h1=12;
        BufferedImage r = _dummyImage(new Color(0x11_1111), w1, h1);
        BufferedImage g = _dummyImage(new Color(0x22_2222), w1, h1);
        BufferedImage b = _dummyImage(new Color(0x33_3333), w1, h1);
        SimpleImageAndPath rr = new SimpleImageAndPath(r,"r","rr");
        SimpleImageAndPath gg = new SimpleImageAndPath(g,"g","gg");
        SimpleImageAndPath bb = new SimpleImageAndPath(b,"b","bb");
        ThreeChannelImageAndPath cc0 = new ThreeChannelImageAndPath(rr,gg,bb, "a","aa");
        ImageAndPath cc1 = cc0.combineChannels();
        assertEquals(w1,cc1.image.getWidth());
        assertEquals(h1,cc1.image.getHeight());
    }
    @Test
    public void combineChannelsTestColor1() {
        int w1=11, h1=12;
        BufferedImage r = _dummyImage(new Color(0x11_1111), w1, h1);
        BufferedImage g = _dummyImage(new Color(0x22_2222), w1, h1);
        BufferedImage b = _dummyImage(new Color(0x33_3333), w1, h1);
        SimpleImageAndPath rr = new SimpleImageAndPath(r,"r","rr");
        SimpleImageAndPath gg = new SimpleImageAndPath(g,"g","gg");
        SimpleImageAndPath bb = new SimpleImageAndPath(b,"b","bb");
        ThreeChannelImageAndPath cc0 = new ThreeChannelImageAndPath(rr,gg,bb, "a","aa");
        ImageAndPath cc1 = cc0.combineChannels();
        for (int i = 0; i<w1; i++) {
            for (int j = 0; j<h1; j++) {
                assertEquals(0x112233, cc1.image.getRGB(i,j)&0xFF_FFFF);
            }
        }
    }
}