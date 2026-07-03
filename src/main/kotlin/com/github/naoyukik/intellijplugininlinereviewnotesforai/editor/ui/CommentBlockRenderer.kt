package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor.ui

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import kotlin.math.ceil

class CommentBlockRenderer(
    val text: String,
    val onClick: () -> Unit,
) : EditorCustomElementRenderer {

    fun backgroundColor(): Color = backgroundColor
    fun borderColor(): Color = borderColor
    fun textColor(): Color = textColor

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val maxLineWidth = text.lineSequence()
            .map { measureRenderedTextWidth(it, renderFont(inlay)) }
            .maxOrNull()
            ?: 0
        return maxLineWidth + horizontalPadding * 2
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        val lineCount = text.lineSequence().count().coerceAtLeast(1)
        return (estimatedLineHeight * lineCount) + verticalPadding * 2
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes,
    ) {
        val graphics = g as Graphics2D
        val metrics = fontMetrics(inlay)
        val previousHint = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING)

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.color = backgroundColor
        graphics.fillRoundRect(
            targetRegion.x,
            targetRegion.y,
            targetRegion.width,
            targetRegion.height,
            arcSize,
            arcSize,
        )

        graphics.color = borderColor
        graphics.drawRoundRect(
            targetRegion.x,
            targetRegion.y,
            targetRegion.width - 1,
            targetRegion.height - 1,
            arcSize,
            arcSize,
        )

        graphics.color = textColor
        graphics.font = renderFont(inlay)

        var y = targetRegion.y + verticalPadding + metrics.ascent
        text.lineSequence().forEach { line ->
            graphics.drawString(line, targetRegion.x + horizontalPadding, y)
            y += metrics.height
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousHint)
    }

    private fun fontMetrics(inlay: Inlay<*>): FontMetrics =
        inlay.editor.contentComponent.getFontMetrics(renderFont(inlay))

    private fun renderFont(inlay: Inlay<*>) = inlay.editor.contentComponent.font.let { baseFont ->
        Font(Font.DIALOG, baseFont.style, baseFont.size)
    }

    companion object {
        private val backgroundColor = Color(43, 43, 43)
        private val borderColor = Color(78, 78, 78)
        private val textColor = Color(190, 190, 190)
        private const val horizontalPadding = 12
        private const val verticalPadding = 8
        private const val arcSize = 12
        private const val estimatedLineHeight = 18
    }
}

internal fun measureRenderedTextWidth(text: String, font: Font): Int =
    ceil(font.getStringBounds(text, fontRenderContext).width).toInt()

private val fontRenderContext = FontRenderContext(null, true, true)
