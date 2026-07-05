package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReviewCommentGutterIconRendererTest : BasePlatformTestCase() {

    fun test_click_action_opens_input_panel() {
        myFixture.configureByText("Foo.kt", "first line\nsecond line\n")

        val editor = myFixture.editor
        val file = myFixture.file.virtualFile
        val renderer = ReviewCommentGutterIconRenderer(
            project = project,
            filePath = file.path,
            lineRange = ReviewCommentLineRange(2, 2),
        )
        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.EDITOR.name -> editor
                CommonDataKeys.VIRTUAL_FILE.name -> file
                else -> null
            }
        }
        val event = AnActionEvent.createFromAnAction(renderer.getClickAction(), null, "place", dataContext)

        renderer.getClickAction().actionPerformed(event)

        assertTrue(CommentInlayManager.hasInputPanel(editor))
    }
}
