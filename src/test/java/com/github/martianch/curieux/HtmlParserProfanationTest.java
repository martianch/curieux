package com.github.martianch.curieux;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class HtmlParserProfanationTest
{
    @Test
    public void getNodeTest() {
        var doc = "<foo bar><bar baar> <qux quux>";
        {
            var res = HtmlParserProfanation.getNode(doc, "bar", s -> s);
            assertThat(res, is("<bar baar>"));
        }
        {
            var res = HtmlParserProfanation.getNode(doc, "foo", s -> s);
            assertThat(res, is("<foo bar>"));
        }
        {
            var res = HtmlParserProfanation.getNode(doc, "qux", s -> s);
            assertThat(res, is("<qux quux>"));
        }
        {
            var res = HtmlParserProfanation.getNode(doc, "baz", s -> s);
            assertThat(res, is(nullValue()));
        }
    }
    @Test
    public void getCuriosityRawImageUrlTest() {
        {
            var doc = "<meta content='foo://bar'  property='og:image'>";
            var res = HtmlParserProfanation.getCuriosityRawImageUrl(doc);
            assertThat(res, is("foo://bar"));
        }
        {
            var doc = "<meta  property='og:image' content='foo://bar'>";
            var res = HtmlParserProfanation.getCuriosityRawImageUrl(doc);
            assertThat(res, is("foo://bar"));
        }
        {
            var doc = "<meta  property='og:image' content=foo://bar>";
            var res = HtmlParserProfanation.getCuriosityRawImageUrl(doc);
            assertThat(res, is("foo://bar"));
        }
        {
            var doc = "<meta  property='og:image' content=\"foo://bar\">";
            var res = HtmlParserProfanation.getCuriosityRawImageUrl(doc);
            assertThat(res, is("foo://bar"));
        }
    }
    @Test
    public void getValueTest() {
        var doc = "<macro foo='bar' baz=\"baar\" baaz=qux corge=grault>";
        {
            var res = HtmlParserProfanation.getValue(doc,"foo");
            assertThat(res, is("bar"));
        }
        {
            var res = HtmlParserProfanation.getValue(doc,"foo=");
            assertThat(res, is("bar"));
        }
        {
            var res = HtmlParserProfanation.getValue(doc,"baz");
            assertThat(res, is("baar"));
        }
        {
            var res = HtmlParserProfanation.getValue(doc,"baaz");
            assertThat(res, is("qux"));
        }
        {
            var res = HtmlParserProfanation.getValue(doc,"corge");
            assertThat(res, is("grault"));
        }
    }
}