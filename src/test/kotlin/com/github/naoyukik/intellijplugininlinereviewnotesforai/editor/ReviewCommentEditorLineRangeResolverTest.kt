package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReviewCommentEditorLineRangeResolverTest : BasePlatformTestCase() {

    fun test_empty_document_resolves_to_first_line() {
        myFixture.configureByText("Empty.txt", "")

        assertEquals(
            ReviewCommentLineRange(1, 1),
            ReviewCommentEditorLineRangeResolver.resolve(myFixture.editor),
        )
    }

    fun test_selection_ending_at_line_boundary_stays_on_starting_line() {
        myFixture.configureByText(
            "Foo.txt",
            "first line\nsecond line\nthird line\n",
        )

        val editor = myFixture.editor
        editor.selectionModel.setSelection(0, "first line\n".length)

        assertEquals(
            ReviewCommentLineRange(1, 1),
            ReviewCommentEditorLineRangeResolver.resolve(editor),
        )
    }

    fun test_selection_spanning_into_next_line_includes_both_lines() {
        myFixture.configureByText(
            "Foo.txt",
            "first line\nsecond line\nthird line\n",
        )

        val editor = myFixture.editor
        editor.selectionModel.setSelection(0, "first line\ns".length)

        assertEquals(
            ReviewCommentLineRange(1, 2),
            ReviewCommentEditorLineRangeResolver.resolve(editor),
        )
    }
}
