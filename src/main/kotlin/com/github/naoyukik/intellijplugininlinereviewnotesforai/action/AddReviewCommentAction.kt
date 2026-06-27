package com.github.naoyukik.intellijplugininlinereviewnotesforai.action

import com.github.naoyukik.intellijplugininlinereviewnotesforai.MyBundle
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ReviewCommentEditorLineRangeResolver
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ReviewCommentLineRange
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class AddReviewCommentAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = editor != null && file != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileDocumentManager.getInstance().getFile(editor.document)
            ?: return
        showPreviewDialog(editor.project, file, ReviewCommentEditorLineRangeResolver.resolve(editor))
    }

    private fun showPreviewDialog(project: Project?, file: VirtualFile, lineRange: ReviewCommentLineRange) {
        Messages.showInfoMessage(
            project,
            AddReviewCommentPresentation.buildPreviewMessage(file.path, lineRange),
            MyBundle.message("add.review.comment.dialog.title"),
        )
    }
}
