package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.awt.Font
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CommentBlockRendererTest : BasePlatformTestCase() {

    fun test_renderer_implements_editor_custom_element_renderer() {
        val renderer = CommentBlockRenderer(
            text = "コメント",
            onClick = {},
        )

        assertTrue(
            renderer.javaClass.interfaces.any { it.simpleName == "EditorCustomElementRenderer" },
        )
    }

    fun test_width_and_height_are_positive() {
        myFixture.configureByText("Foo.kt", "code")

        val renderer = CommentBlockRenderer(
            text = "複数行\nのコメント",
            onClick = {},
        )

        assertTrue(renderer.calcWidthInPixels(dummyInlay()) > 0)
        assertTrue(renderer.calcHeightInPixels(dummyInlay()) > 0)
    }

    fun test_japanese_text_uses_wider_measurement_than_ascii() {
        val font = Font(Font.DIALOG, Font.PLAIN, 12)

        assertTrue(
            measureRenderedTextWidth("日本語", font) > measureRenderedTextWidth("AB", font),
        )
    }

    fun test_on_click_callback_is_retained() {
        var clickCount = 0
        val renderer = CommentBlockRenderer(
            text = "コメント",
            onClick = { clickCount += 1 },
        )

        renderer.onClick()

        assertEquals(1, clickCount)
    }

    fun test_dark_theme_palette_is_not_light_gray() {
        val renderer = CommentBlockRenderer(
            text = "コメント",
            onClick = {},
        )

        assertTrue(renderer.backgroundColor().red < 100)
        assertTrue(renderer.borderColor().red < 100)
        assertTrue(renderer.textColor().red in 170..210)
    }

    private fun dummyInlay(): Inlay<*> =
        Proxy.newProxyInstance(
            Inlay::class.java.classLoader,
            arrayOf(Inlay::class.java),
            DummyInvocationHandler(myFixture.editor),
        ) as Inlay<*>

    private class DummyInvocationHandler(
        private val editor: Editor,
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? =
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                Double::class.javaPrimitiveType -> 0.0
                Char::class.javaPrimitiveType -> '\u0000'
                Editor::class.java -> editor
                Void.TYPE -> null
                else -> null
            }
    }
}
