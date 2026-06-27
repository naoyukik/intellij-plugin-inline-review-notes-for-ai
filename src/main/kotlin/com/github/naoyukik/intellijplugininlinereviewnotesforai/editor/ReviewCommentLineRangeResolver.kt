package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

object ReviewCommentLineRangeResolver {

    fun resolveSelection(startLine: Int, endLine: Int): ReviewCommentLineRange =
        ReviewCommentLineRange(minOf(startLine, endLine), maxOf(startLine, endLine))

    fun resolveCaret(caretLine: Int): ReviewCommentLineRange =
        ReviewCommentLineRange(caretLine, caretLine)
}
