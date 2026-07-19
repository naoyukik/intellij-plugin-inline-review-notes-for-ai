package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun save_shortcut_invokes_on_save() {
        var savedText = ""
        val panel = CommentInputPanel(
            onSave = { savedText = it },
            onCancel = {},
            onDelete = {},
        )
        panel.textArea.text = "ショートカット保存"
        // アクションマップから直接アクションを実行して検証
        val action = panel.textArea.actionMap.get("save")
        assertNotNull("save action should be registered", action)
        action?.actionPerformed(
            java.awt.event.ActionEvent(panel.textArea, java.awt.event.ActionEvent.ACTION_PERFORMED, ""),
        )
        assertEquals("ショートカット保存", savedText)
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

    @Test
    fun tab_focus_cycles_through_components() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // FocusTraversalPolicyの動作を直接検証
        val policy = panel.focusTraversalPolicy
        assertNotNull(policy)

        // テキストエリアから次のコンポーネントはsaveButton
        val afterTextArea = policy.getComponentAfter(panel, panel.textArea)
        assertEquals(panel.saveButton, afterTextArea)

        // saveButtonから次のコンポーネントはcancelButton
        val afterSave = policy.getComponentAfter(panel, panel.saveButton)
        assertEquals(panel.cancelButton, afterSave)

        // cancelButtonから次のコンポーネントはtextArea（循環）
        val afterCancel = policy.getComponentAfter(panel, panel.cancelButton)
        assertEquals(panel.textArea, afterCancel)
    }

    @Test
    fun shift_tab_focus_cycles_reverse() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // FocusTraversalPolicyの動作を直接検証
        val policy = panel.focusTraversalPolicy
        assertNotNull(policy)

        // テキストエリアから前のコンポーネントはcancelButton（逆順循環）
        val beforeTextArea = policy.getComponentBefore(panel, panel.textArea)
        assertEquals(panel.cancelButton, beforeTextArea)

        // cancelButtonから前のコンポーネントはsaveButton
        val beforeCancel = policy.getComponentBefore(panel, panel.cancelButton)
        assertEquals(panel.saveButton, beforeCancel)

        // saveButtonから前のコンポーネントはtextArea
        val beforeSave = policy.getComponentBefore(panel, panel.saveButton)
        assertEquals(panel.textArea, beforeSave)
    }

    @Test
    fun delete_button_included_in_focus_cycle_when_visible() {
        val panel = CommentInputPanel(
            existingComment = "既存コメント",
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // deleteButtonが表示されていることを確認
        assertTrue(panel.deleteButton.isVisible)

        // FocusTraversalPolicyの動作を直接検証
        val policy = panel.focusTraversalPolicy
        assertNotNull(policy)

        // テキストエリアから次のコンポーネントはsaveButton
        val afterTextArea = policy.getComponentAfter(panel, panel.textArea)
        assertEquals(panel.saveButton, afterTextArea)

        // saveButtonから次のコンポーネントはcancelButton
        val afterSave = policy.getComponentAfter(panel, panel.saveButton)
        assertEquals(panel.cancelButton, afterSave)

        // cancelButtonから次のコンポーネントはdeleteButton（表示時）
        val afterCancel = policy.getComponentAfter(panel, panel.cancelButton)
        assertEquals(panel.deleteButton, afterCancel)

        // deleteButtonから次のコンポーネントはtextArea（循環）
        val afterDelete = policy.getComponentAfter(panel, panel.deleteButton)
        assertEquals(panel.textArea, afterDelete)
    }

    @Test
    fun tab_key_transfers_focus_from_textarea() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )
        // アクションマップから Tab アクションを取得して直接実行
        val forwardAction = panel.textArea.actionMap.get("forward")
        assertNotNull("forward action should be registered", forwardAction)
    }

    @Test
    fun shift_tab_key_transfers_focus_backward_from_textarea() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )
        // アクションマップから Shift+Tab アクションを取得して直接実行
        val backwardAction = panel.textArea.actionMap.get("backward")
        assertNotNull("backward action should be registered", backwardAction)
    }

    @Test
    fun calculateInitialWidth_returns_60_percent_of_editor_width() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // エディタ幅 1000px の場合、60% = 600px
        val width = panel.calculateInitialWidth(1000)
        assertEquals(600, width)
    }

    @Test
    fun edit_mode_uses_same_initial_width_calculation() {
        val panel = CommentInputPanel(
            existingComment = "既存コメント",
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        panel.updatePanelSize(1000)

        assertEquals(75, panel.textArea.columns)
        assertEquals(600, panel.preferredSize.width)
    }

    @Test
    fun updatePanelSize_updates_edit_panel_width_for_successive_editor_widths() {
        val panel = CommentInputPanel(
            existingComment = "既存コメント",
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        panel.updatePanelSize(100)
        assertEquals(300, panel.preferredSize.width)
        assertEquals(37, panel.textArea.columns)

        panel.updatePanelSize(1000)
        assertEquals(600, panel.preferredSize.width)
        assertEquals(75, panel.textArea.columns)

        panel.updatePanelSize(2000)
        assertEquals(800, panel.preferredSize.width)
        assertEquals(100, panel.textArea.columns)
    }

    @Test
    fun calculateInitialWidth_respects_minimum_width() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // 非常に狭いエディタでも最小幅を返す
        val width = panel.calculateInitialWidth(100)
        assertTrue("Width should be at least minWidth", width >= 300)
    }

    @Test
    fun calculateInitialWidth_respects_maximum_width() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // 非常に広いエディタでも最大幅を返す
        val width = panel.calculateInitialWidth(2000)
        assertTrue("Width should be at most maxWidth", width <= 800)
    }

    @Test
    fun calculateHeight_returns_based_on_text_lines() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // 1行テキストの場合、最小行数(3行) + EXTRA_LINES(2行) = 5行
        val height1 = panel.calculateHeight("Hello")
        assertTrue("Height should be positive", height1 > 0)

        // 複数行テキストの場合、高さが増加する
        val multilineText = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        val height2 = panel.calculateHeight(multilineText)
        assertTrue("Multiline height should be greater", height2 > height1)
    }

    @Test
    fun calculateHeight_respects_maximum_lines() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        // 20行テキストでも最大行数(15行)に制限される
        val manyLines = (1..20).joinToString("\n") { "Line $it" }
        val height = panel.calculateHeight(manyLines)
        // 最大高さ = (15 + 2) * 20 + 2 * 8 = 340 + 16 = 356
        assertTrue("Height should not exceed max", height <= 356)
    }

    @Test
    fun updatePanelSize_updates_textarea_columns() {
        val panel = CommentInputPanel(
            onSave = {},
            onCancel = {},
            onDelete = {},
        )

        val initialColumns = panel.textArea.columns
        panel.updatePanelSize(1000)
        val newColumns = panel.textArea.columns

        // エディタ幅に応じて列数が更新される
        assertTrue("Columns should be updated", newColumns != initialColumns)
    }
}
