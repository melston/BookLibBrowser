package org.elsoft.bkdb.data.local

import java.util.UUID

// Transactions look like this:
//[
//    {
//        "bookId": 1234,
//        "type": "TOGGLE_READ",
//        "value": "true",
//        "timestamp": 1709734000000
//    },
//    {
//        "bookId": 567,
//        "type": "UPDATE_DESCRIPTION",
//        "value": "This is a classic space opera from 1974.",
//        "timestamp": 1709734050000
//    }
//]

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val bookId: Int,
    val type: TransactionType,
    val newValue: String, // Stringified value (e.g., "true", or the description text)
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    TOGGLE_READ,
    TOGGLE_FAVORITE,
    UPDATE_DESCRIPTION,
    DELETE_BOOK
}
