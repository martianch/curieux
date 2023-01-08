package com.github.martianch.curieux;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SuccessFailureCounterTest {

    @Test
    public void countSuccess() throws ExecutionException, InterruptedException {
        {
            var sfc = new SuccessFailureCounter();
            String tn = Thread.currentThread().toString();
            sfc.countSuccess();
            assertEquals(new Pair<>(1, 0), sfc.counts.get(tn));
            sfc.countSuccess();
            assertEquals(new Pair<>(2, 0), sfc.counts.get(tn));
            sfc.countSuccess();
            assertEquals(new Pair<>(3, 0), sfc.counts.get(tn));
            String tn2a =
                CompletableFuture.supplyAsync(() -> {
                        String tn2 = Thread.currentThread().toString();
                        sfc.countSuccess();
                        sfc.countSuccess();
                        return tn2;
                    }
                )
                .get();
            assertEquals(sfc.counts.get(tn2a), new Pair<>(2, 0));
        }
        {
            var sfc = new SuccessFailureCounter();
            String tn = Thread.currentThread().toString();
            sfc.countFailure();
            assertEquals(new Pair<>(0,1), sfc.counts.get(tn));
            sfc.countSuccess();
            assertEquals(new Pair<>(1, 1), sfc.counts.get(tn));
            sfc.countSuccess();
            assertEquals(new Pair<>(2, 1), sfc.counts.get(tn));
        }
    }

    @Test
    public void countFailure() throws ExecutionException, InterruptedException {
        {
            var sfc = new SuccessFailureCounter();
            String tn = Thread.currentThread().toString();
            sfc.countFailure();
            assertEquals(new Pair<>(0, 1), sfc.counts.get(tn));
            sfc.countFailure();
            assertEquals(new Pair<>(0, 2), sfc.counts.get(tn));
            sfc.countFailure();
            assertEquals(new Pair<>(0, 3), sfc.counts.get(tn));
        }
        {
            var sfc = new SuccessFailureCounter();
            String tn = Thread.currentThread().toString();
            sfc.countSuccess();
            assertEquals(new Pair<>(1, 0), sfc.counts.get(tn));
            sfc.countFailure();
            assertEquals(new Pair<>(1, 1), sfc.counts.get(tn) );
            sfc.countFailure();
            assertEquals(new Pair<>(1, 2), sfc.counts.get(tn)) ;
            String tn2a =
                CompletableFuture.supplyAsync(() -> {
                            String tn2 = Thread.currentThread().toString();
                            sfc.countFailure();
                            sfc.countFailure();
                            return tn2;
                        }
                )
                .get();
            assertEquals(sfc.counts.get(tn2a), new Pair<>(0, 2));
        }
    }

    @Test
    public void testToString() throws ExecutionException, InterruptedException {
        var sfc = new SuccessFailureCounter();
        String tn = Thread.currentThread().toString();
        sfc.countSuccess();
        String tn2a =
            CompletableFuture.supplyAsync(() -> {
                        String tn2 = Thread.currentThread().toString();
                        sfc.countFailure();
                        sfc.countFailure();
                        return tn2;
                    }
            )
            .get();
        String expected =
            "{\n"
             +  Stream.of(tn, tn2a)
                .sorted()
                .map(x -> " "+x+" -> "+(x.equals(tn)?new Pair(1,0):new Pair(0,2)))
                .collect(Collectors.joining("\n"))
            +   "\n}\n";
        assertEquals(expected, sfc.toString());
    }
}