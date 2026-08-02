package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import java.nio.file.Path

@Suppress("TooManyFunctions")
class RangeMarkerManager : Disposable {

    data class ResolvedLineRange(
        val lineStart: Int,
        val lineEnd: Int,
    )

    private data class MarkerEntry(
        val document: Document,
        var marker: RangeMarker,
        var references: Int,
    )

    private val markers = mutableMapOf<String, MutableList<MarkerEntry>>()

    fun register(commentId: String, document: Document, lineRange: ReviewCommentLineRange) {
        val entries = markers.getOrPut(commentId) { mutableListOf() }
        val existing = entries.firstOrNull { it.document === document }
        if (existing != null) {
            existing.marker.dispose()
            existing.marker = createMarker(document, lineRange)
            existing.references++
        } else {
            entries.add(MarkerEntry(document, createMarker(document, lineRange), references = 1))
        }
    }

    fun replace(commentId: String, document: Document, lineRange: ReviewCommentLineRange) {
        val entries = markers.getOrPut(commentId) { mutableListOf() }
        val existing = entries.firstOrNull { it.document === document }
        if (existing != null) {
            existing.marker.dispose()
            existing.marker = createMarker(document, lineRange)
        } else {
            entries.add(MarkerEntry(document, createMarker(document, lineRange), references = 1))
        }
    }

    fun dispose(commentId: String) {
        markers.remove(commentId)?.forEach { it.marker.dispose() }
    }

    fun dispose(commentId: String, document: Document) {
        val entries = markers[commentId] ?: return
        val entry = entries.firstOrNull { it.document === document } ?: return
        entry.references--
        if (entry.references <= 0) {
            entry.marker.dispose()
            entries.remove(entry)
        }
        if (entries.isEmpty()) {
            markers.remove(commentId)
        }
    }

    fun resolveLineRange(commentId: String): ResolvedLineRange? {
        val marker = markers[commentId]?.firstOrNull()?.marker ?: return null
        return resolveLineRange(marker)
    }

    fun resolveLineRange(commentId: String, document: Document): ResolvedLineRange? {
        val marker = markers[commentId]
            ?.firstOrNull { it.document === document }
            ?.marker
            ?: return null
        return resolveLineRange(marker)
    }

    private fun resolveLineRange(marker: RangeMarker): ResolvedLineRange? {
        return if (marker.isValid) {
            ResolvedLineRange(
                lineStart = marker.document.getLineNumber(marker.startOffset) + 1,
                lineEnd = marker.document.getLineNumber(marker.endOffset) + 1,
            )
        } else {
            null
        }
    }

    fun isTracked(commentId: String, document: Document): Boolean =
        markers[commentId]?.any { it.document === document } == true

    fun syncOnSave(document: Document, projectRoot: Path, filePath: String) {
        val storage = ReviewCommentStorage(projectRoot)
        val doc = storage.load()
        val updatedComments = doc.comments.map { comment ->
            if (comment.filePath != filePath) return@map comment
            val resolved = resolveLineRange(comment.id, document)
            if (resolved != null) {
                comment.copy(lineStart = resolved.lineStart, lineEnd = resolved.lineEnd, isOutdated = false)
            } else if (isTracked(comment.id, document)) {
                comment.copy(isOutdated = true)
            } else {
                comment
            }
        }
        storage.save(doc.copy(comments = updatedComments))
    }

    override fun dispose() {
        markers.values.flatten().forEach { it.marker.dispose() }
        markers.clear()
    }

    private fun createMarker(document: Document, lineRange: ReviewCommentLineRange): RangeMarker {
        val startLineIndex = (lineRange.startLine - 1).coerceIn(0, document.lineCount - 1)
        val endLineIndex = (lineRange.endLine - 1).coerceIn(0, document.lineCount - 1)
        val marker = document.createRangeMarker(
            document.getLineStartOffset(startLineIndex),
            document.getLineEndOffset(endLineIndex),
        )
        marker.isGreedyToRight = true
        marker.isGreedyToLeft = false
        return marker
    }
}
