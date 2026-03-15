package org.elsoft.bkdb.utils

import org.elsoft.bkdb.EBook

object DuplicateFinder {
    fun findDuplicates(books: List<EBook>, threshold: Double = 0.85): List<List<EBook>> {
        val duplicates = mutableListOf<MutableList<EBook>>()
        val processed = mutableSetOf<Int>()

        for (i in books.indices) {
            if (books[i].id in processed) continue

            val group = mutableListOf(books[i])
            val title1 = normalize(books[i].title)

            for (j in i + 1 until books.size) {
                if (books[j].id in processed) continue

                val title2 = normalize(books[j].title)

                // Check if one starts with the other, or they are fuzzy-similar
                if (title1.startsWith(title2) || title2.startsWith(title1) ||
                    similarity(title1, title2) > threshold) {
//                    println("Duplicate $title1/$title2")
                    group.add(books[j])
                }
            }

            if (group.size > 1) {
                duplicates.add(group)
                processed.addAll(group.map { it.id })
            }
        }
        return duplicates
    }

    private fun normalize(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9]"), "") // Remove punctuation/spaces
            .replace(Regex("edition$|manning$|v[0-9]$"), "") // Strip common suffixes
            .trim()
    }

    private fun similarity(s1: String, s2: String): Double {
        // Simple Levenshtein implementation or use a library
        val distance = levenshtein(s1, s2)
        return 1.0 - (distance.toDouble() / maxOf(s1.length, s2.length))
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val v0 = IntArray(s2.length + 1) { it }
        val v1 = IntArray(s2.length + 1)

        for (i in s1.indices) {
            v1[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                v1[j + 1] = minOf(v1[j] + 1, minOf(v0[j + 1] + 1, v0[j] + cost))
            }
            v0.copyFrom(v1)
        }
        return v0[s2.length]
    }

    private fun IntArray.copyFrom(src: IntArray) {
        for (i in indices) this[i] = src[i]
    }
}
