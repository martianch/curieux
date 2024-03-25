package com.github.martianch.curieux;

import org.junit.Test;

import static org.junit.Assert.*;

public class MastcamPairFinderTest {

    @Test
    public void testAreMrMlMatch() {
        {
            String s1 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951MR0154050221302187C00_DXXX.jpg";
            String s2 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951ML0154050221103851C00_DXXX.jpg";
            assertTrue(MastcamPairFinder.areMrMlPair(s1, s2));
            assertFalse(MastcamPairFinder.areMrMlPair(s2, s1));
        }
        {
            String s1 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951MR0154050211302186C00_DXXX.jpg";
            String s2 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951ML0154050211103850C00_DXXX.jpg";
            assertTrue(MastcamPairFinder.areMrMlPair(s1, s2));
            assertFalse(MastcamPairFinder.areMrMlPair(s2, s1));
        }
        {
            String s1 = "file:///home/foo/Downloads/_mars.nasa.gov_msl-raw-images_msss_04132_mcam_4132MR1055230331504678C00_DXXX.jpg";
            String s2 = "file:///home/foo/Downloads/_mars.nasa.gov_msl-raw-images_msss_04132_mcam_4132ML1055230320908223C00_DXXX.jpg";
            assertTrue(MastcamPairFinder.areMrMlPair(s1, s2));
            assertFalse(MastcamPairFinder.areMrMlPair(s2, s1));
        }
//        { // not a pair!!!
//            //                                                               /ssssMXjjjjjjppp
//            String s1 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951MR0154060431302242C00_DXXX.jpg";
//            String s2 = "https://mars.nasa.gov/msl-raw-images/msss/02951/mcam/2951ML0154060441103905C00_DXXX.jpg";
//            assertTrue(MastcamPairFinder.areMrMlMatch(s1, s2));
//            assertFalse(MastcamPairFinder.areMrMlMatch(s2, s1));
//        }
    }
}