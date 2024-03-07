package com.github.martianch.curieux;

import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractRangeStats_DistributionStatsTest {

    @Test
    public void combinedTest() {
        AbstractRangeStats rs = new RS(3,3);
        IntStream.range(0,10).forEach(i -> rs.updateChannel(0,0));
        IntStream.range(0,11).forEach(i -> rs.updateChannel(1,0));
        IntStream.range(0,12).forEach(i -> rs.updateChannel(2,0));
        IntStream.range(0,20).forEach(i -> rs.updateChannel(0,1));
        IntStream.range(0,21).forEach(i -> rs.updateChannel(1,1));
        IntStream.range(0,22).forEach(i -> rs.updateChannel(2,1));
        IntStream.range(0,30).forEach(i -> rs.updateChannel(0,2));
        IntStream.range(0,31).forEach(i -> rs.updateChannel(1,2));
        IntStream.range(0,32).forEach(i -> rs.updateChannel(2,2));
        assertArrayEquals(rs.counters, new long[]{10,11,12,20,21,22,30,31,32});
        var ds = rs.getDistributionStats();
        assertArrayEquals(ds.totalBefore, new long[]{10,11,12,30,32,34,60,63,66});
        assertEquals(60, ds.getTotal(0));
        assertEquals(63, ds.getTotal(1));
        assertEquals(66, ds.getTotal(2));
    }
}

class RS extends AbstractRangeStats {
    public RS(int channels, int channelSize) {
        super(channels, channelSize);
    }
    @Override
    AbstractRangeStats copy() {
        return null;
    }
}