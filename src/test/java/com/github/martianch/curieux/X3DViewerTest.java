package com.github.martianch.curieux;

import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

public class X3DViewerTest {
    @Test
    public void testZoom_sizes() {
        Color red = new Color(255, 0, 0);
        Color green = new Color(0, 255, 0);
        BufferedImage image1 = ImageAndPath._dummyImage(red, 20, 20);
        BufferedImage image2 = ImageAndPath._dummyImage(green, 60, 60);
        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 1., 0, 0);
            var z2 = X3DViewer.zoom(image2, 1., image1, 1., 0, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 4., image2, 1., 0, 0);
            var z2 = X3DViewer.zoom(image2, 1., image1, 4., 0, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 2., 0, 0);
            var z2 = X3DViewer.zoom(image2, 2., image1, 1., 0, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }

        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 1., 10, 0);
            var z2 = X3DViewer.zoom(image2, 1., image1, 1., -10, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 4., image2, 1., 10, 0);
            var z2 = X3DViewer.zoom(image2, 1., image1, 4., -10, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 2., 10, 0);
            var z2 = X3DViewer.zoom(image2, 2., image1, 1., -10, 0);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }

        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 1., 0, 30);
            var z2 = X3DViewer.zoom(image2, 1., image1, 1., 0, -30);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 4., image2, 1., 0, 30);
            var z2 = X3DViewer.zoom(image2, 1., image1, 4., 0, -30);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
        {
            var z1 = X3DViewer.zoom(image1, 1., image2, 2., 0, 30);
            var z2 = X3DViewer.zoom(image2, 2., image1, 1., 0, -30);
            assertEquals(z1.getWidth(), z2.getWidth());
            assertEquals(z1.getHeight(), z2.getHeight());
        }
    }

}