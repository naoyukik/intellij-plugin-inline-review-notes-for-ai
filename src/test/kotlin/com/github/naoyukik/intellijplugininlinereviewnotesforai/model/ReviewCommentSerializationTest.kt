package com.github.naoyukik.intellijplugininlinereviewnotesforai.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewCommentSerializationTest {

    private val json = Json {
        prettyPrint = false
    }

    @Test
    fun review_comment_document_round_trips_through_json() {
        val original = ReviewCommentDocument(
            comments = listOf(
                ReviewComment(
                    id = "123e4567-e89b-12d3-a456-426614174000",
                    filePath = "src/main/kotlin/Foo.kt",
                    lineStart = 42,
                    lineEnd = 45,
                    comment = "ここを修正すべきだ。",
                    createdAt = "2026-06-19T10:00:00+09:00",
                    resolvedAt = null,
                ),
            ),
        )

        val encoded = json.encodeToString(ReviewCommentDocument.serializer(), original)
        val decoded = json.decodeFromString(ReviewCommentDocument.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun review_comment_round_trips_with_isOutdated_true() {
        val original = ReviewComment(
            id = "id-001",
            filePath = "src/Foo.kt",
            lineStart = 10,
            lineEnd = 20,
            comment = "outdated comment",
            createdAt = "2026-07-01T00:00:00+09:00",
            isOutdated = true,
        )

        val encoded = json.encodeToString(ReviewComment.serializer(), original)
        val decoded = json.decodeFromString(ReviewComment.serializer(), encoded)

        assertTrue(decoded.isOutdated)
        assertEquals(original, decoded)
    }

    @Test
    fun review_comment_defaults_isOutdated_to_false_when_missing_from_json() {
        val jsonWithoutOutdated = """
            {"id":"id-002","filePath":"src/Bar.kt","lineStart":5,"lineEnd":5,"comment":"legacy","createdAt":"2026-06-01T00:00:00+09:00","resolvedAt":null}
        """.trimIndent()

        val decoded = json.decodeFromString(ReviewComment.serializer(), jsonWithoutOutdated)

        assertFalse(decoded.isOutdated)
    }
}
