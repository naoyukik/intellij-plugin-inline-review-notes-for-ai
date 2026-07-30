package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import java.nio.file.Path

class RangeMarkerManager {

    data class ResolvedLineRange(
        val lineStart: Int,
        val lineEnd: Int,
    )

    private val markers = mutableMapOf<String, RangeMarker>()

    fun register(commentId: String, document: Document, lineRange: ReviewCommentLineRange) {
        val startLineIndex = (lineRange.startLine - 1).coerceIn(0, document.lineCount - 1)
        val endLineIndex = (lineRange.endLine - 1).coerceIn(0, document.lineCount - 1)
        val startOffset = document.getLineStartOffset(startLineIndex)
        val endOffset = document.getLineEndOffset(endLineIndex)
        val marker = document.createRangeMarker(startOffset, endOffset)
        marker.isGreedyToRight = true
        marker.isGreedyToLeft = false
        markers[commentId]?.dispose()
        markers[commentId] = marker
    }

    fun replace(commentId: String, document: Document, lineRange: ReviewCommentLineRange) {
        register(commentId, document, lineRange)
    }

    fun dispose(commentId: String) {
        markers.remove(commentId)?.dispose()
    }

    fun resolveLineRange(commentId: String): ResolvedLineRange? {
        val marker = markers[commentId] ?: return null
        return if (marker.isValid) {
            ResolvedLineRange(
                lineStart = marker.document.getLineNumber(marker.startOffset) + 1,
                lineEnd = marker.document.getLineNumber(marker.endOffset) + 1,
            )
        } else {
            null
        }
    }

    fun isTracked(commentId: String): Boolean = markers.containsKey(commentId)

    fun syncOnSave(@Suppress("UNUSED_PARAMETER") document: Document, projectRoot: Path, filePath: String) {
        val storage = ReviewCommentStorage(projectRoot)
        val doc = storage.load()
        val updatedComments = doc.comments.map { comment ->
            if (comment.filePath != filePath) return@map comment
            val resolved = resolveLineRange(comment.id)
            if (resolved != null) {
                comment.copy(lineStart = resolved.lineStart, lineEnd = resolved.lineEnd, isOutdated = false)
            } else if (isTracked(comment.id)) {
                comment.copy(isOutdated = true)
            } else {
                comment
            }
        }
        storage.save(doc.copy(comments = updatedComments))
    }
}
