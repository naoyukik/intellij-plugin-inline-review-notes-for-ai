package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewComment
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

class CommentInlayManagerTest : BasePlatformTestCase() {

    private val filePath: String by lazy { myFixture.file.virtualFile.path }

    fun test_open_input_panel_on_empty_file_creates_input_state() {
        myFixture.configureByText("Empty.kt", "")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)

        assertTrue(CommentInlayManager.hasInputPanel(editor))
    }

    fun test_save_comment_on_empty_file_creates_block_renderer() {
        myFixture.configureByText("Empty.kt", "x")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.deleteString(0, editor.document.textLength)
        }
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "空ファイルのコメント"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_restore_comment_on_empty_file_creates_block_renderer() {
        myFixture.configureByText("Empty.kt", "")

        val editor = myFixture.editor
        val storage = ReviewCommentStorage(Path.of(project.basePath!!))
        storage.save(
            ReviewCommentDocument(
                comments = listOf(
                    ReviewComment(
                        id = "empty-document-comment",
                        filePath = "src/Empty.kt",
                        lineStart = 1,
                        lineEnd = 1,
                        comment = "復元されたコメント",
                        createdAt = "2026-08-08T18:24:00+09:00",
                    ),
                ),
            ),
        )

        CommentInlayManager.releaseEditor(editor)
        CommentInlayManager.restoreComments(editor, project, filePath)

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("復元されたコメント", CommentInlayManager.activeBlockRenderer(editor)?.text)
    }

    fun test_open_input_panel_creates_input_state() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2), project, filePath)

        assertTrue(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("", CommentInlayManager.activeInputPanel(editor)?.textArea?.text)
    }

    fun test_save_replaces_input_panel_with_block_renderer() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "保存済み"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("保存済み", CommentInlayManager.activeBlockRenderer(editor)?.text)
    }

    fun test_cancel_discards_new_input_panel() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.cancelButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_cancel_restores_block_renderer_for_existing_comment() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "元コメント"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val blockRenderer = CommentInlayManager.activeBlockRenderer(editor)
        require(blockRenderer is CommentBlockRenderer)

        blockRenderer.onClick()

        assertTrue(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals("元コメント", CommentInlayManager.activeInputPanel(editor)?.textArea?.text)
    }

    fun test_multiple_saves_shows_all_block_renderers() {
        myFixture.configureByText("Foo.kt", "line1\nline2\nline3\nline4\n")
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "コメント1"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(3, 3), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "コメント2"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertEquals(2, CommentInlayManager.commentInlayCount(editor))
    }

    fun test_restore_comments_is_idempotent() {
        myFixture.configureByText("Foo.kt", "line1\nline2\nline3\n")
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "復元テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val countBefore = CommentInlayManager.commentInlayCount(editor)
        CommentInlayManager.restoreComments(editor, project, filePath)
        assertEquals(countBefore, CommentInlayManager.commentInlayCount(editor))
    }

    fun test_delete_discards_all_state() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(2, 2), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.deleteButton?.doClick()

        assertFalse(CommentInlayManager.hasInputPanel(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_reloadComments_restores_saved_comment() {
        myFixture.configureByText("Bar.kt", "line1\nline2\nline3\n")
        val editor = myFixture.editor
        CommentInlayManager.releaseEditor(editor)

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "リロードテスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        CommentInlayManager.reloadComments(editor, project, filePath)

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
        assertTrue(CommentInlayManager.commentInlayCount(editor) >= 1)
    }

    fun test_clearAllComments_removes_all_inlays() {
        myFixture.configureByText("Baz.kt", "line1\nline2\nline3\n")
        val editor = myFixture.editor
        CommentInlayManager.releaseEditor(editor)

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "クリアテスト1"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(3, 3), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "クリアテスト2"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.commentInlayCount(editor) >= 2)

        CommentInlayManager.clearAllComments(editor)

        assertEquals(0, CommentInlayManager.commentInlayCount(editor))
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }
}
