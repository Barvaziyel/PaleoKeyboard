package io.github.barvaziyel.paleokeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardTextTest {

    @Test
    fun `backspace removes a whole Paleo-Hebrew letter`() {
        assertEquals(2, charsToDelete("𐤀𐤁"))
    }

    @Test
    fun `the word divider is U+1091F`() {
        assertEquals(0x1091F, WORD_DIVIDER.codePointAt(0))
        assertEquals(2, WORD_DIVIDER.length)
    }

    @Test
    fun `backspace removes a whole word divider`() {
        assertEquals(2, charsToDelete("𐤀𐤁$WORD_DIVIDER"))
    }

    @Test
    fun `backspace takes the invisible mark with the letter after it`() {
        // Text typed by the old version had a mark before every letter.
        assertEquals(3, charsToDelete("${RLM}𐤀${RLM}𐤁"))
        assertEquals(2, charsToDelete("${RLM}𐤀${RLM} "))
        assertEquals(3, charsToDelete("${RLM}𐤀"))
    }

    @Test
    fun `backspace never stops on a lone invisible mark`() {
        assertEquals(1, charsToDelete("${RLM}"))
        assertEquals(4, charsToDelete("${RLM}𐤀${RLM}"))
    }

    @Test
    fun `backspace removes ordinary characters one at a time`() {
        assertEquals(1, charsToDelete("abc"))
        assertEquals(1, charsToDelete("a\n"))
    }

    @Test
    fun `backspace removes a whole emoji`() {
        assertEquals(4, charsToDelete("ok 👍🏽"))
        assertEquals(8, charsToDelete("👨‍👩‍👧"))
    }

    @Test
    fun `nothing before the cursor means nothing to delete`() {
        assertEquals(0, charsToDelete(""))
    }

    @Test
    fun `a line starts at the beginning of the field or after a new line`() {
        assertTrue(startsLine(""))
        assertTrue(startsLine("\n"))
        assertFalse(startsLine("\uDD00"))
        assertFalse(startsLine(" "))
        assertFalse(startsLine(null))
    }
}
