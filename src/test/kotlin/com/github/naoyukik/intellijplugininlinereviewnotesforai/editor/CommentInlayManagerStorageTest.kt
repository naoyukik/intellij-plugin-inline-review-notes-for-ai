package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

class CommentInlayManagerStorageTest : BasePlatformTestCase() {

    private lateinit var projectRoot: Path

    override fun setUp() {
        super.setUp()
        myFixture.configureByText("Foo.kt", "first line\nsecond line\nthird line\n")
        projectRoot = Path.of(project.basePath!!)
        val storageDir = projectRoot.resolve(".inline-review-notes")
        if (storageDir.toFile().exists()) {
            storageDir.toFile().deleteRecursively()
        }
    }

    fun test_save_persists_comment_to_storage() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor = editor,
            lineRange = ReviewCommentLineRange(2, 2),
            project = project,
            filePath = file.path,
            existingComment = null,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "保存済みコメント"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val document = storage.load()
        assertTrue(document.comments.isNotEmpty())

        val savedComment = document.comments.first()
        assertEquals(file.path, savedComment.filePath)
        assertEquals(2, savedComment.lineStart)
        assertEquals(2, savedComment.lineEnd)
        assertEquals("保存済みコメント", savedComment.comment)
        assertNotNull(savedComment.id)
        assertFalse(savedComment.id.isEmpty())
        assertNotNull(savedComment.createdAt)
        assertFalse(savedComment.createdAt.isEmpty())
    }

    fun test_delete_removes_comment_from_storage() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor = editor,
            lineRange = ReviewCommentLineRange(2, 2),
            project = project,
            filePath = file.path,
            existingComment = null,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "削除対象"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        assertEquals(1, storage.load().comments.size)

        val blockRenderer = CommentInlayManager.activeBlockRenderer(editor)
        assertNotNull(blockRenderer)
        assertTrue(blockRenderer is CommentBlockRenderer)
        (blockRenderer as CommentBlockRenderer).onClick()

        CommentInlayManager.activeInputPanel(editor)?.deleteButton?.doClick()

        assertEquals(0, storage.load().comments.size)
    }

    fun test_save_with_existing_comment_updates_storage() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor = editor,
            lineRange = ReviewCommentLineRange(2, 2),
            project = project,
            filePath = file.path,
            existingComment = null,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "初回保存"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val blockRenderer = CommentInlayManager.activeBlockRenderer(editor)
        assertNotNull(blockRenderer)
        assertTrue(blockRenderer is CommentBlockRenderer)
        (blockRenderer as CommentBlockRenderer).onClick()

        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "更新後テキスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val document = storage.load()
        assertEquals(1, document.comments.size)
        assertEquals("更新後テキスト", document.comments.first().comment)
    }
}
