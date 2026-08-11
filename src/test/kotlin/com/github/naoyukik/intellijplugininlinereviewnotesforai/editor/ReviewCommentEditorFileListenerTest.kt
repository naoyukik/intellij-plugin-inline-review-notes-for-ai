package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewComment
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.UUID

class ReviewCommentEditorFileListenerTest : BasePlatformTestCase() {

    fun test_emptyFile_trackingWithoutJson_preparesGutterIcon() {
        myFixture.configureByText("Empty.kt", "")
        val editor = myFixture.editor

        ReviewCommentEditorTracker.release(editor)
        ReviewCommentEditorTracker.track(editor)

        assertTrue(editor.markupModel.allHighlighters.any { it.gutterIconRenderer != null })
    }

    fun test_fileOpened_restores_unresolved_comments() {
        myFixture.configureByText("Foo.kt", "line1\nline2\n")
        val editor = myFixture.editor
        val project = project
        val file = myFixture.file.virtualFile

        CommentInlayManager.openInputPanel(
            editor = editor,
            lineRange = ReviewCommentLineRange(1, 1),
            project = project,
            filePath = file.path,
        )
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "auto restore"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        CommentInlayManager.releaseEditor(editor)
        assertFalse(CommentInlayManager.hasBlockRenderer(editor))

        val listener = ReviewCommentEditorFileListener()
        listener.fileOpened(FileEditorManager.getInstance(project), file)
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_fileOpened_installs_gutter_icon() {
        myFixture.configureByText("Foo.kt", "line1\nline2\nline3\n")
        val editor = myFixture.editor
        val project = project
        val file = myFixture.file.virtualFile

        ReviewCommentEditorTracker.release(editor)

        val listener = ReviewCommentEditorFileListener()
        listener.fileOpened(FileEditorManager.getInstance(project), file)
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        val hasGutterIcon = editor.markupModel.allHighlighters.any {
            it.gutterIconRenderer != null
        }
        assertTrue(hasGutterIcon)
    }

    fun test_restoreComments_filters_resolved_comments() {
        myFixture.configureByText("Foo.kt", "line1\nline2\n")
        val editor = myFixture.editor
        val project = project
        val file = myFixture.file.virtualFile

        val resolvedComment = ReviewComment(
            id = UUID.randomUUID().toString(),
            filePath = "src/Foo.kt",
            lineStart = 1,
            lineEnd = 1,
            comment = "resolved",
            createdAt = OffsetDateTime.now().toString(),
            resolvedAt = OffsetDateTime.now().toString(),
        )
        val storage = ReviewCommentStorage(Path.of(project.basePath!!))
        storage.save(ReviewCommentDocument(comments = listOf(resolvedComment)))

        CommentInlayManager.restoreComments(editor, project, file.path)

        assertEquals(0, CommentInlayManager.commentInlayCount(editor))
    }
}
