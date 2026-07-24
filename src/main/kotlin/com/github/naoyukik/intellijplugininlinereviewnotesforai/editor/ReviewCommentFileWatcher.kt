package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import javax.swing.Timer

class ReviewCommentFileWatcher(private val project: Project) : BulkFileListener {

    private var debounceTimer: Timer? = null
    private var pendingBranchChange = false

    override fun after(events: MutableList<out VFileEvent>) {
        val relevantEvents = events.filter { isRelevantEvent(it) }
        if (relevantEvents.isEmpty()) return

        if (relevantEvents.any { isBranchFilePath(it.path) }) {
            pendingBranchChange = true
            reloadAllEditorsNow()
            return
        }
        debounceReload()
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

    fun onBranchChanged() {
        pendingBranchChange = true
        debounceReload()
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
        return isStoragePath(path) || isBranchFilePath(path)
    }

    private fun isStoragePath(path: String): Boolean {
        val normalized = path.replace("\\", "/")
        return normalized.contains("/.inline-review-notes/") &&
            normalized.endsWith(".json")
    }

    private fun isBranchFilePath(path: String): Boolean {
        val normalized = path.replace("\\", "/")
        return normalized.endsWith("/.git/HEAD")
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300
    }
}
