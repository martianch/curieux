package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static com.github.martianch.curieux.DebayerBicubic.debayer_bicubic;
import static com.github.martianch.curieux.ImageAndPath.imageIoRead;
import static org.junit.jupiter.api.Assertions.*;

class ParTest {

    @Test
    void setParallelismTest() {
        var par = new Par1();
        int p1 = par.getParallelism();
        assertEquals(p1, par.getParallelism());
        par.setParallelism(4);
        assertEquals(4, par.getParallelism());
//        assertEquals(4, par.forkJoinPool.getParallelism());
        par.setParallelism(8);
        assertEquals(8, par.getParallelism());
//        assertEquals(8, par.forkJoinPool.getParallelism());
        par.setParallelism(p1);
        assertEquals(p1, par.getParallelism());
//        assertEquals(p1, par.forkJoinPool.getParallelism());
    }
    @Test
    void splitForTest0() {
        var par = new Par0();
//        int WIDTH = 1600_0000;
        int WIDTH = 160;
        int HEIGHT = 64;
        int[] target = new int[WIDTH*HEIGHT];
        warmup2(target);
        long startAt = System.currentTimeMillis();
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
        par.splitFor(0, HEIGHT, (from, to) -> {
            for (int j=from; j<to; j++) {
                for (int i=0; i<WIDTH; i++) {
                    var ii = j*WIDTH + i;
                    target[ii] = ii;
                }
            }
            s.add(Thread.currentThread().getName());
        });
        long endAt = System.currentTimeMillis();
        System.out.println("elapsed="+(endAt - startAt));
        for (int i=0; i<target.length; i++) {
            int finalI = i;
            assertEquals(i, target[i], () -> "error at i="+ finalI +": "+target[finalI]);
        }
        assertEquals(1, s.size());
    }
    @Test
    void splitForTest1() {
        var par = new Par1();
//        int WIDTH = 1600_0000;
        int WIDTH = 160;
        int HEIGHT = 64;
        int[] target = new int[WIDTH*HEIGHT];
        warmup2(target);
//        warmUp();
        long startAt = System.currentTimeMillis();
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
        par.splitFor(0, HEIGHT, (from, to) -> {
            for (int j=from; j<to; j++) {
                for (int i=0; i<WIDTH; i++) {
                    var ii = j*WIDTH + i;
                    target[ii] = ii;
                }
            }
            s.add(Thread.currentThread().getName());
        });
        long endAt = System.currentTimeMillis();
        System.out.println("elapsed="+(endAt - startAt));
        for (int i=0; i<target.length; i++) {
            int finalI = i;
            assertEquals(i, target[i], () -> "error at i="+ finalI +": "+target[finalI]);
        }
        assertEquals(par.getParallelism(), s.size());
    }
    @Test
    void warmUpPerfTest() {
        var par = new Par1();
//        int WIDTH = 1600_0000;
        int WIDTH = 160;
        int HEIGHT = 64;
        int[] target = new int[WIDTH * HEIGHT];
        warmup2(target);
        long startAt = System.currentTimeMillis();
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
        warmUp(s, par);
        long endAt = System.currentTimeMillis();
        System.out.println("elapsed="+(endAt - startAt));
        assertEquals(par.getParallelism(), s.size());
    }
    @Test
    void debayerPerfTest() throws IOException {
        int N=4;
        for (int i=0; i<N; i++) {
            String imagePath = "/home/me/Downloads/fsf-data/ZL0_1032_0758545673_098ECM_N0490370ZCAM09037_1100LMJ01.png";
            var imageAndPath = imageIoRead(imagePath, imagePath);
            long start = System.nanoTime();
            BufferedImage bi = debayer_bicubic(imageAndPath.image);
            long end = System.nanoTime();
            System.out.println("nanoseconds: " + nsToString(end - start));
        }
    }
    String nsToString(long ns) {
        return ns/1000_000_000 + "." + String.format("%9d", ns%1000_000_000);
    }
// no use
    void warmUp(Set<String> s, Par1 par) {
        int n = par.getParallelism();
        Callable<Void>[] tasks = new Callable[n];
        IntStream.range(0, n)
                .forEach(i -> {
                    tasks[i] = () -> {
                        s.add(Thread.currentThread().getName());
                        return null;
                    };
                });
        try {
            par.getPool().invokeAll(Arrays.asList(tasks));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    void warmup2(int[] a) {
        for (int i=0; i<a.length; i++) {
            a[i] = -1;
        }
    }
    void foo() {
        Executors.newFixedThreadPool(10);
    }
}