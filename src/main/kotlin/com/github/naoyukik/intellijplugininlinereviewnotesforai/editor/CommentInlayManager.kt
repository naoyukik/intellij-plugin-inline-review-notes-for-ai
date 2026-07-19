@file:Suppress("ArgumentListWrapping")

package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentBlockRenderer
import com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui.CommentInputPanel
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewComment
import com.github.naoyukik.intellijplugininlinereviewnotesforai.storage.ReviewCommentStorage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.UUID

@Suppress("TooManyFunctions")
object CommentInlayManager {

    private val editorStates = IdentityHashMap<Editor, EditorState>()

    fun openInputPanel(
        editor: Editor,
        lineRange: ReviewCommentLineRange,
        project: Project,
        filePath: String,
        existingComment: String? = null,
        existingCommentId: String? = null,
    ) {
        val state = editorStates.getOrPut(editor) { EditorState() }
        state.project = project
        state.filePath = filePath
        state.currentEditLineRange = lineRange
        state.currentEditCommentId = existingCommentId
        state.disposeInputInlay()
        state.removeComponentListener(editor)

        if (existingCommentId != null) {
            state.disposeCommentInlay(existingCommentId)
        }

        val inputPanel = CommentInputPanel(
            existingComment = existingComment,
            onSave = { text -> saveComment(editor, text) },
            onCancel = { cancelComment(editor) },
            onDelete = { deleteComment(editor) },
        )
        inputPanel.setOnSizeChanged { packInputPopup(state) }

        // 初期サイズ設定
        val editorWidth = editor.contentComponent.width
        inputPanel.updatePanelSize(editorWidth)
        state.lastInputPanelEditorWidth = editorWidth

        state.inputPanel = inputPanel
        state.popup = createInputPopup(inputPanel)
        state.popup?.show(createPopupLocation(editor, lineRange))

        val componentListener = object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                resizeInputPopup(editor, state)
            }
        }
        editor.contentComponent.addComponentListener(componentListener)
        state.componentListener = componentListener
    }

    fun hasInputPanel(editor: Editor): Boolean = editorStates[editor]?.inputPanel != null

    fun hasBlockRenderer(editor: Editor): Boolean = editorStates[editor]?.commentInlays?.isNotEmpty() ?: false

    fun activeInputPanel(editor: Editor): CommentInputPanel? = editorStates[editor]?.inputPanel

    fun activeBlockRenderer(editor: Editor): CommentBlockRenderer? {
        val state = editorStates[editor] ?: return null
        return state.commentRenderers.entries.lastOrNull()?.value
    }

    fun commentInlayCount(editor: Editor): Int = editorStates[editor]?.commentInlays?.size ?: 0

    fun releaseEditor(editor: Editor) {
        editorStates.remove(editor)?.let { state ->
            state.commentInlays.values.forEach { Disposer.dispose(it) }
            state.removeComponentListener(editor)
            state.disposeInputInlay()
        }
    }

    private fun resizeInputPopup(editor: Editor, state: EditorState) {
        val panel = state.inputPanel ?: return
        val editorWidth = editor.contentComponent.width
        if (editorWidth == state.lastInputPanelEditorWidth) return
        panel.updatePanelSize(editorWidth)
        state.lastInputPanelEditorWidth = editorWidth
        packInputPopup(state)
    }

    private fun packInputPopup(state: EditorState) {
        state.popup
            ?.takeUnless { it.isDisposed }
            ?.pack(true, true)
    }

    @Suppress("LoopWithTooManyJumpStatements")
    fun restoreComments(editor: Editor, project: Project, filePath: String) {
        val state = editorStates.getOrPut(editor) { EditorState() }
        state.project = project

        if (state.filePath == filePath && state.commentInlays.isNotEmpty()) return

        if (state.filePath != filePath && state.filePath != null) {
            state.commentInlays.values.forEach { Disposer.dispose(it) }
            state.commentInlays.clear()
            state.commentRenderers.clear()
        }

        state.filePath = filePath

        val projectRoot = project.basePath?.let { Path.of(it) } ?: return
        val storage = ReviewCommentStorage(projectRoot)
        val document = storage.load()

        val relativePath = resolveRelativePath(editor, project)
        val fileComments = document.comments.filter {
            it.filePath == relativePath && it.resolvedAt == null
        }
        for (comment in fileComments) {
            val lineRange = ReviewCommentLineRange(comment.lineStart, comment.lineEnd)
            val renderer = CommentBlockRenderer(
                text = comment.comment,
                onClick = {
                    editorStates[editor]?.let { s ->
                        openInputPanel(
                            editor,
                            lineRange,
                            s.project!!,
                            s.filePath!!,
                            comment.comment,
                            comment.id,
                        )
                    }
                },
            )
            val inlay = addBlockInlay(editor, lineRange, renderer) ?: continue
            state.commentInlays[comment.id] = inlay
            state.commentRenderers[comment.id] = renderer
        }
    }

    @Suppress("ReturnCount")
    private fun saveComment(editor: Editor, text: String) {
        val state = editorStates[editor] ?: return
        val lineRange = state.currentEditLineRange ?: return
        val project = state.project ?: return

        val projectRoot = project.basePath?.let { Path.of(it) } ?: return
        val storage = ReviewCommentStorage(projectRoot)
        val document = storage.load()

        val editCommentId = state.currentEditCommentId
        val commentId: String
        val filteredComments: List<ReviewComment>

        if (editCommentId != null) {
            commentId = editCommentId
            filteredComments = document.comments.filter { it.id != commentId }
            state.disposeCommentInlay(commentId)
        } else {
            commentId = UUID.randomUUID().toString()
            filteredComments = document.comments
        }

        val relativePath = resolveRelativePath(editor, project) ?: return
        val reviewComment = ReviewComment(
            id = commentId,
            filePath = relativePath,
            lineStart = lineRange.startLine,
            lineEnd = lineRange.endLine,
            comment = text,
            createdAt = OffsetDateTime.now().toString(),
        )
        storage.save(document.copy(comments = filteredComments + reviewComment))

        state.disposeInputInlay()
        state.currentEditCommentId = null
        state.currentEditLineRange = null

        val blockRenderer = CommentBlockRenderer(
            text = text,
            onClick = {
                editorStates[editor]?.let { s ->
                    openInputPanel(editor, lineRange, s.project!!, s.filePath!!, text, commentId)
                }
            },
        )
        val inlay = addBlockInlay(editor, lineRange, blockRenderer) ?: return
        state.commentInlays[commentId] = inlay
        state.commentRenderers[commentId] = blockRenderer
    }

    private fun cancelComment(editor: Editor) {
        val state = editorStates[editor] ?: return
        val editCommentId = state.currentEditCommentId
        state.disposeInputInlay()

        if (editCommentId != null) {
            val project = state.project ?: return
            val filePath = state.filePath ?: return
            val projectRoot = project.basePath?.let { Path.of(it) } ?: return
            val storage = ReviewCommentStorage(projectRoot)
            val document = storage.load()
            val comment = document.comments.find { it.id == editCommentId }
            if (comment != null) {
                val restoreLineRange = ReviewCommentLineRange(comment.lineStart, comment.lineEnd)
                val renderer = CommentBlockRenderer(
                    text = comment.comment,
                    onClick = {
                        editorStates[editor]?.let { s ->
                            openInputPanel(
                                editor,
                                restoreLineRange,
                                s.project!!,
                                s.filePath!!,
                                comment.comment,
                                comment.id,
                            )
                        }
                    },
                )
                val inlay = addBlockInlay(editor, restoreLineRange, renderer) ?: return
                state.commentInlays[editCommentId] = inlay
                state.commentRenderers[editCommentId] = renderer
            }
        }

        state.currentEditCommentId = null
        state.currentEditLineRange = null
    }

    private fun deleteComment(editor: Editor) {
        val state = editorStates[editor] ?: return
        val project = state.project
        val commentId = state.currentEditCommentId

        if (project != null && commentId != null) {
            val projectRoot = project.basePath?.let { Path.of(it) }
            if (projectRoot != null) {
                val storage = ReviewCommentStorage(projectRoot)
                val document = storage.load()
                storage.save(document.copy(comments = document.comments.filter { it.id != commentId }))
            }
        }

        state.disposeInputInlay()
        if (commentId != null) {
            state.disposeCommentInlay(commentId)
        }
        state.currentEditCommentId = null
        state.currentEditLineRange = null
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
                for ((commentId, inlay) in state.commentInlays) {
                    val bounds = inlay.bounds
                    if (bounds != null && bounds.contains(clickPoint)) {
                        state.commentRenderers[commentId]?.onClick()
                        break
                    }
                }
            }
        }
        editor.addEditorMouseListener(listener)
        state.mouseListener = listener
    }

    private class EditorState {
        var project: Project? = null
        var filePath: String? = null

        val commentInlays: MutableMap<String, Inlay<*>> = LinkedHashMap()
        val commentRenderers: MutableMap<String, CommentBlockRenderer> = LinkedHashMap()

        var currentEditCommentId: String? = null
        var currentEditLineRange: ReviewCommentLineRange? = null
        var inputPanel: CommentInputPanel? = null
        var popup: JBPopup? = null
        var mouseListener: EditorMouseListener? = null
        var componentListener: ComponentAdapter? = null
        var lastInputPanelEditorWidth: Int? = null

        fun disposeInputInlay() {
            popup?.cancel()
            popup = null
            inputPanel = null
            lastInputPanelEditorWidth = null
        }

        fun removeComponentListener(editor: Editor) {
            componentListener?.let { editor.contentComponent.removeComponentListener(it) }
            componentListener = null
        }

        fun disposeCommentInlay(commentId: String) {
            commentInlays[commentId]?.let { Disposer.dispose(it) }
            commentInlays.remove(commentId)
            commentRenderers.remove(commentId)
        }
    }
}

private fun resolveRelativePath(editor: Editor, project: Project): String? {
    val vf = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    val base = project.basePath?.replace('\\', '/')?.trimEnd('/') ?: return null
    val filePath = vf.path.replace('\\', '/')
    val prefix = "$base/"
    return when {
        filePath.startsWith(prefix) -> filePath.substring(prefix.length)
        filePath.startsWith('/') -> filePath.substring(1)
        else -> null
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
