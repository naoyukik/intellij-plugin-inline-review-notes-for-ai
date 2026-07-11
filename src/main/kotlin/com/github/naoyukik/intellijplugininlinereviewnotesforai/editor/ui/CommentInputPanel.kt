package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import com.intellij.openapi.util.SystemInfo
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.FlowLayout
import java.awt.FocusTraversalPolicy
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke

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

        // Save ショートカット: Ctrl+Enter (Windows/Linux) / Cmd+Enter (macOS)
        val modifier = if (SystemInfo.isMac) KeyEvent.META_DOWN_MASK else KeyEvent.CTRL_DOWN_MASK
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifier)
        textArea.inputMap.put(keyStroke, "save")
        textArea.actionMap.put(
            "save",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    saveButton.doClick()
                }
            },
        )

        // Tab フォーカス移動
        focusTraversalPolicy = CommentFocusTraversalPolicy()
        isFocusCycleRoot = true
    }

    private fun buttonPanel(): JPanel =
        JPanel(FlowLayout(FlowLayout.RIGHT, buttonGap, 0)).apply {
            add(deleteButton)
            add(cancelButton)
            add(saveButton)
        }

    private inner class CommentFocusTraversalPolicy : FocusTraversalPolicy() {
        private val components: List<Component>
            get() {
                val base = listOf<Component>(textArea, saveButton, cancelButton)
                return if (deleteButton.isVisible) base + deleteButton else base
            }

        override fun getComponentAfter(aContainer: Container, aComponent: Component): Component {
            val index = components.indexOf(aComponent)
            return if (index < components.size - 1) components[index + 1] else components.first()
        }

        override fun getComponentBefore(aContainer: Container, aComponent: Component): Component {
            val index = components.indexOf(aComponent)
            return if (index > 0) components[index - 1] else components.last()
        }

        override fun getFirstComponent(aContainer: Container): Component = components.first()

        override fun getLastComponent(aContainer: Container): Component = components.last()

        override fun getDefaultComponent(aContainer: Container): Component = components.first()
    }

    companion object {
        private const val componentPadding = 8
        private const val defaultRows = 5
        private const val defaultColumns = 40
        private const val buttonGap = 8
    }
}
