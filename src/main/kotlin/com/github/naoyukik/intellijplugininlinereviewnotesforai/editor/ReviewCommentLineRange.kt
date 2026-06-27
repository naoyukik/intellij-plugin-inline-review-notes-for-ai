package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

data class ReviewCommentLineRange(
    val startLine: Int,
    val endLine: Int,
) {

    init {
        require(startLine >= 1) { "startLine must be greater than or equal to 1." }
        require(endLine >= startLine) { "endLine must be greater than or equal to startLine." }
    }
}
