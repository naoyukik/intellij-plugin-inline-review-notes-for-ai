package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path

class ReviewCommentEditorProjectActivity : ProjectActivity, DumbAware {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) {
                return@invokeLater
            }

            val fileWatcher = ReviewCommentFileWatcher(project)
            val appBus = ApplicationManager.getApplication().messageBus.connect(project)
            appBus.subscribe(VirtualFileManager.VFS_CHANGES, fileWatcher)
            val projectBus = project.messageBus.connect(project)
            projectBus.subscribe(BranchChangeListener.VCS_BRANCH_CHANGED, fileWatcher)

            val rangeMarkerManager = RangeMarkerManager()
            CommentInlayManager.rangeMarkerManager = rangeMarkerManager
            appBus.subscribe(
                FileDocumentManagerListener.TOPIC,
                object : FileDocumentManagerListener {
                    override fun beforeDocumentSaving(document: Document) {
                        val projectRoot = project.basePath?.let { Path.of(it) } ?: return
                        val relativePath = resolveRelativePathForSaving(document, project) ?: return
                        rangeMarkerManager.syncOnSave(document, projectRoot, relativePath)
                    }
                },
            )

            EditorFactory.getInstance().allEditors
                .asSequence()
                .filter { it.project == project }
                .forEach(ReviewCommentEditorTracker::track)
        }, ModalityState.any())
    }
}

private fun resolveRelativePathForSaving(document: Document, project: Project): String? {
    val vf = FileDocumentManager.getInstance().getFile(document) ?: return null
    val base = project.basePath?.replace('\\', '/')?.trimEnd('/') ?: return null
    val filePath = vf.path.replace('\\', '/')
    val prefix = "$base/"
    return when {
        filePath.startsWith(prefix) -> filePath.substring(prefix.length)
        filePath.startsWith('/') -> filePath.substring(1)
        else -> null
    }
}
