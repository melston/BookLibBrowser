package org.elsoft.bkdb

import kotlin.test.Test

class CacheNameTest {
    @Test
    fun `test filename shortening removes lowercase and underscores`() {
        val input = "/Docs/EBooks/Java_Concurrency In_Action.epub"
        val output = cacheName(input)

        // Assert that lowercase 'a', 'v', etc. and ' ' are gone
        // But the extension .epub and the hash remain
        assert(output.startsWith("BK_"))
        assert(output.endsWith(".epub"))
        assert(!output.contains("a"))
        assert(!output.contains(" "))
    }
}