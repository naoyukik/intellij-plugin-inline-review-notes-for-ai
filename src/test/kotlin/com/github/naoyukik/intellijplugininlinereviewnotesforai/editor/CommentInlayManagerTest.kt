package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CommentInlayManagerTest : BasePlatformTestCase() {

    fun test_open_input_panel_creates_input_state() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2))

        assertTrue(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("", CommentInlayManager.activeInputPanel(editor)?.textArea?.text)
    }

    fun test_save_replaces_input_panel_with_block_renderer() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2))
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "保存済み"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("保存済み", CommentInlayManager.activeBlockRenderer(editor)?.text)
    }

    fun test_cancel_discards_new_input_panel() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2))
        CommentInlayManager.activeInputPanel(editor)?.cancelButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_cancel_restores_block_renderer_for_existing_comment() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2))
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "元コメント"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val blockRenderer = CommentInlayManager.activeBlockRenderer(editor)
        require(blockRenderer is CommentBlockRenderer)

        blockRenderer.onClick()

        assertTrue(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("元コメント", CommentInlayManager.activeInputPanel(editor)?.textArea?.text)
    }

    fun test_delete_discards_all_state() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2))
        CommentInlayManager.activeInputPanel(editor)?.deleteButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }
}
