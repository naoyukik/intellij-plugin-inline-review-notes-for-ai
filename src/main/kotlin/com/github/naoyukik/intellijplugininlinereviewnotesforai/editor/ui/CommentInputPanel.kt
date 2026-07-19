package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import com.intellij.openapi.util.SystemInfo
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
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
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CommentInputPanel(
    existingComment: String? = null,
    private val onSave: (String) -> Unit,
    private val onCancel: () -> Unit,
    private val onDelete: () -> Unit,
) : JPanel(BorderLayout()) {

    val textArea: JTextArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        rows = minLines
        columns = defaultColumns
        border = BorderFactory.createEmptyBorder(componentPadding, componentPadding, componentPadding, componentPadding)
        text = existingComment.orEmpty()
    }

    val saveButton: JButton = JButton("Save")
    val cancelButton: JButton = JButton("Cancel")
    val deleteButton: JButton = JButton("Delete")

    private var configuredWidth: Int? = null
    private val focusPolicy = CommentFocusTraversalPolicy()

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

        // Tab キーのフォーカス移動: JTextArea のデフォルト動作（タブ文字挿入）を無効化
        val tabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
        textArea.inputMap.put(tabKeyStroke, "forward")
        textArea.actionMap.put(
            "forward",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val next = focusPolicy.getComponentAfter(this@CommentInputPanel, textArea)
                    next.requestFocusInWindow()
                }
            },
        )

        val shiftTabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)
        textArea.inputMap.put(shiftTabKeyStroke, "backward")
        textArea.actionMap.put(
            "backward",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val prev = focusPolicy.getComponentBefore(this@CommentInputPanel, textArea)
                    prev.requestFocusInWindow()
                }
            },
        )

        // Tab フォーカス移動
        focusTraversalPolicy = focusPolicy
        isFocusCycleRoot = true

        // テキスト変更時の高さ動的調整
        textArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateHeight()
            override fun removeUpdate(e: DocumentEvent) = updateHeight()
            override fun changedUpdate(e: DocumentEvent) = updateHeight()
        })
    }

    fun calculateInitialWidth(editorWidth: Int): Int {
        return (editorWidth * WIDTH_RATIO).toInt().coerceIn(minWidth, maxWidth)
    }

    fun calculateHeight(text: String): Int {
        val lineCount = text.lines().size.coerceAtLeast(1)
        val effectiveLines = (lineCount + EXTRA_LINES).coerceIn(minLines, maxLines)
        return effectiveLines * lineHeight + 2 * componentPadding
    }

    override fun getPreferredSize(): Dimension {
        val preferredSize = super.getPreferredSize()
        val width = configuredWidth ?: return preferredSize
        return Dimension(width, preferredSize.height)
    }

    fun updatePanelSize(editorWidth: Int) {
        val newWidth = calculateInitialWidth(editorWidth)
        val columns = (newWidth / charWidth).toInt().coerceAtLeast(minColumns)
        textArea.columns = columns
        configuredWidth = newWidth
        revalidate()
    }

    private fun updateHeight() {
        val lines = textArea.text.lines().size.coerceIn(minLines, maxLines)
        textArea.rows = lines
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
        private const val defaultColumns = 40
        private const val buttonGap = 8
        private const val WIDTH_RATIO = 0.6
        private const val minLines = 3
        private const val maxLines = 15
        private const val EXTRA_LINES = 2
        private const val minWidth = 300
        private const val maxWidth = 800
        private const val minColumns = 20
        private const val lineHeight = 20
        private const val charWidth = 8
    }
}
