package com.github.martianch.curieux;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class RoverTimeTest {

    @Test
    public void toUtcMillisTest() {
        assertThat(RoverTime.toUtcMillisC(636566514), is(1583296902000L));
        assertThat(RoverTime.toUtcMillisP(666952977), is(1613681069000L)); // perseverance landing
        assertThat(RoverTime.toUtcMillisP(726428840), is(1673157401000L)); // 2023-01-08T05:56:41
    }
    @Test
    public void parseTimestampTest() {
        assertThat(RoverTime.parseTimestamp(RoverTime.TIMESTAMP_OFFSET_CURIOSITY,"NRB_636566514EDR1234.JPG"), is(636566514L));
        assertThat(RoverTime.parseTimestamp(RoverTime.TIMESTAMP_OFFSET_CURIOSITY,"NRB_63656EDR1234.JPG"), is(0L));
        assertThat(RoverTime.parseTimestamp(RoverTime.TIMESTAMP_OFFSET_CURIOSITY,"NRB_-63656EDR1234.JPG"), is(0L));
        assertThat(RoverTime.parseTimestamp(RoverTime.TIMESTAMP_OFFSET_MER,"2P226681417EFFASWTP2567R2M1.JPG"), is(226681417L));
    }
    @Test
    public void earthDateForFileTest() {
        //RLB_636566514EDR_F0790294RHAZ00313M_.JPG	Sol 2693 (2020-03-04T04:41:42.000Z)
        assertThat(RoverTime.earthDateForFile("RLB_636566514EDR_F0790294RHAZ00313M_.JPG"), is("2020-03-04T04:41:42"));
        assertThat(RoverTime.earthDateForFile("NRB_63656EDR1234.JPG"), is(""));
        assertThat(RoverTime.earthDateForFile("NRB_-63656EDR1234.JPG"), is(""));
        assertThat(RoverTime.earthDateForFile("RRR_0000_0666952977_663ECM_N0010044AUT_04096_00_2I3J02.png"), is("2021-02-18T20:44:29"));
        assertThat(RoverTime.earthDateForFile("NRF_0670_0726428840_635ECM_N0320672NCAM12670_04_195J01.png"), is("2023-01-08T05:56:41"));

        // January 25, 2004 (GMT) (January 24, 2004, PST) landing
        //https://mars.nasa.gov/mer/gallery/all/1/p/003/1P128449364EFF0200P2213R2M1.JPG
        assertThat(RoverTime.earthDateForFile("1P128449364EFF0200P2213R2M1.JPG"), is("2004-01-27T04:21:39"));

    }
}
