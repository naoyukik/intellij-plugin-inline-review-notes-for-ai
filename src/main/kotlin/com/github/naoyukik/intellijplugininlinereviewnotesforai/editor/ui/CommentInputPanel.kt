package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class CommentInputPanel(
    existingComment: String? = null,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit,
    private val onDelete: () -> Unit,
) : JPanel(BorderLayout()) {

    val textArea: JTextArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        rows = defaultRows
        columns = defaultColumns
        border = BorderFactory.createEmptyBorder(componentPadding, componentPadding, componentPadding, componentPadding)
        text = existingComment.orEmpty()
    }

    val saveButton: JButton = JButton("Save")
    val cancelButton: JButton = JButton("Cancel")
    val deleteButton: JButton = JButton("Delete")

    init {
        border = BorderFactory.createEmptyBorder(componentPadding, componentPadding, componentPadding, componentPadding)

        add(JScrollPane(textArea), BorderLayout.CENTER)
        add(buttonPanel(), BorderLayout.SOUTH)

        saveButton.addActionListener {
            onSave(textArea.text)
        }
        cancelButton.addActionListener {
            onCancel()
        }
        deleteButton.addActionListener {
            onDelete()
        }

        deleteButton.isVisible = existingComment != null
    }

    private fun buttonPanel(): JPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT, buttonGap, 0)).apply {
            add(deleteButton)
            add(cancelButton)
            add(saveButton)
        }

    companion object {
        private const val componentPadding = 8
        private const val defaultRows = 5
        private const val defaultColumns = 40
        private const val buttonGap = 8
    }
}
