@file:Suppress("ArgumentListWrapping")

package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewComment
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.UUID

class CommentInlayManagerStorageTest : BasePlatformTestCase() {

    private lateinit var projectRoot: Path
    private lateinit var rangeMarkerManager: RangeMarkerManager

    override fun setUp() {
        super.setUp()
        myFixture.configureByText("Foo.kt", "first line\nsecond line\nthird line\n")
        projectRoot = Path.of(project.basePath!!)
        rangeMarkerManager = RangeMarkerManager()
        CommentInlayManager.registerRangeMarkerManager(project, rangeMarkerManager)
        val storageDir = projectRoot.resolve(".inline-review-notes")
        if (storageDir.toFile().exists()) {
            storageDir.toFile().deleteRecursively()
        }
    }

    fun test_syncOnSave_updates_line_range_after_insert() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "sync test"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line\n")
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val saved = storage.load().comments.first()
        assertEquals(3, saved.lineStart)
        assertEquals(3, saved.lineEnd)
        assertFalse(saved.isOutdated)
    }

    fun test_edit_after_document_change_preserves_tracked_line_range() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "before edit"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line\n")
        }

        val blockRenderer = CommentInlayManager.activeBlockRenderer(editor)
        assertNotNull(blockRenderer)
        assertTrue(blockRenderer is CommentBlockRenderer)
        (blockRenderer as CommentBlockRenderer).onClick()
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "after edit"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val saved = storage.load().comments.first()
        assertEquals(3, saved.lineStart)
        assertEquals(3, saved.lineEnd)
        assertEquals("after edit", saved.comment)
    }

    fun test_syncOnSave_marks_isOutdated_when_range_deleted() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "delete range"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.deleteString(0, document.textLength)
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val saved = storage.load().comments.first()
        assertTrue(saved.isOutdated)
        assertEquals(2, saved.lineStart)
        assertEquals(2, saved.lineEnd)
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
        assertEquals("src/Foo.kt", savedComment.filePath)
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

    fun test_restoreComments_registers_all_comments_with_rangeMarkerManager() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile
        val storage = ReviewCommentStorage(projectRoot)

        val resolvedComment = ReviewComment(
            id = UUID.randomUUID().toString(),
            filePath = "src/Foo.kt",
            lineStart = 1,
            lineEnd = 1,
            comment = "resolved",
            createdAt = OffsetDateTime.now().toString(),
            resolvedAt = OffsetDateTime.now().toString(),
        )
        val unresolvedComment = ReviewComment(
            id = UUID.randomUUID().toString(),
            filePath = "src/Foo.kt",
            lineStart = 3,
            lineEnd = 3,
            comment = "unresolved",
            createdAt = OffsetDateTime.now().toString(),
        )
        storage.save(ReviewCommentDocument(comments = listOf(resolvedComment, unresolvedComment)))

        CommentInlayManager.restoreComments(editor, project, file.path)

        assertNotNull(rangeMarkerManager.resolveLineRange(resolvedComment.id))
        assertNotNull(rangeMarkerManager.resolveLineRange(unresolvedComment.id))
        assertEquals(1, CommentInlayManager.commentInlayCount(editor))
    }

    fun test_save_comment_replaces_marker_in_rangeMarkerManager() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            file.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "tracked"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val commentId = storage.load().comments.first().id
        assertNotNull(rangeMarkerManager.resolveLineRange(commentId))
    }

    fun test_delete_comment_disposes_marker_in_rangeMarkerManager() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            file.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "delete me"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val commentId = storage.load().comments.first().id
        assertNotNull(rangeMarkerManager.resolveLineRange(commentId))

        CommentInlayManager.activeBlockRenderer(editor)?.onClick()
        CommentInlayManager.activeInputPanel(editor)?.deleteButton?.doClick()

        assertNull(rangeMarkerManager.resolveLineRange(commentId))
    }

    fun test_reloadComments_reregisters_markers() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            file.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "reload test"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val commentId = storage.load().comments.first().id
        assertNotNull(rangeMarkerManager.resolveLineRange(commentId))

        CommentInlayManager.reloadComments(editor, project, file.path)

        assertNotNull(rangeMarkerManager.resolveLineRange(commentId))
    }

    fun test_releaseEditor_disposes_markers() {
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            file.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "release test"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val storage = ReviewCommentStorage(projectRoot)
        val commentId = storage.load().comments.first().id
        assertNotNull(rangeMarkerManager.resolveLineRange(commentId))

        CommentInlayManager.releaseEditor(editor)

        assertNull(rangeMarkerManager.resolveLineRange(commentId))
    }

    fun test_syncOnSave_single_line_after_lines_inserted_before() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "insert before"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line A\nnew line B\n")
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val saved = storage.load().comments.first()
        assertEquals(4, saved.lineStart)
        assertEquals(4, saved.lineEnd)
        assertFalse(saved.isOutdated)
    }

    fun test_syncOnSave_multi_line_after_lines_inserted_in_middle() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 3),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "multi range"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(document.getLineEndOffset(1) + 1, "inserted A\ninserted B\n")
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val saved = storage.load().comments.first()
        assertEquals(2, saved.lineStart)
        assertEquals(5, saved.lineEnd)
        assertFalse(saved.isOutdated)
    }

    fun test_syncOnSave_multiple_comments_all_updated() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(1, 1),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "first"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(3, 3),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "second"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line\n")
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val comments = storage.load().comments.sortedBy { it.lineStart }
        assertEquals(2, comments.size)
        assertEquals(2, comments[0].lineStart)
        assertEquals(2, comments[0].lineEnd)
        assertEquals(4, comments[1].lineStart)
        assertEquals(4, comments[1].lineEnd)
        assertFalse(comments[0].isOutdated)
        assertFalse(comments[1].isOutdated)
    }

    fun test_syncOnSave_mixed_resolved_unresolved() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 2),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "unresolved"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        val resolvedComment = ReviewComment(
            id = UUID.randomUUID().toString(),
            filePath = "src/Foo.kt",
            lineStart = 1,
            lineEnd = 1,
            comment = "resolved",
            createdAt = OffsetDateTime.now().toString(),
            resolvedAt = OffsetDateTime.now().toString(),
        )
        val existingDoc = storage.load()
        storage.save(existingDoc.copy(comments = existingDoc.comments + resolvedComment))
        rangeMarkerManager.register(resolvedComment.id, document, ReviewCommentLineRange(1, 1))

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line\n")
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val comments = storage.load().comments
        assertEquals(2, comments.size)
        comments.forEach { assertFalse(it.isOutdated) }
    }

    fun test_syncOnSave_multi_line_preserves_last_known_on_full_delete() {
        val editor = myFixture.editor
        val document = editor.document
        val storage = ReviewCommentStorage(projectRoot)

        CommentInlayManager.openInputPanel(
            editor,
            ReviewCommentLineRange(2, 4),
            project,
            myFixture.file.virtualFile.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "multi delete"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.deleteString(0, document.textLength)
        }

        rangeMarkerManager.syncOnSave(document, projectRoot, "src/Foo.kt")

        val saved = storage.load().comments.first()
        assertTrue(saved.isOutdated)
        assertEquals(2, saved.lineStart)
        assertEquals(4, saved.lineEnd)
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
