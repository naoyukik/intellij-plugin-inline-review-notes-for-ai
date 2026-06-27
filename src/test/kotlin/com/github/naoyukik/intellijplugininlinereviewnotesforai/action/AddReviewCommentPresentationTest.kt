package com.github.naoyukik.intellijplugininlinereviewnotesforai.action

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ReviewCommentLineRange
import org.junit.Assert.assertEquals
import org.junit.Test

class AddReviewCommentPresentationTest {

    @Test
    fun build_preview_message_includes_file_path_and_line_range() {
        assertEquals(
            "File: src/main/kotlin/Foo.kt\nLine range: 3-5",
            AddReviewCommentPresentation.buildPreviewMessage(
                "src/main/kotlin/Foo.kt",
                ReviewCommentLineRange(3, 5),
            ),
        )
    }
}
