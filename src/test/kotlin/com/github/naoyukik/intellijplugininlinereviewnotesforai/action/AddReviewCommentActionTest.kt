package com.github.naoyukik.intellijplugininlinereviewnotesforai.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AddReviewCommentActionTest : BasePlatformTestCase() {

    fun test_update_enables_action_when_virtual_file_is_present() {
        myFixture.configureByText("Foo.kt", "code")
        val editor = myFixture.editor
        val file = myFixture.file.virtualFile
        val action = AddReviewCommentAction()

        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.EDITOR.name -> editor
                CommonDataKeys.VIRTUAL_FILE.name -> file
                else -> null
            }
        }
        val event = AnActionEvent.createFromAnAction(action, null, "place", dataContext)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun test_update_enables_action_when_virtual_file_is_absent_but_resolvable_from_document() {
        myFixture.configureByText("Foo.kt", "code")
        val editor = myFixture.editor
        val action = AddReviewCommentAction()

        // CommonDataKeys.VIRTUAL_FILE is intentionally null, but EDITOR is provided
        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.EDITOR.name -> editor
                CommonDataKeys.VIRTUAL_FILE.name -> null
                else -> null
            }
        }
        val event = AnActionEvent.createFromAnAction(action, null, "place", dataContext)

        action.update(event)

        // Before fix, this assertion should fail because event.presentation.isEnabledAndVisible will be false
        assertTrue(event.presentation.isEnabledAndVisible)
    }
}
