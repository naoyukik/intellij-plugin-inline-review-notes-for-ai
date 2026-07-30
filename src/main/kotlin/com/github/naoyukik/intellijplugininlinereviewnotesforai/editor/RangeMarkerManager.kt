package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker

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
}
