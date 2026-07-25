package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.MyBundle
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path
import javax.swing.Timer

class ReviewCommentFileWatcher(private val project: Project) : BulkFileListener, BranchChangeListener {

    private val projectRoot = project.basePath?.let(Path::of)
    private val storage = projectRoot?.let(::ReviewCommentStorage)
    private var debounceTimer: Timer? = null
    private var currentBranchName = storage?.currentBranchName()

    override fun after(events: MutableList<out VFileEvent>) {
        if (events.any { isGitHeadPath(it.path) } && reloadIfBranchChanged()) return

        val storageEvents = events.filter { isCurrentStoragePath(it.path) }
        if (storageEvents.isEmpty()) return

        if (storageEvents.any { it is VFileDeleteEvent }) {
            onStorageFileDeleted()
            return
        }
        debounceReload()
    }

    override fun branchWillChange(oldBranch: String) = Unit

    override fun branchHasChanged(newBranch: String) {
        reloadIfBranchChanged()
    }

    fun onStorageFileDeleted() {
        val editors = EditorFactory.getInstance().allEditors
            .filter { it.project == project }
        for (editor in editors) {
            CommentInlayManager.clearAllComments(editor)
        }
    }

    internal fun reloadAllEditorsNow() {
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
                ?.createNotification(
                    MyBundle.message("review.comments.reloaded"),
                    NotificationType.INFORMATION,
                )
                ?.notify(project)
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

    private fun reloadIfBranchChanged(): Boolean {
        val branchName = storage?.currentBranchName() ?: return false
        if (branchName == currentBranchName) return false

        currentBranchName = branchName
        reloadAllEditorsNow()
        showReloadNotification()
        return true
    }

    private fun isCurrentStoragePath(path: String): Boolean =
        storage?.resolveStorageFilePath()?.toString()?.normalizePath() == path.normalizePath()

    private fun isGitHeadPath(path: String): Boolean =
        projectRoot?.resolve(".git")?.resolve("HEAD")?.toString()?.normalizePath() == path.normalizePath()

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300
    }
}

private fun String.normalizePath(): String = replace("\\", "/")
