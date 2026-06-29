package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class ReviewCommentEditorFactoryListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        ReviewCommentEditorTracker.editorCreated(event)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        ReviewCommentEditorTracker.editorReleased(event)
    }
}
