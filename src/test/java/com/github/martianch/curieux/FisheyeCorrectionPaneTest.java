package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import static com.github.martianch.curieux.FisheyeCorrectionPane.HalfPane.replaceLast;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FisheyeCorrectionPaneTest {
    @Test
    void replaceLastTest() {
        assertEquals("abc", replaceLast("abc", "d", "e"));
        assertEquals("abce", replaceLast("abca", "a", "e"));
        assertEquals("ebc", replaceLast("abc", "a", "e"));
        assertEquals("<br>foo<br>bar<br>baz </html>", replaceLast("<br>foo<br>bar<br>baz<br></html>", "<br>", " "));
        assertEquals("<br>foo<br>bar, baz </html>", replaceLast("<br>foo<br>bar<br>baz </html>", "<br>", ", "));
    }
}