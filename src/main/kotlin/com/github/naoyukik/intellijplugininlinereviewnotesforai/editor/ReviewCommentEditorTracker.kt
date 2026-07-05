package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import java.util.IdentityHashMap

object ReviewCommentEditorTracker {

    private val editorStates = IdentityHashMap<Editor, EditorState>()

    fun track(editor: Editor) {
        if (FileDocumentManager.getInstance().getFile(editor.document) == null) {
            return
        }

        if (editorStates.containsKey(editor)) {
            refresh(editor)
            return
        }

        val state = EditorState()
        editorStates[editor] = state

        editor.caretModel.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    refresh(editor)
                }
            },
            state.disposable,
        )

        editor.selectionModel.addSelectionListener(
            object : SelectionListener {
                override fun selectionChanged(event: SelectionEvent) {
                    refresh(editor)
                }
            },
            state.disposable,
        )

        refresh(editor)

        val project = editor.project
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (project != null && file != null) {
            CommentInlayManager.restoreComments(editor, project, file.path)
        }
    }

    fun release(editor: Editor) {
        editorStates.remove(editor)?.dispose(editor)
    }

    fun editorCreated(event: EditorFactoryEvent) {
        track(event.editor)
    }

    fun editorReleased(event: EditorFactoryEvent) {
        release(event.editor)
    }

    private fun refresh(editor: Editor) {
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return release(editor)
        val lineRange = ReviewCommentEditorLineRangeResolver.resolve(editor)
        val state = editorStates[editor] ?: return

        if (state.filePath == file.path && state.lineRange == lineRange) {
            return
        }

        state.highlighter?.let { editor.markupModel.removeHighlighter(it) }

        val startLineIndex = (lineRange.startLine - 1).coerceIn(0, editor.document.lineCount - 1)
        val highlighter = editor.markupModel.addLineHighlighter(
            startLineIndex,
            HighlighterLayer.ADDITIONAL_SYNTAX,
            null,
        )
        highlighter.gutterIconRenderer = ReviewCommentGutterIconRenderer(editor.project, file.path, lineRange)

        state.filePath = file.path
        state.lineRange = lineRange
        state.highlighter = highlighter
    }

    private class EditorState {
        val disposable = Disposer.newDisposable()
        var filePath: String? = null
        var lineRange: ReviewCommentLineRange? = null
        var highlighter: RangeHighlighter? = null

        fun dispose(editor: Editor) {
            highlighter?.let { editor.markupModel.removeHighlighter(it) }
            highlighter = null
            Disposer.dispose(disposable)
        }
    }
}
