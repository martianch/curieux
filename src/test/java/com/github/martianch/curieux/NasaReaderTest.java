package com.github.martianch.curieux;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static com.github.martianch.curieux.NasaReader.dataStructureMrlMatchesFromImageId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * These tests need internet connection to the NASA site
 */
public class NasaReaderTest {
    @Ignore
    @Test
    public void dataStructureMRLPairsFromImageIdTest() throws IOException {
        String id = "1407ML0068890110601897C00_DXXX.jpg";
        Object pairs = dataStructureMrlMatchesFromImageId(id);
        System.out.println(pairs);
//        List<Object> list = (List<Object>) JsonDiy.get(jsonObject, "items");
//        list.stream().forEach( o -> {
//            if (o instanceof Map) {
//                Map m = (Map) o;
//                String date = m.get("date_taken").toString();
//                String id = m.get("imageid").toString();
//                nmap.put(date+"^"+id, m);
//            }
//        });
        fail("This test must not be run at build time!");
    }
    @Ignore
    @Test
    public void findMrlMatchTest() throws IOException {
        String mre = "1407MR0068890110702105E01_DXXX";
        String mle = "1407ML0068890110601897E01_DXXX";
        String mrc = "1407MR0068890110702105C00_DXXX";
        String mlc = "1407ML0068890110601897C00_DXXX";
        assertEquals(
                mre,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mle).get())
        );
        assertEquals(
                mrc,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mlc).get())
        );
        assertEquals(
                mle,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mre).get())
        );
        assertEquals(
                mlc,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mrc).get())
        );
        fail("This test must not be run at build time!");
    }
    //https://mars.jpl.nasa.gov/msl-raw-images/msss/01407/mcam/1407MR0068890100702104C00_DXXX.JPG
    //https://mars.jpl.nasa.gov/msl-raw-images/msss/01407/mcam/1407MR0068890100702104E01_DXXX.jpg
    //https://mars.jpl.nasa.gov/msl-raw-images/msss/01407/mcam/1407ML0068890100601896C00_DXXX.jpg
    //https://mars.jpl.nasa.gov/msl-raw-images/msss/01407/mcam/1407ML0068890100601896E01_DXXX.jpg
    @Ignore
    @Test
    public void findMrlMatchTest2() throws IOException {
        String mre = "1407MR0068890100702104E01_DXXX";
        String mle = "1407ML0068890100601896E01_DXXX";
        String mrc = "1407MR0068890100702104C00_DXXX";
        String mlc = "1407ML0068890100601896C00_DXXX";
        assertEquals(
                mrc,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mlc).get())
        );
        assertEquals(
                mlc,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mrc).get())
        );
        assertEquals(
                mre,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mle).get())
        );
        assertEquals(
                mle,
                FileLocations.getFileNameNoExt(MastcamPairFinder.findMrlMatch(mre).get())
        );
        fail("This test must not be run at build time!");
    }
}