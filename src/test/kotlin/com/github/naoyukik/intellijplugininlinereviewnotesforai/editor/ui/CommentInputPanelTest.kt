package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentInputPanelTest {

    @Test
    fun panel_exposes_expected_components() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        assertTrue(panel.textArea.text.isEmpty())
        assertTrue(panel.saveButton.text.contains("Save", ignoreCase = true))
        assertTrue(panel.cancelButton.text.contains("Cancel", ignoreCase = true))
        assertTrue(panel.deleteButton.text.contains("Delete", ignoreCase = true))
    }

    @Test
    fun save_click_invokes_on_save_with_text() {
        var savedText = ""
        val panel = CommentInputPanel(
            onSave = { savedText = it },
            onCancel = {},
            onDelete = {},
        )

        panel.textArea.text = "保存する内容"
        panel.saveButton.doClick()

        assertEquals("保存する内容", savedText)
    }

    @Test
    fun cancel_click_invokes_on_cancel() {
        var cancelCount = 0
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = { cancelCount += 1 },
            onDelete = {},
        )

        panel.cancelButton.doClick()

        assertEquals(1, cancelCount)
    }

    @Test
    fun delete_click_invokes_on_delete() {
        var deleteCount = 0
        val panel = CommentInputPanel(
            existingComment = "既存コメント",
            onSave = {},
            onCancel = {},
            onDelete = { deleteCount += 1 },
        )

        panel.deleteButton.doClick()

        assertEquals(1, deleteCount)
    }

    @Test
    fun delete_button_is_hidden_for_new_comment() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        assertFalse(panel.deleteButton.isVisible)
    }

    @Test
    fun edit_mode_prefills_text_and_shows_delete_button() {
        val panel = CommentInputPanel(
            existingComment = "既存コメント",
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        assertEquals("既存コメント", panel.textArea.text)
        assertTrue(panel.deleteButton.isVisible)
    }
}
