package com.github.martianch.curieux;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.github.martianch.curieux.FileLocations.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FileLocationsTest {
    @Test
    public void testGetFileName() {
        {
            String s = FileLocations.getFileName("/some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("http://my.site/some/global/path/filename.ext?foo=bar&l=en");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("curious:r:http://my.site/some/global/path/filename.ext?foo=bar&l=en");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("http://my.site/some/global/path/filename?foo=bar&l=en");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileName("http://my.site/some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("curious:l:http://my.site/some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("https://my.site/some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("file://some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("curious:l:file://some/global/path/filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("filename.ext");
            assertThat(s, is("filename.ext"));
        }
        {
            String s = FileLocations.getFileName("/filename.ext");
            assertThat(s, is("filename.ext"));
        }
    }
    @Test
    public void testGetFileNameNoExt() {
        {
            String s = FileLocations.getFileNameNoExt("/some/global/path/filename.ext");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("http://my.site/some/global/path/filename.ext?foo=bar&l=en");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("http://my.site/some/global/path/filename?foo=bar&l=en");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("http://my.site/some/global/path/filename.ext");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("https://my.site/some/global/path/filename.ext");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("file://some/global/path/filename.ext");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("filename.ext");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileNameNoExt("/filename.ext");
            assertThat(s, is("filename"));
        }
    }
    @Test
    public void testGetFileExt() {
        {
            String s = FileLocations.getFileExt("/some/global/path/filename.ext");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("http://my.site/some/global/path/filename.ext?foo=bar&l=en");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("http://my.site/some/global/path/filename?foo=bar&l=en");
            assertThat(s, is(""));
        }
        {
            String s = FileLocations.getFileExt("http://my.site/some/global/path/filename.ext");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("https://my.site/some/global/path/filename.ext");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("file://some/global/path/filename.ext");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("filename.ext");
            assertThat(s, is(".ext"));
        }
        {
            String s = FileLocations.getFileExt("/filename.ext");
            assertThat(s, is(".ext"));
        }
    }
    @Test
    public void testGetFileName2() {
        {
            String s = FileLocations.getFileName("/");
            assertThat(s, is(""));
        }
        {
            String s = FileLocations.getFileName("https://google.com/");
            assertThat(s, is(""));
        }
    }
    public void testGetFileNameNoExt2() {
        {
            String s = FileLocations.getFileNameNoExt("/");
            assertThat(s, is(""));
        }
        {
            String s = FileLocations.getFileNameNoExt("https://google.com/");
            assertThat(s, is(""));
        }
    }
    public void testGetFileExt2() {
        {
            String s = FileLocations.getFileExt("/");
            assertThat(s, is(""));
        }
        {
            String s = FileLocations.getFileExt("https://google.com/");
            assertThat(s, is(""));
        }
    }
    @Test
    public void idFromUrlTest () {
        String stringUrl = "https://mars.nasa.gov/msl-raw-images/msss/02840/mcam/2840MR0148590720704886C00_DXXX.JPG";
        assertTrue("2840MR0148590720704886C00_DXXX".equals(FileLocations.getFileNameNoExt(stringUrl)));
    }
    @Test
    public void testReplaceSuffix() {
        assertThat(FileLocations.replaceSuffix("-thm.jpg", ".jpg", "file-thm.jpg"), is("file.jpg"));
        assertThat(FileLocations.replaceSuffix("-tjm.jpg", ".jpg", "file-thm.jpg"), is("file-thm.jpg"));
        assertThat(FileLocations.replaceSuffix("-thm.jpg", ".jpg", "hm.jpg"), is("hm.jpg"));
        assertThat(FileLocations.replaceSuffix("-thm.jpg", ".jpg", ""), is(""));
    }
    @Test
    public void testTwoPaths() {
        // expected order: R L
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02682/opgs/edr/rcam/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }

        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FRA_397502305EDR_D0010000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FRA_397502305EDR_D0010000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FLA_397502305EDR_D0010000AUT_04096M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FLA_397502305EDR_D0010000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FRA_397502305EDR_D0010000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/fcam/FLA_397502305EDR_D0010000AUT_04096M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RRA_397502188EDR_D0010000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RRA_397502188EDR_D0010000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RLA_397502188EDR_D0010000AUT_04096M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RLA_397502188EDR_D0010000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RRA_397502188EDR_D0010000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00000/opgs/edr/rcam/RLA_397502188EDR_D0010000AUT_04096M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NLA_397681339EDR_F0020000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NRA_397681339EDR_F0020000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NLA_397681339EDR_F0020000AUT_04096M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NRA_397681339EDR_F0020000AUT_04096M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NRA_397681339EDR_F0020000AUT_04096M_.JPG"));
            assertThat(r.get(1), is("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00002/opgs/edr/ncam/NLA_397681339EDR_F0020000AUT_04096M_.JPG"));
        }

        {
            List<String> r = FileLocations.twoPaths("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("./RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("./RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations.twoPaths("https://mars.jpl.nasa.gov/msl-raw-images/msss/02693/mcam/2693MR0140890070604865C00_DXXX.jpg");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("https://mars.jpl.nasa.gov/msl-raw-images/msss/02693/mcam/2693MR0140890070604865C00_DXXX.jpg"));
            assertThat(r.get(1), is("curious:l:https://mars.jpl.nasa.gov/msl-raw-images/msss/02693/mcam/2693MR0140890070604865C00_DXXX.jpg"));
        }
    }

    @Test
    public void test_twoPaths() {
        {
            List<String> r = FileLocations._twoPaths("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations._twoPaths("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("/my/folder/RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("/my/folder/RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations._twoPaths("RLB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("./RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
        {
            List<String> r = FileLocations._twoPaths("RRB_635598676EDR_F0790000RHAZ00337M_.JPG");
            assertThat(r.size(), is(2));
            assertThat(r.get(0), is("RRB_635598676EDR_F0790000RHAZ00337M_.JPG"));
            assertThat(r.get(1), is("./RLB_635598676EDR_F0790000RHAZ00337M_.JPG"));
        }
    }
    @Test
    public void chemcamTwoPathsTest() {
        {
            String path0 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/opgs/edr/ccam/CR0_654932925EDR_F0822176CCAM02899M_.JPG";
            String path1 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/soas/rdr/ccam/CR0_654932925PRC_F0822176CCAM02899L1.PNG";
            List<String> r = FileLocations._twoPaths(path0);
            assertThat(r.size(), is(2));
            assertTrue(r.contains(path0));
            assertTrue(r.contains(path1));
            assertThat(r.get(0), is(path0));
            assertThat(r.get(1), is(path1));
        }
        {
            String path0 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/opgs/edr/ccam/CR0_654932925EDR_F0822176CCAM02899M_.JPG";
            String path1 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/soas/rdr/ccam/CR0_654932925PRC_F0822176CCAM02899L1.PNG";
            List<String> r = FileLocations._twoPaths(path1);
            assertThat(r.size(), is(2));
            assertTrue(r.contains(path0));
            assertTrue(r.contains(path1));
            assertThat(r.get(0), is(path0));
            assertThat(r.get(1), is(path1));
        }
    }
    @Test
    public void chemcamIsTest() {
        String path0 = "CR0_654932925EDR_F0822176CCAM02899M_.JPG";
        String path1 = "CR0_654932925PRC_F0822176CCAM02899L1.PNG";
        assertEquals(true, isChemcamMarkedR(path0));
        assertEquals(false, isChemcamMarkedR(path1));
        assertEquals(false, isChemcamMarkedL(path0));
        assertEquals(true, isChemcamMarkedL(path1));
        assertEquals(path0, chemcamLToR(path1));
        assertEquals(path1, chemcamRToL(path0));
    }
    @Test
    public void chemcamL2RTest() {
        String path0 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/opgs/edr/ccam/CR0_654932925EDR_F0822176CCAM02899M_.JPG";
        String path1 = "https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02900/soas/rdr/ccam/CR0_654932925PRC_F0822176CCAM02899L1.PNG";
        assertEquals(path0, chemcamLToR(path1));
        assertEquals(path1, chemcamRToL(path0));
    }

    @Test
    public void isUrlTest() {
        assertTrue(isUrl("http://google.com"));
        assertTrue(isUrl("https://google.com"));
        assertTrue(isUrl("file://my/file.txt"));
        assertFalse(isUrl("text.txt"));
        assertFalse(isUrl("/text.txt"));
        assertFalse(isUrl("./text.txt"));
        assertFalse(isUrl("/some/dir/text.txt"));
        assertFalse(isUrl("../text.txt"));
        assertFalse(isUrl("dir/text.txt"));
    }

    @Test
    public void getRidOfBackslashesTest() {
        assertThat(FileLocations.getRidOfBackslashes("c:\\test\\"), is("c:/test/"));
    }
    @Test
    public void isBayeredTest() {
        assertTrue(isBayered("0560ML0022630070204612C00_DXXX.jpg"));
        assertFalse(isBayered("0560ML0022630070204612E01_DXXX.jpg"));
        assertTrue(isBayered("/foo/bar/0560ML0022630070204612C00_DXXX.jpg"));
        assertFalse(isBayered("/foo/bar/0560ML0022630070204612E01_DXXX.jpg"));
        assertTrue(isBayered("file://foo/bar/0560ML0022630070204612C00_DXXX.jpg"));
        assertFalse(isBayered("file://foo/bar/0560ML0022630070204612E01_DXXX.jpg"));
        assertTrue(isBayered("https://mars.nasa.gov/foo/bar/0560ML0022630070204612C00_DXXX.jpg"));
        assertFalse(isBayered("https://mars.nasa.gov/foo/bar/0560ML0022630070204612E01_DXXX.jpg"));
        assertFalse(isBayered("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02804/opgs/edr/ncam/NRB_646422318EDR_F0810628NCAM00354M_.JPG"));
        assertFalse(isBayered("NRB_646422318EDR_F0810628NCAM00354M_.JPG"));
        assertFalse(isBayered("/tmp/NRB_646422318EDR_F0810628NCAM00354M_.JPG"));
        assertTrue(isBayered("https://mars.nasa.gov/msl-raw-images/msss/02914/mcam/2914ML0152070131101802K00_DXXX.jpg"));
    }
    @Test
    public void isBayeredTest2() {
        // Perseverance Navcam
        assertTrue(isBayered("NLE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("NRE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("/foo/bar/NLE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("/foo/bar/NRE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("file://foo/bar/NLE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00015/ids/edr/browse/ncam/NLE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00015/ids/edr/browse/ncam/NRE_0015_0668275677_973ECM_N0030188NCAM00400_08_0LLJ01.png"));
        assertTrue(isBayered("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00030/ids/edr/browse/shrlc/SI1_0030_0669612176_172ECM_N0030828SRLC07000_0000LUJ01.png"));
        assertFalse(isBayered("https://mars.nasa.gov/mars2020-raw-images/pub/ods/surface/sol/00030/ids/edr/browse/shrlc/SIF_0030_0669612176_172EBY_N0030828SRLC07000_0000LUJ01.png"));
    }
    @Test
    public void getSolTest() {
        assertEquals(Optional.of(2804),FileLocations.getSol("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02804/opgs/edr/ncam/NRB_646422318EDR_F0810628NCAM00354M_.JPG"));
        assertEquals(Optional.of(2700),FileLocations.getSol("file://foo/bar/2700/0560ML0022630070204612C00_DXXX.jpg"));
        assertEquals(Optional.of(2701),FileLocations.getSol("/foo/bar/2701/0560ML0022630070204612C00_DXXX.jpg"));
        assertEquals(Optional.of(2702),FileLocations.getSol("C:\\foo\\bar\\2702\\0560ML0022630070204612C00_DXXX.jpg"));
        assertEquals(Optional.empty(),FileLocations.getSol("/foo/bar/0560ML0022630070204612C00_DXXX.jpg"));
    }
    @Test
    public void isMrlTest() {
//        assertEquals(true, FileLocations.isMrl("/msl-raw-images/msss/02893/mcam/2893ML0151010021101295C00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMrl("/msl-raw-images/msss/02893/mcam/2893MR0151010011300209C00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMrl("2893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMrl("2893MR0151010011300209C00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMrl("2914ML0152070131101802K00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMrl("https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893MR0151010021300210C00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMrl("https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893ML0151010021101295C00_DXXX.jpg"));
//        assertEquals(false, FileLocations.isMrl("/msl-raw-images/msss/02893/mcam/02893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(false, FileLocations.isMrl("02893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(false, FileLocations.isMrl("CR0_654758502PRC_F0822176CCAM03897L1.PNG"));
//        assertEquals(false, FileLocations.isMrl("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02898/soas/rdr/ccam/CR0_654758502PRC_F0822176CCAM03897L1.PNG"));
    }
    @Test
    public void isMrTest() {
//        assertEquals(false, FileLocations.isMr("/msl-raw-images/msss/02893/mcam/2893ML0151010021101295C00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMr("/msl-raw-images/msss/02893/mcam/2893MR0151010011300209C00_DXXX.jpg"));
        assertEquals(false, FileLocations.isMr("2893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMr("2893MR0151010011300209C00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMr("2914MR0152070131101802K00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMr("https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893MR0151010021300210C00_DXXX.jpg"));
//        assertEquals(false, FileLocations.isMr("https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893ML0151010021101295C00_DXXX.jpg"));
//        assertEquals(true, FileLocations.isMr("/msl-raw-images/msss/02893/mcam/2893MR0151010021300210C00_DXXX.jpg"));
//        assertEquals(false, FileLocations.isMr("/msl-raw-images/msss/02893/mcam/02893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(false, FileLocations.isMr("02893ML0151010021101295C00_DXXX.jpg"));
        assertEquals(false, FileLocations.isMr("CR0_654758502PRC_F0822176CCAM03897L1.PNG"));
//        assertEquals(false, FileLocations.isMr("https://mars.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/02898/soas/rdr/ccam/CR0_654758502PRC_F0822176CCAM03897L1.PNG"));
    }
    @Test
    public void isMlTest() {
        assertEquals(false, FileLocations.isMl("2914MR0152070131101802K00_DXXX.jpg"));
        assertEquals(true, FileLocations.isMl("2914ML0152070131101802K00_DXXX.jpg"));
    }
    @Test
    public void replaceFileNameTest() {
        String ml = "2893ML0151010021101295C00_DXXX.jpg";
        String mr = "2893MR0151010021300210C00_DXXX.jpg";
        String mll = "https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893ML0151010021101295C00_DXXX.jpg";
        String mrl = "https://mars.nasa.gov/msl-raw-images/msss/02893/mcam/2893MR0151010021300210C00_DXXX.jpg";
        assertEquals(mr, FileLocations.replaceFileName(ml, mr));
        assertEquals(ml, FileLocations.replaceFileName(mr, ml));
        assertEquals(mrl, FileLocations.replaceFileName(mll, mr));
        assertEquals(mrl, FileLocations.replaceFileName(mrl, mr));
        assertEquals(mll, FileLocations.replaceFileName(mrl, ml));
        assertEquals(mll, FileLocations.replaceFileName(mll, ml));
    }
}
