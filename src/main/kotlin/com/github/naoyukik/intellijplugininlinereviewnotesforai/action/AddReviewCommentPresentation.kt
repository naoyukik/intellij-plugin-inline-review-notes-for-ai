package com.github.naoyukik.intellijplugininlinereviewnotesforai.action

import com.github.naoyukik.intellijplugininlinereviewnotesforai.MyBundle
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ReviewCommentLineRange

object AddReviewCommentPresentation {

    fun buildPreviewMessage(filePath: String, lineRange: ReviewCommentLineRange): String =
        MyBundle.message(
            "add.review.comment.dialog.message",
            filePath,
            lineRange.startLine,
            lineRange.endLine,
        )
}
