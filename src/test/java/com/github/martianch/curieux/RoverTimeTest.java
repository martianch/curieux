package com.github.martianch.curieux;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class RoverTimeTest {

    @Test
    public void toUtcMillisTest() {
        assertThat(RoverTime.toUtcMillis(636566514), is(1583296902000L));
    }
    @Test
    public void parseTimestampTest() {
        assertThat(RoverTime.parseTimestamp(4,"NRB_636566514EDR1234.JPG"), is(636566514L));
        assertThat(RoverTime.parseTimestamp(4,"NRB_63656EDR1234.JPG"), is(0L));
        assertThat(RoverTime.parseTimestamp(4,"NRB_-63656EDR1234.JPG"), is(0L));
    }
    @Test
    public void earthDateForFileTest() {
        //RLB_636566514EDR_F0790294RHAZ00313M_.JPG	Sol 2693 (2020-03-04T04:41:42.000Z)
        assertThat(RoverTime.earthDateForFile(4,"RLB_636566514EDR_F0790294RHAZ00313M_.JPG"), is("2020-03-04T04:41:42"));
        assertThat(RoverTime.earthDateForFile(4,"NRB_63656EDR1234.JPG"), is(""));
        assertThat(RoverTime.earthDateForFile(4,"NRB_-63656EDR1234.JPG"), is(""));
    }
}