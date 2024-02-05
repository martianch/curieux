package com.github.martianch.curieux;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.martianch.curieux.DebayerBicubic.debayer_bicubic;
import static com.github.martianch.curieux.ImageAndPath.imageIoRead;
import static org.junit.jupiter.api.Assertions.*;

class ParTest {
    @BeforeEach
    void setUp() {
        Par.init();
    }

    @AfterEach
    void tearDown() {
        Par.Configurator.shutdownPar();
    }

    @Test
    void setParallelismTest() {
        int p1 = Par.Configurator.getPoolParallelism();
        assertEquals(p1, Par.forkJoinPool.getParallelism());
        Par.Configurator.setPoolParallelism(4);
        assertEquals(4, Par.forkJoinPool.getParallelism());
        Par.Configurator.setPoolParallelism(8);
        assertEquals(8, Par.forkJoinPool.getParallelism());
        Par.Configurator.setPoolParallelism(p1);
        assertEquals(p1, Par.forkJoinPool.getParallelism());
    }
    @Test
    @Disabled
    void splitForBareWriteTest() {
        int WIDTH = 1600_0000;
        int HEIGHT = 64;
        int[] target = new int[WIDTH*HEIGHT];
        long startAt = System.currentTimeMillis();
        {
            var from = 0;
            var to = target.length;
            for (int j = from; j < to; j++) {
                    target[j] = j;
            }
        }
        long endAt = System.currentTimeMillis();
        System.out.println("elapsed="+(endAt - startAt));
        System.out.println("target size="+(target.length*4L/1000000.)+"M");
        for (int i=0; i<target.length; i++) {
            int finalI = i;
            assertEquals(i, target[i], () -> "error at i="+ finalI +": "+target[finalI]);
        }
    }
    @Test
    @Disabled
    void splitForTest0() {
        var par = new SeqLoopSplitter();
        int WIDTH = 1600_0000;
//        int WIDTH = 160;
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
    @Disabled
    @Test
    void splitForTest1() {
        var par = Par.loopSplitter;
        int WIDTH = 1600_0000;
//        int WIDTH = 160_000;
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
        printThreads(s);
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
    }
    @Disabled
    @Test
    void splitForResTest1() {
//                int WIDTH = 1600_0000;
        int WIDTH = 160_000;
        int HEIGHT = 64;
        int[] array = new int[WIDTH*HEIGHT];
        for (int j=0; j<HEIGHT; j++) {
            for (int i=0; i<WIDTH; i++) {
                var ii = j*WIDTH + i;
                array[ii] = i;
            }
        }
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
        array[WIDTH/2 + WIDTH*(HEIGHT/2)] = WIDTH+1;
        var par = Par.loopSplitter;
        assertEquals(WIDTH+1, parMax(array, 1, array.length, par, s));
        printThreads(s);
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
        assertEquals(WIDTH+1, parMax(array, WIDTH, HEIGHT, par, s));
        printThreads(s);
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
        array[0] = WIDTH+2;
        assertEquals(WIDTH+2, parMax(array, 1, array.length, par, s));
        printThreads(s);
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
        array[array.length-1] = WIDTH+3;
        assertEquals(WIDTH+3, parMax(array, 1, array.length, par, s));
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
        array[1] = WIDTH+4;
        assertEquals(WIDTH+4, parMax(array, 1, array.length, par, s));
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
        array[WIDTH+2] = WIDTH+5;
        assertEquals(WIDTH+5, parMax(array, 1, array.length, par, s));
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
        s.clear();
    }
    @Disabled
    @Test
    void splitForResTest1a() {
        var par = Par.loopSplitter;
//        int WIDTH = 160_000_000;
        var pairCaller = Par.pairRunner;
//        pairCaller.runOne(() -> {

        int WIDTH = 100000000;
        int HEIGHT = 3;
        int[] array = new int[WIDTH*HEIGHT];
        for (int j=0; j<HEIGHT; j++) {
            for (int i=0; i<WIDTH; i++) {
                var ii = j*WIDTH + i;
                array[ii] = i;
            }
        }
        array[WIDTH/2 + WIDTH*(HEIGHT/2)] = WIDTH+1;
        Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<>());
        assertEquals(WIDTH+1, parMax(array, WIDTH, HEIGHT, par, s));
        printThreads(s);
        assertEquals(3,s.size());
        s.clear();
        array[0] = WIDTH+2;
        assertEquals(WIDTH+2, parMax(array, WIDTH, HEIGHT, par, s));
        printThreads(s);
        assertEquals(3,s.size());
        s.clear();
        array[array.length-1] = WIDTH+3;
        assertEquals(WIDTH+3, parMax(array, WIDTH, HEIGHT, par, s));
        assertEquals(3,s.size());
        s.clear();
        array[1] = WIDTH+4;
        assertEquals(WIDTH+4, parMax(array, WIDTH, HEIGHT, par, s));
        assertEquals(3,s.size());
        s.clear();
        array[2] = WIDTH+5;
        assertEquals(WIDTH+5, parMax(array, WIDTH, HEIGHT, par, s));
        assertEquals(3,s.size());
        printThreads(s);
//        });
    }
    void printThreads(Collection<String> s) {
        TreeSet<String> ss = new TreeSet<String>(Comparator.comparing(String::length).thenComparing(Function.identity()));
        ss.addAll(s);
        String d = ss.stream().collect(Collectors.joining("\n"));
        System.out.println("Threads:\n"+d);
    }
    int parMax(int[] arr, int width, int height, LoopSplitter par, Set<String> s) {
        return par.splitFor(0, height, (from, to) -> {
            if (from == to) {
                throw new RuntimeException("empty job "+from+".."+to);
            }
            int res = arr[from*width+0];
            for (int j=from; j<to; j++) {
                for (int i=0; i<width; i++) {
                    int ii = j*width+i;
                    res = Math.max(res, arr[ii]);
                }
            }
            s.add(Thread.currentThread().getName());
            return res;
        }, stream -> {
            return stream.max(Integer::compareTo).get();
        });
    }
    @Disabled
    @Test
    void warmUpPerfTest() {
        var par = Par.loopSplitter;
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
        assertEquals(Par.Configurator.getPoolParallelism(), s.size());
    }
    @Disabled
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
    void warmUp(Set<String> s, LoopSplitter par) {
        int n = par.getNTasksToSpawn();
        Callable<Void>[] tasks = new Callable[n];
        IntStream.range(0, n)
                .forEach(i -> {
                    tasks[i] = () -> {
                        s.add(Thread.currentThread().getName());
                        return null;
                    };
                });
        try {
            Par.forkJoinPool.invokeAll(Arrays.asList(tasks));
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