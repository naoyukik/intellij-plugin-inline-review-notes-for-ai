package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewCommentLineRangeResolverTest {

    @Test
    fun resolve_caret_line_as_single_line_range() {
        assertEquals(
            ReviewCommentLineRange(7, 7),
            ReviewCommentLineRangeResolver.resolveCaret(7),
        )
    }

    @Test
    fun resolve_selection_keeps_single_line_range() {
        assertEquals(
            ReviewCommentLineRange(12, 12),
            ReviewCommentLineRangeResolver.resolveSelection(12, 12),
        )
    }

    @Test
    fun resolve_selection_normalizes_line_order() {
        assertEquals(
            ReviewCommentLineRange(3, 5),
            ReviewCommentLineRangeResolver.resolveSelection(5, 3),
        )
    }
}
