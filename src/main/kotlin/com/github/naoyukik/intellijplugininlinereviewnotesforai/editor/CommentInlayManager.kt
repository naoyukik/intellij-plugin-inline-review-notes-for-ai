package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentInputPanel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import java.util.IdentityHashMap

object CommentInlayManager {

    private val editorStates = IdentityHashMap<Editor, EditorState>()

    fun openInputPanel(
        editor: Editor,
        lineRange: ReviewCommentLineRange,
        existingComment: String? = null,
    ) {
        val state = editorStates.getOrPut(editor) { EditorState() }
        state.lineRange = lineRange
        state.existingComment = existingComment
        state.disposeBlockInlay()
        state.disposeInputInlay()

        val inputPanel = CommentInputPanel(
            existingComment = existingComment,
            onSave = { text -> saveComment(editor, text) },
            onCancel = { cancelComment(editor) },
            onDelete = { deleteComment(editor) },
        )
        state.inputPanel = inputPanel
        state.popup = createInputPopup(inputPanel)
        state.popup?.show(createPopupLocation(editor, lineRange))
    }

    fun hasInputPanel(editor: Editor): Boolean = editorStates[editor]?.inputPanel != null

    fun hasBlockRenderer(editor: Editor): Boolean = editorStates[editor]?.blockRenderer != null

    fun activeInputPanel(editor: Editor): CommentInputPanel? = editorStates[editor]?.inputPanel

    fun activeBlockRenderer(editor: Editor): CommentBlockRenderer? = editorStates[editor]?.blockRenderer

    private fun saveComment(editor: Editor, text: String) {
        val state = editorStates[editor] ?: return
        val lineRange = state.lineRange ?: return

        state.disposeInputInlay()
        state.disposeBlockInlay()

        val blockRenderer = CommentBlockRenderer(
            text = text,
            onClick = { openInputPanel(editor, lineRange, text) },
        )
        state.blockRenderer = blockRenderer
        state.existingComment = text
        state.blockInlay = addBlockInlay(
            editor = editor,
            lineRange = lineRange,
            renderer = blockRenderer,
        )
    }

    private fun cancelComment(editor: Editor) {
        val state = editorStates[editor] ?: return
        val lineRange = state.lineRange ?: return

        state.disposeInputInlay()
        if (state.existingComment != null) {
            val blockRenderer = CommentBlockRenderer(
                text = state.existingComment.orEmpty(),
                onClick = { openInputPanel(editor, lineRange, state.existingComment) },
            )
            state.blockRenderer = blockRenderer
            state.blockInlay = addBlockInlay(
                editor = editor,
                lineRange = lineRange,
                renderer = blockRenderer,
            )
        } else {
            state.disposeBlockInlay()
        }
    }

    private fun deleteComment(editor: Editor) {
        val state = editorStates[editor] ?: return
        state.disposeInputInlay()
        state.disposeBlockInlay()
        state.lineRange = null
        state.existingComment = null
    }

    private fun addBlockInlay(
        editor: Editor,
        lineRange: ReviewCommentLineRange,
        renderer: EditorCustomElementRenderer,
    ): Inlay<*>? {
        val offset = editor.document.getLineStartOffset(
            (lineRange.startLine - 1).coerceIn(0, editor.document.lineCount - 1),
        )
        val inlay = editor.inlayModel.addBlockElement(
            offset,
            false,
            false,
            0,
            renderer,
        )
        installInlayClickListener(editor)
        return inlay
    }

    private fun installInlayClickListener(editor: Editor) {
        val state = editorStates[editor] ?: return
        if (state.mouseListener != null) return

        val listener = object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                val mouseEvent = event.mouseEvent
                val clickPoint = mouseEvent.point
                val blockInlay = state.blockInlay ?: return
                val bounds = blockInlay.bounds ?: return
                if (bounds.contains(clickPoint)) {
                    val renderer = state.blockRenderer
                    if (renderer != null) {
                        renderer.onClick()
                    }
                }
            }
        }
        editor.addEditorMouseListener(listener)
        state.mouseListener = listener
    }

    private class EditorState {
        var lineRange: ReviewCommentLineRange? = null
        var existingComment: String? = null
        var inputPanel: CommentInputPanel? = null
        var blockRenderer: CommentBlockRenderer? = null
        var popup: JBPopup? = null
        var blockInlay: Inlay<*>? = null
        var mouseListener: EditorMouseListener? = null

        fun disposeInputInlay() {
            popup?.cancel()
            popup = null
            inputPanel = null
        }

        fun disposeBlockInlay() {
            blockInlay?.let(Disposer::dispose)
            blockInlay = null
            blockRenderer = null
        }
    }
}

private fun createInputPopup(panel: CommentInputPanel): JBPopup =
    JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, panel.textArea)
        .setRequestFocus(true)
        .setResizable(false)
        .setMovable(false)
        .setCancelOnClickOutside(false)
        .setCancelOnWindowDeactivation(false)
        .setCancelKeyEnabled(true)
        .createPopup()

private fun createPopupLocation(
    editor: Editor,
    lineRange: ReviewCommentLineRange,
): RelativePoint {
    val lineIndex = (lineRange.startLine - 1).coerceIn(0, editor.document.lineCount - 1)
    val visualPosition = editor.offsetToVisualPosition(editor.document.getLineStartOffset(lineIndex))
    val point = editor.visualPositionToXY(visualPosition)
    point.translate(0, editor.lineHeight)
    return RelativePoint(editor.contentComponent, point)
}
