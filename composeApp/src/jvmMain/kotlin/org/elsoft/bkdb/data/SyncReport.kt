package org.elsoft.bkdb.data

data class SyncReport(val processed: Int, val failed: Int)

class CompositeSyncException(
    val successfulCount: Int,
    val failures: List<Throwable>
) : Exception("Sync completed with ${failures.size} failures.")
