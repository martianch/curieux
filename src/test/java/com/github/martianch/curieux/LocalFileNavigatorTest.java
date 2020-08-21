package com.github.martianch.curieux;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LocalFileNavigatorTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void createFiles() throws IOException {
        tempFolder.newFile("baz.png");
        tempFolder.newFile("qux.png");
        tempFolder.newFile("foo.png");
        tempFolder.newFile("bar.png");
    }

    @Test
    public void test_loadInitial_dir_up() {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(tempFolder.getRoot().toString());
        assertEquals(null, lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        assertEquals("bar.png", lfn.toNext().getCurrentKey());
        assertEquals("baz.png", lfn.toNext().getCurrentKey());
        assertEquals("foo.png", lfn.toNext().getCurrentKey());
        assertEquals("qux.png", lfn.toNext().getCurrentKey());
        assertEquals(null, lfn.toNext().getCurrentKey());
        System.out.println(lfn.nmap);
    }

    @Test
    public void test_loadInitial_dir_up_path() {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(tempFolder.getRoot().toString());
        assertEquals(null, lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        assertEquals(new File(tempFolder.getRoot(),"bar.png").getAbsolutePath(), lfn.toNext().getCurrentPath());
        assertEquals(new File(tempFolder.getRoot(),"baz.png").getAbsolutePath(), lfn.toNext().getCurrentPath());
        assertEquals(new File(tempFolder.getRoot(),"foo.png").getAbsolutePath(), lfn.toNext().getCurrentPath());
        assertEquals(new File(tempFolder.getRoot(),"qux.png").getAbsolutePath(), lfn.toNext().getCurrentPath());
        assertEquals(null, lfn.toNext().getCurrentPath());
        System.out.println(lfn.nmap);
    }
    @Test
    public void test_loadInitial_file_up() {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(new File(tempFolder.getRoot(),"foo.png").toString());
        assertEquals("foo.png", lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        assertEquals("qux.png", lfn.toNext().getCurrentKey());
        assertEquals(null, lfn.toNext().getCurrentKey());
        assertEquals("bar.png", lfn.toNext().getCurrentKey());
        assertEquals("baz.png", lfn.toNext().getCurrentKey());
        assertEquals("foo.png", lfn.toNext().getCurrentKey());
        System.out.println(lfn.nmap);
    }
    @Test
    public void test_loadInitial_changes_up() throws IOException {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(new File(tempFolder.getRoot(),"foo.png").toString());
        assertEquals("foo.png", lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        tempFolder.newFile("aaa.png");
        tempFolder.newFile("zzz.png");
        assertEquals("qux.png", lfn.toNext().getCurrentKey());
//        assertEquals(null, lfn.toNext().getCurrentKey());
        assertEquals("zzz.png", lfn.toNext().getCurrentKey());
        assertEquals(null, lfn.toNext().getCurrentKey());
        assertEquals("aaa.png", lfn.toNext().getCurrentKey());
        assertEquals("bar.png", lfn.toNext().getCurrentKey());
        assertEquals("baz.png", lfn.toNext().getCurrentKey());
        assertEquals("foo.png", lfn.toNext().getCurrentKey());
        assertEquals("qux.png", lfn.toNext().getCurrentKey());
        assertEquals("zzz.png", lfn.toNext().getCurrentKey());
        assertEquals(null, lfn.toNext().getCurrentKey());
        System.out.println(lfn.nmap);
    }

    @Test
    public void test_loadInitial_dir_dn() {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(tempFolder.getRoot().toString());
        assertEquals(null, lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        assertEquals("qux.png", lfn.toPrev().getCurrentKey());
        assertEquals("foo.png", lfn.toPrev().getCurrentKey());
        assertEquals("baz.png", lfn.toPrev().getCurrentKey());
        assertEquals("bar.png", lfn.toPrev().getCurrentKey());
        assertEquals(null, lfn.toPrev().getCurrentKey());
        System.out.println(lfn.nmap);
    }

    @Test
    public void test_loadInitial_file_dn() {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(new File(tempFolder.getRoot(),"foo.png").toString());
        assertEquals("foo.png", lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        assertEquals("baz.png", lfn.toPrev().getCurrentKey());
        assertEquals("bar.png", lfn.toPrev().getCurrentKey());
        assertEquals(null, lfn.toPrev().getCurrentKey());
        assertEquals("qux.png", lfn.toPrev().getCurrentKey());
        assertEquals("foo.png", lfn.toPrev().getCurrentKey());
        System.out.println(lfn.nmap);
    }

    @Test
    public void test_loadInitial_changes_dn() throws IOException {
        LocalFileNavigator lfn = new LocalFileNavigator();
        lfn._loadInitial(new File(tempFolder.getRoot(),"foo.png").toString());
        assertEquals("foo.png", lfn.currentKey);
        assertEquals(4, lfn.nmap.size());
        tempFolder.newFile("aaa.png");
        tempFolder.newFile("zzz.png");
        assertEquals("baz.png", lfn.toPrev().getCurrentKey());
        assertEquals("bar.png", lfn.toPrev().getCurrentKey());
//        assertEquals(null, lfn.toPrev().getCurrentKey());
        assertEquals("aaa.png", lfn.toPrev().getCurrentKey());
        assertEquals(null, lfn.toPrev().getCurrentKey());
        assertEquals("zzz.png", lfn.toPrev().getCurrentKey());
        assertEquals("qux.png", lfn.toPrev().getCurrentKey());
        assertEquals("foo.png", lfn.toPrev().getCurrentKey());
        System.out.println(lfn.nmap);
    }
}