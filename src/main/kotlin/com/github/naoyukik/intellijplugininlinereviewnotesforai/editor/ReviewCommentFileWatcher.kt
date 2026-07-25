package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.MyBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import javax.swing.Timer

class ReviewCommentFileWatcher(private val project: Project) : BulkFileListener, BranchChangeListener {

    private var debounceTimer: Timer? = null
    private var pendingBranchChange = false

    override fun after(events: MutableList<out VFileEvent>) {
        val relevantEvents = events.filter { isRelevantEvent(it) }
        if (relevantEvents.isEmpty()) return
        debounceReload()
    }

    override fun branchWillChange(oldBranch: String) {
        pendingBranchChange = true
    }

    override fun branchHasChanged(newBranch: String) {
        pendingBranchChange = true
        reloadAllEditorsNow()
        showReloadNotification()
    }

    fun onStorageFileChanged() {
        debounceReload()
    }

    fun onStorageFileDeleted() {
        val editors = EditorFactory.getInstance().allEditors
            .filter { it.project == project }
        for (editor in editors) {
            CommentInlayManager.clearAllComments(editor)
        }
    }

    internal fun reloadAllEditorsNow() {
        pendingBranchChange = false
        val editors = EditorFactory.getInstance().allEditors
            .filter { it.project == project }
        for (editor in editors) {
            val file = FileDocumentManager.getInstance().getFile(editor.document) ?: continue
            CommentInlayManager.reloadComments(editor, project, file.path)
        }
    }

    private fun showReloadNotification() {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Inline Review Notes")
                .createNotification(
                    MyBundle.message("review.comments.reloaded"),
                    NotificationType.INFORMATION,
                )
                .notify(project)
        }
    }

    private fun debounceReload() {
        debounceTimer?.stop()
        debounceTimer = Timer(DEBOUNCE_DELAY_MS) {
            ApplicationManager.getApplication().invokeLater {
                reloadAllEditorsNow()
            }
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun isRelevantEvent(event: VFileEvent): Boolean {
        val path = event.path
        return isStoragePath(path)
    }

    private fun isStoragePath(path: String): Boolean {
        val normalized = path.replace("\\", "/")
        return normalized.contains("/.inline-review-notes/") &&
            normalized.endsWith(".json")
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300
    }
}
