package com.github.martianch.curieux;

import org.junit.Test;

import java.util.List;

import static com.github.martianch.curieux.FileLocations.isBayered;
import static com.github.martianch.curieux.FileLocations.isUrl;
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
            String s = FileLocations.getFileName("http://my.site/some/global/path/filename?foo=bar&l=en");
            assertThat(s, is("filename"));
        }
        {
            String s = FileLocations.getFileName("http://my.site/some/global/path/filename.ext");
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
            assertThat(r.get(1), is(""));
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
    }
}
