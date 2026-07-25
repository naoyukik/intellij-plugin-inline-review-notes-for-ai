package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class ReviewCommentFileWatcherTest : BasePlatformTestCase() {

    private val filePath: String by lazy { myFixture.file.virtualFile.path }

    override fun setUp() {
        super.setUp()
        myFixture.configureByText("WatcherTest.kt", "line1\nline2\n")
        CommentInlayManager.releaseEditor(myFixture.editor)
        CommentInlayManager.clearAllComments(myFixture.editor)
    }

    fun test_watcher_clears_only_on_current_storage_file_deletion() {
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "削除テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        val watcher = ReviewCommentFileWatcher(project)
        val otherBranchFile = myFixture.tempDirFixture.createFile(".inline-review-notes/other.json")
        watcher.after(mutableListOf(VFileDeleteEvent(null, otherBranchFile, false)))

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        val storageFile = ReviewCommentStorage(Path.of(project.basePath!!)).resolveStorageFilePath()
        val storageVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(storageFile)!!
        watcher.after(mutableListOf(VFileDeleteEvent(null, storageVirtualFile, false)))

        assertFalse(CommentInlayManager.hasBlockRenderer(editor))
    }

    fun test_watcher_reloads_on_git_head_change() {
        val editor = myFixture.editor

        CommentInlayManager.openInputPanel(editor, ReviewCommentLineRange(1, 1), project, filePath)
        CommentInlayManager.activeInputPanel(editor)?.textArea?.text = "ブランチ変更テスト"
        CommentInlayManager.activeInputPanel(editor)?.saveButton?.doClick()

        assertTrue(CommentInlayManager.hasBlockRenderer(editor))

        val watcher = ReviewCommentFileWatcher(project)
        val headFile = Path.of(project.basePath!!).resolve(".git").resolve("HEAD")
        Files.createDirectories(headFile.parent)
        Files.writeString(headFile, "ref: refs/heads/feature/reload-test\n")
        val headVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(headFile)!!
        watcher.after(mutableListOf(VFileDeleteEvent(null, headVirtualFile, false)))

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
