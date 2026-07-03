package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.MyBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

class ReviewCommentGutterIconRenderer(
    private val project: Project?,
    private val filePath: String,
    private val lineRange: ReviewCommentLineRange,
) : GutterIconRenderer() {

    override fun getIcon() = AllIcons.General.Add

    override fun getClickAction(): AnAction = object : AnAction(), DumbAware {
        override fun actionPerformed(e: AnActionEvent) {
            val editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR) ?: return
            CommentInlayManager.openInputPanel(editor, lineRange, null)
        }
    }

    override fun getTooltipText(): String = MyBundle.message("review.comment.gutter.tooltip")

    override fun getAlignment(): Alignment = Alignment.CENTER

    override fun isNavigateAction(): Boolean = true

    override fun equals(other: Any?): Boolean =
        other is ReviewCommentGutterIconRenderer &&
            project == other.project &&
            filePath == other.filePath &&
            lineRange == other.lineRange

    override fun hashCode(): Int = arrayOf(project, filePath, lineRange).contentHashCode()
}
