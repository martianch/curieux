package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class NasaReaderMerTest {

    private List<String> listOfKeys006 = Arrays.asList(
            "0006^128711401^1N128711401EFF0211P1525L0M",
            "0006^128711401^1N128711401EFF0211P1525R0M",
            "0006^128711450^1N128711450EFF0211P1525L0M",
            "0006^128711450^1N128711450EFF0211P1525R0M",
            "0006^128711487^1N128711487EFF0211P1525L0M",
            "0006^128711487^1N128711487EFF0211P1525R0M",
            "0006^128711536^1N128711536EFF0211P1525L0M",
            "0006^128711536^1N128711536EFF0211P1525R0M",
            "0006^128724221^1N128724221EFF0211P1527L0M",
            "0006^128724221^1N128724221EFF0211P1527R0M",
            "0006^128724268^1N128724268EFF0211P1527L0M",
            "0006^128724268^1N128724268EFF0211P1527R0M",
            "0006^128724318^1N128724318EFF0211P1527L0M",
            "0006^128724318^1N128724318EFF0211P1527R0M",
            "0006^128724366^1N128724366EFF0211P1527L0M",
            "0006^128724366^1N128724366EFF0211P1527R0M"
    );

    @Test
    void composeUrlTest() {
        assertEquals(
            "https://mars.nasa.gov/mer/gallery/all/opportunity_r003.html",
            NasaReaderMer.composeUrl(3,'r', WhichRover.OPPORTUNITY)
        );
        assertEquals(
                "https://mars.nasa.gov/mer/gallery/all/opportunity_p5106.html",
                NasaReaderMer.composeUrl(5106,'p', WhichRover.OPPORTUNITY)
        );
        assertEquals(
                "https://mars.nasa.gov/mer/gallery/all/spirit_f2158.html",
                NasaReaderMer.composeUrl(2158,'f', WhichRover.SPIRIT)
        );
        assertEquals(
                "https://mars.nasa.gov/mer/gallery/all/spirit_n684.html",
                NasaReaderMer.composeUrl(684,'n', WhichRover.SPIRIT)
        );
        assertEquals(
                "https://mars.nasa.gov/mer/gallery/all/spirit_m2143.html",
                NasaReaderMer.composeUrl(2143,'m', WhichRover.SPIRIT)
        );
    }

    @Test
    void readTocPageTest() {
        String html;
        try (final Scanner scanner = new Scanner(
                getClass().getResourceAsStream("/opportunity_n006.html"),
                "UTF-8"
            ).useDelimiter("\\A")
        ) {
            html = scanner.next();
        }
        try (MockedStatic<NasaReaderBase> theMock = Mockito.mockStatic(NasaReaderBase.class)) {
            theMock.when(() -> NasaReaderBase.readUrl(any(URL.class))).thenReturn(html);
            var t = NasaReaderMer.readTocPage(6, "https://foo/bar");
            assertEquals(
                    listOfKeys006,
                    new ArrayList<>(t.keySet())
            );
//            for (String s : t.keySet()) {
//                System.out.println(s+",");
//            }

        }
    }

//    @Test
//    void readTocTest() {
//    }
    private RemoteFileNavigatorMer newRfn() {
        return new RemoteFileNavigatorMer();
    }
}