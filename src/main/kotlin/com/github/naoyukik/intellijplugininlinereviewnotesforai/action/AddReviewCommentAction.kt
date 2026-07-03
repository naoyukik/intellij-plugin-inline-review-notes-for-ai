package com.github.naoyukik.intellijplugininlinereviewnotesforai.action

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.CommentInlayManager
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ReviewCommentEditorLineRangeResolver
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware

class AddReviewCommentAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: editor?.let { FileDocumentManager.getInstance().getFile(it.document) }
        event.presentation.isEnabledAndVisible = editor != null && file != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val lineRange = ReviewCommentEditorLineRangeResolver.resolve(editor)
        CommentInlayManager.openInputPanel(editor, lineRange, null)
    }
}
