package com.github.naoyukik.intellijplugininlinereviewnotesforai.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
                )
            )
        )

        val encoded = json.encodeToString(ReviewCommentDocument.serializer(), original)
        val decoded = json.decodeFromString(ReviewCommentDocument.serializer(), encoded)

        assertEquals(original, decoded)
    }
}
