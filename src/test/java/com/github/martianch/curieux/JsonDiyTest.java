package com.github.martianch.curieux;


import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.github.martianch.curieux.JsonDiy.getStringDelimitedBy;
import static org.junit.Assert.*;

public class JsonDiyTest {
    @Test
    public void stringDelimitedBy_Test() {
        var is = new JsonDiy.InputState(0,"\"foo bar\"");
        assertEquals('"', is.get());
        assertEquals("foo bar", getStringDelimitedBy(is, '"', '\0'));
        assertEquals("", getStringDelimitedBy(is, '"', '\0'));
    }
    @Test
    public void stringDelimitedByString1_Test() {
        var is = new JsonDiy.InputState(0,"\"foo bar\"");
        assertEquals('\"', is.get());
        assertEquals("foo bar", getStringDelimitedBy(is, new char[]{'"'}));
    }
    @Test
    public void stringDelimitedByString2_Test() {
        var is = new JsonDiy.InputState(0,"[foo bar, baz qux]");
        assertEquals('[', is.get());
        assertEquals("foo bar", getStringDelimitedBy(is, ',', ']'));
        assertEquals(" baz qux", getStringDelimitedBy(is, ',', ']'));
    }
    @Test
    public void getArray_Test() {
        var is = new JsonDiy.InputState(0,"[foo bar, baz qux]");
        assertEquals('[', is.get());
        var list = JsonDiy.getArray(is,'\0');
        assertEquals(2, list.size());
        assertEquals(new JsonDiy.Symbol("foo bar"), list.get(0));
        assertEquals(new JsonDiy.Symbol("baz qux"), list.get(1));
        assertEquals(Arrays.asList(new JsonDiy.Symbol("foo bar"),new JsonDiy.Symbol("baz qux")), list);
    }
    @Test
    public void getArrayWs_Test() {
        var is = new JsonDiy.InputState(0,"[  foo bar  ,baz qux  ]");
        assertEquals('[', is.get());
        var list = JsonDiy.getArray(is,'\0');
        assertEquals(Arrays.asList(new JsonDiy.Symbol("foo bar"),new JsonDiy.Symbol("baz qux")), list);
    }
    @Test
    public void getArrayQ_Test() {
        var is = new JsonDiy.InputState(0,"[\"foo bar\", \"baz qux\"]");
        assertEquals('[', is.get());
        var list = JsonDiy.getArray(is,'\0');
        assertEquals(Arrays.asList("foo bar","baz qux"), list);
    }
    @Test
    public void getArrayE_Test() {
        var is = new JsonDiy.InputState(0,"[\"foo bar\", \"baz qux\"");
        assertEquals('[', is.get());
        var excWas = false;
        try {
            var list = JsonDiy.getArray(is);
//            assertEquals(Arrays.asList("foo bar", "baz qux"), list);
//            assertEquals(2, list.size());
//            assertEquals("foo bar", list.get(0));
//            assertEquals("baz qux", list.get(1));
//            assertEquals(Arrays.asList("foo bar", "baz qux"), list);
        } catch (JsonDiy.PrematureEndOfInputException e) {
            excWas = true;
        }
        assertTrue(excWas);
        assertTrue(is.pos >= is.source.length());
    }
    @Test
    public void jsonToDataStructure_String_Test() {
        {
            String s = "foo bar\", \"baz qux\"";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(new JsonDiy.Symbol(s), obj);
        }
        {
            String s = "[\"foo bar\", \"baz qux\"]";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(Arrays.asList("foo bar", "baz qux"), obj);
        }
        {
            String s = "[   foo bar  ,   baz qux   ]";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(Arrays.asList(new JsonDiy.Symbol("foo bar"), new JsonDiy.Symbol("baz qux")), obj);
        }
        {
            String s = "[\"foo bar\", \"baz qux\"";
            boolean excWas = false;
            try {
                var obj = JsonDiy.jsonToDataStructure(s);
            } catch (JsonDiy.PrematureEndOfInputException e) {
                excWas = true;
            }
            assertTrue(excWas);
        }
        {
            String s = "{foo: bar, baz : qux }";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(Map.of("foo",new JsonDiy.Symbol("bar"),"baz",new JsonDiy.Symbol("qux")), obj);
        }
        {
            String s = "{\"foo bar\":\"baz qux\", \"corge grault\" : \"waldo garply\"  }";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(Map.of("foo bar","baz qux","corge grault","waldo garply"), obj);
        }
        {
            String s = "{\"foo bar\":\"baz qux\", \"corge grault\" : \"waldo garply\"  ";
            boolean excWas = false;
            try {
                var obj = JsonDiy.jsonToDataStructure(s);
            } catch (JsonDiy.DelimiterNotFoundException e) {
                excWas = true;
            }
            assertTrue(excWas);
        }
        {
            String s = "{\"foo bar\":\"baz qux\", \"corge grault\" : \"waldo garply";
            boolean excWas = false;
            try {
                var obj = JsonDiy.jsonToDataStructure(s);
            } catch (JsonDiy.DelimiterNotFoundException e) {
                excWas = true;
            }
            assertTrue(excWas);
        }
        {
            String s = "{\"foo bar\":\"baz qux\", \"corge grault\" :  ";
            boolean excWas = false;
            try {
                var obj = JsonDiy.jsonToDataStructure(s);
            } catch (JsonDiy.DelimiterNotFoundException e) {
                excWas = true;
            }
            assertTrue(excWas);
        }
        {
            String s = "{\"foo bar\":\"baz qux\", \"corge grault\" }";
            boolean excWas = false;
            try {
                var obj = JsonDiy.jsonToDataStructure(s);
            } catch (JsonDiy.DelimiterNotFoundException e) {
                excWas = true;
            }
            assertTrue(excWas);
        }
    }
    @Test
    public void jsonToDataStructure_real_Test() {
        {
            String s = "{\"items\":\n" +
                    "\t[{\n" +
                    "\t\t\"id\":374484,\n" +
                    "\t\t\"imageid\":\"1489ML0074780020603644D01_DXXX\",\n" +
                    "\t\t\"sol\":1489,\n" +
                    "\t\t\"instrument\":\"MAST_LEFT\",\n" +
                    "\t\t\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/msss/01489/mcam/1489ML0074780020603644D01_DXXX.jpg\",\n" +
                    "\t\t\"created_at\":\"2019-07-30T00:48:45.854Z\",\n" +
                    "\t\t\"updated_at\":\"2019-09-06T00:07:47.134Z\",\n" +
                    "\t\t\"mission\":\"msl\",\n" +
                    "\t\t\"extended\":\n" +
                    "\t\t{\n" +
                    "\t\t\t\"lmst\":null,\n" +
                    "\t\t\t\"bucket\":\"msl-raws\",\n" +
                    "\t\t\t\"mast_az\":null,\n" +
                    "\t\t\t\"mast_el\":null,\n" +
                    "\t\t\t\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/msss/01489/mcam/1489ML0074780020603644D01_DXXX.jpg\",\n" +
                    "\t\t\t\"contributor\":\"MSSS\",\n" +
                    "\t\t\t\"filter_name\":null,\n" +
                    "\t\t\t\"sample_type\":\"subframe\"\n" +
                    "\t\t},\n" +
                    "\t\t\"date_taken\":\"2016-10-13T23:20:28.000Z\",\n" +
                    "\t\t\"date_received\":\"2016-10-22T23:00:58.000Z\",\n" +
                    "\t\t\"link\":\"/raw_images/374484\",\n" +
                    "\t\t\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/msss/01489/mcam/1489ML0074780020603644D01_DXXX.jpg\"\n" +
                    "\t}],\n" +
                    "\"more\":false,\n" +
                    "\"total\":1,\n" +
                    "\"page\":0,\n" +
                    "\"per_page\":10\n" +
                    "}\n";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(new JsonDiy.Symbol("false"), JsonDiy.get(obj,"more"));
            assertEquals(new JsonDiy.Symbol("1"), JsonDiy.get(obj,"total"));
            assertEquals(new JsonDiy.Symbol("0"), JsonDiy.get(obj,"page"));
            assertEquals(new JsonDiy.Symbol("374484"), JsonDiy.get(obj,"items","0","id"));
            assertEquals("2016-10-13T23:20:28.000Z", JsonDiy.get(obj,"items","0","date_taken"));
            assertEquals("2016-10-13T23:20:28.000Z", JsonDiy.get(JsonDiy.get(obj,"items","0"),"date_taken"));
            assertEquals("2016-10-13T23:20:28.000Z", JsonDiy.get(JsonDiy.get(JsonDiy.get(obj,"items"),"0"),"date_taken"));
            assertEquals(null, JsonDiy.get(obj,"items","2","date_taken"));
            assertEquals(null, JsonDiy.get(obj,"iitems","0","date_taken"));
            assertEquals(null, JsonDiy.get(obj,"items","0","date_takenn"));
            assertEquals(null, JsonDiy.get(obj,"items","0","date_taken","a"));
        }
    }
    @Test
    public void jsonToDataStructure_real1_Test() {
        {
            String s = "{\n" +
                    "\"more\":false,\n" +
                    "\"total\":1,\n" +
                    "\"page\":0,\n" +
                    "\"per_page\":10\n" +
                    "}\n" +
                    "\n";
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(new JsonDiy.Symbol("false"), JsonDiy.get(obj,"more"));
            assertEquals(new JsonDiy.Symbol("1"), JsonDiy.get(obj,"total"));
            assertEquals(new JsonDiy.Symbol("0"), JsonDiy.get(obj,"page"));
        }
    }
    @Test
    public void jsonToDataStructure_real2_Test() {
        {
            String s = "{\n" +
                    "\"items\":\n" +
                    "\t[] ,\n" +
                    "\"more\":false,\n" +
                    "\"total\":1,\n" +
                    "\"page\":0,\n" +
                    "\"per_page\":10\n" +
                    "}\n" +
                    "\n"
                    ;
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(new JsonDiy.Symbol("false"), JsonDiy.get(obj,"more"));
            assertEquals(new JsonDiy.Symbol("1"), JsonDiy.get(obj,"total"));
            assertEquals(new JsonDiy.Symbol("0"), JsonDiy.get(obj,"page"));
        }
    }
    @Test
    public void jsonToDataStructure_real3_Test() {
        {
            String s = "{\n" +
                    "\"items\":\n" +
                    "\t[{},{}] ,\n" +
                    "\"more\":false,\n" +
                    "\"total\":1,\n" +
                    "\"page\":0,\n" +
                    "\"per_page\":10\n" +
                    "}\n" +
                    "\n"
                    ;
            var obj = JsonDiy.jsonToDataStructure(s);
            assertEquals(new JsonDiy.Symbol("false"), JsonDiy.get(obj,"more"));
            assertEquals(new JsonDiy.Symbol("1"), JsonDiy.get(obj,"total"));
            assertEquals(new JsonDiy.Symbol("0"), JsonDiy.get(obj,"page"));
        }
    }
    @Test
    public void toStringTest() {
        String s = "{\"foo\":\"bar\", \"a\":[{\"baz\":\"qux\", \"n\":1},{\"waldo\":\"garply\",\"n\":2}]}";
        var obj = JsonDiy.jsonToDataStructure(s);
        assertEquals(
                "{\"a\":[{\"baz\":\"qux\", \"n\":1}, {\"n\":2, \"waldo\":\"garply\"}], \"foo\":\"bar\"}",
                JsonDiy.toString(obj)
        );
    }
    @Test
    public void deleteAllKeysBut_test() {
        String s = "{\"foo\":\"bar\", \"a\":[{\"baz\":\"qux\", \"n\":1},{\"waldo\":\"garply\",\"n\":2}], \"plugh\":\"xyzzy\"}";
        var obj = JsonDiy.jsonToDataStructure(s);
        var obj2 = JsonDiy.deleteAllKeysBut(obj, "plugh", "n", "a");
        assertEquals(
                "{\"a\":[{\"n\":1}, {\"n\":2}], \"plugh\":\"xyzzy\"}",
                JsonDiy.toString(obj2)
        );
    }
}
