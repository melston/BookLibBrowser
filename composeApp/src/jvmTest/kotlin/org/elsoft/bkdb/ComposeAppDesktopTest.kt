package org.elsoft.bkdb

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppDesktopTest {

    @Test
    fun example() = runBlocking {
        //val testUrl = "dropbox:/Docs/EBooks/Misc/TxtStuff/Other/XXX/Unknown/Jacks Family Saga - Unknown.epub"
        //val testUrl = "dropbox:/Docs/EBooks/Misc/TxtStuff/Other/XXX/Unknown/Kathy Learns - Unknown.epub"
        val testUrl = "dropbox:/Docs/EBooks/Misc/TxtStuff/Other/XXX/Unknown/Me and Martha Jane - Unknown.epub"
        //val testUrl = "dropbox:/Docs/EBooks/Misc/TxtStuff/Other/XXX/Unknown/Me and.epub"

        val result = openEBook(testUrl)

        assertTrue("Opening should succeed", result.isSuccess)
        // Give okular a couple of seconds to appear
        println("Waiting for viewer to initialize")
        delay(2000)
    }
}