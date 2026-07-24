package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager

class ReviewCommentEditorProjectActivity : ProjectActivity, DumbAware {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) {
                return@invokeLater
            }

            val fileWatcher = ReviewCommentFileWatcher(project)
            ApplicationManager.getApplication().messageBus.connect().subscribe(
                VirtualFileManager.VFS_CHANGES,
                fileWatcher,
            )

            EditorFactory.getInstance().allEditors
                .asSequence()
                .filter { it.project == project }
                .forEach(ReviewCommentEditorTracker::track)
        }, ModalityState.any())
    }
}
