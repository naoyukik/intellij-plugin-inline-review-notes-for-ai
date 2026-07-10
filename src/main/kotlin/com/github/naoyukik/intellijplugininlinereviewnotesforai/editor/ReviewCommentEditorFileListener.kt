package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class ReviewCommentEditorFileListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return@invokeLater
            val editor =
                EditorFactory.getInstance().getEditors(document, source.project).firstOrNull() ?: return@invokeLater
            CommentInlayManager.restoreComments(editor, source.project, file.path)
            ReviewCommentEditorTracker.track(editor)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) = Unit

    override fun selectionChanged(event: FileEditorManagerEvent) {
        ApplicationManager.getApplication().invokeLater {
            val newFile = event.newFile ?: return@invokeLater
            val document =
                FileDocumentManager.getInstance().getDocument(newFile) ?: return@invokeLater
            val editor =
                EditorFactory.getInstance().getEditors(document, event.manager.project).firstOrNull()
                    ?: return@invokeLater
            CommentInlayManager.restoreComments(editor, event.manager.project, newFile.path)
            ReviewCommentEditorTracker.track(editor)
        }
    }
}
