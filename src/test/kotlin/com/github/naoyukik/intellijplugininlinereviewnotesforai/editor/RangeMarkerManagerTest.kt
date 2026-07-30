package com.github.naoyukik.intellijplugininlinereviewnotesforai.editor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RangeMarkerManagerTest : BasePlatformTestCase() {

    private val manager = RangeMarkerManager()

    fun test_register_then_resolve_returns_registered_range() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(2, 4))

        val resolved = manager.resolveLineRange("id-1")
        assertNotNull(resolved)
        assertEquals(2, resolved!!.lineStart)
        assertEquals(4, resolved.lineEnd)
    }

    fun test_replaces_marker_for_same_id() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(1, 1))
        manager.replace("id-1", document, ReviewCommentLineRange(3, 5))

        val resolved = manager.resolveLineRange("id-1")
        assertNotNull(resolved)
        assertEquals(3, resolved!!.lineStart)
        assertEquals(5, resolved.lineEnd)
    }

    fun test_dispose_removes_marker() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(1, 1))
        manager.dispose("id-1")

        assertNull(manager.resolveLineRange("id-1"))
    }

    fun test_resolve_returns_null_for_unregistered_id() {
        assertNull(manager.resolveLineRange("nonexistent"))
    }

    fun test_tracks_range_after_lines_inserted_before() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(2, 3))
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "inserted line\n")
        }

        val resolved = manager.resolveLineRange("id-1")
        assertNotNull(resolved)
        assertEquals(3, resolved!!.lineStart)
        assertEquals(4, resolved.lineEnd)
    }

    fun test_tracks_range_after_lines_deleted_before() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(3, 4))
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.deleteString(0, document.getLineEndOffset(1) + 1)
        }

        val resolved = manager.resolveLineRange("id-1")
        assertNotNull(resolved)
        assertEquals(1, resolved!!.lineStart)
        assertEquals(2, resolved.lineEnd)
    }

    fun test_tracks_multi_line_range_after_lines_inserted_before() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(2, 4))
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "line A\nline B\n")
        }

        val resolved = manager.resolveLineRange("id-1")
        assertNotNull(resolved)
        assertEquals(4, resolved!!.lineStart)
        assertEquals(6, resolved.lineEnd)
    }

    fun test_returns_null_when_entire_range_deleted() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        val startOffset = document.getLineStartOffset(1)
        val endOffset = document.getLineEndOffset(3) + 1

        manager.register("id-1", document, ReviewCommentLineRange(2, 4))
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.deleteString(startOffset, endOffset)
        }

        assertNull(manager.resolveLineRange("id-1"))
    }

    fun test_multiple_comments_tracked_independently() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\nline4\nline5\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(1, 1))
        manager.register("id-2", document, ReviewCommentLineRange(3, 3))

        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(0, "new line\n")
        }

        assertEquals(2, manager.resolveLineRange("id-1")!!.lineStart)
        assertEquals(4, manager.resolveLineRange("id-2")!!.lineStart)
    }

    fun test_register_reuses_storage_for_same_id() {
        myFixture.configureByText("test.kt", "line1\nline2\nline3\n")
        val document = myFixture.editor.document

        manager.register("id-1", document, ReviewCommentLineRange(1, 1))

        manager.register("id-1", document, ReviewCommentLineRange(2, 2))

        val marker2 = manager.resolveLineRange("id-1")
        assertNotNull(marker2)
        assertEquals(2, marker2!!.lineStart)
    }
}
