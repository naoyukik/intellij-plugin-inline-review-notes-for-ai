package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.editor.Editor

object ReviewCommentEditorLineRangeResolver {

    fun resolve(editor: Editor): ReviewCommentLineRange {
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            val startLine = editor.document.getLineNumber(selectionModel.selectionStart) + 1
            val endOffset = (selectionModel.selectionEnd - 1).coerceAtLeast(selectionModel.selectionStart)
            val endLine = editor.document.getLineNumber(endOffset) + 1
            ReviewCommentLineRangeResolver.resolveSelection(startLine, endLine)
        } else {
            ReviewCommentLineRangeResolver.resolveCaret(editor.caretModel.logicalPosition.line + 1)
        }
    }
}
