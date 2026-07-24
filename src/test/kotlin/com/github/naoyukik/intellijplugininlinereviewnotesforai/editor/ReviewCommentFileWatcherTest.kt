package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReviewCommentFileWatcherTest : BasePlatformTestCase() {

    private val filePath: String by lazy { myFixture.file.virtualFile.path }

    override fun setUp() {
        super.setUp()
        myFixture.configureByText("WatcherTest.kt", "line1\nline2\n")
        CommentInlayManager.releaseEditor(myFixture.editor)
        CommentInlayManager.clearAllComments(myFixture.editor)
    }

    fun test_watcher_clears_on_file_deletion() {
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "削除テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        val watcher = ReviewCommentFileWatcher(project)
        watcher.onStorageFileDeleted()

        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_reloadComments_restores_after_save() {
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "ブランチ切替テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        CommentInlayManager.reloadComments(editor, project, filePath)

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
        assertTrue(CommentInlayManager.commentInlayCount(editor) >= 1)
    }

    fun test_clearAllComments_removes_all() {
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        CommentInlayManager.clearAllComments(editor)

        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
        assertEquals(0, CommentInlayManager.commentInlayCount(editor))
    }
}
