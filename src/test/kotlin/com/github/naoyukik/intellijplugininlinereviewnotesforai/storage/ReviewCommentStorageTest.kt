package com.github.naoyukik.intellijplugininlinereviewnotesforai.storage

import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewComment
import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.Charsets

class ReviewCommentStorageTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun sanitize_branch_name_and_resolve_storage_file_path() {
        val storage = ReviewCommentStorage(
            projectRoot = temporaryFolder.root.toPath(),
            branchNameProvider = { "feature/issue-3" },
        )

        assertEquals("feature_issue-3", storage.sanitizeBranchName("feature/issue-3"))
        assertEquals(
            temporaryFolder.root.toPath().resolve(".inline-review-notes").resolve("feature_issue-3.json"),
            storage.resolveStorageFilePath(),
        )
    }

    @Test
    fun save_writes_json_and_updates_gitignore_once() {
        val projectRoot = temporaryFolder.root.toPath()
        projectRoot.resolve(".gitignore").writeText("build/\n")
        val storage = ReviewCommentStorage(
            projectRoot = projectRoot,
            branchNameProvider = { "bug/fix#123" },
        )
        val document = ReviewCommentDocument(
            comments = listOf(
                ReviewComment(
                    id = "123e4567-e89b-12d3-a456-426614174000",
                    filePath = "src/main/kotlin/Foo.kt",
                    lineStart = 12,
                    lineEnd = 14,
                    comment = "ここを調整する。",
                    createdAt = "2026-06-19T10:00:00+09:00",
                    resolvedAt = null,
                ),
            ),
        )

        storage.save(document)
        storage.save(document)

        val storageFile = projectRoot.resolve(".inline-review-notes").resolve("bug_fix_123.json")
        assertTrue(storageFile.readText(Charsets.UTF_8).isNotBlank())
        assertEquals(document, storage.load())

        val gitignoreContent = projectRoot.resolve(".gitignore").readText()
        assertTrue(gitignoreContent.contains(".inline-review-notes/"))
        assertEquals(1, gitignoreContent.lineSequence().count { it == ".inline-review-notes/" })
    }

    @Test
    fun branch_name_falls_back_when_git_branch_is_unavailable() {
        val storage = ReviewCommentStorage(
            projectRoot = temporaryFolder.root.toPath(),
            branchNameProvider = { null },
        )

        assertEquals("default", storage.currentBranchName())
        assertEquals(
            temporaryFolder.root.toPath().resolve(".inline-review-notes").resolve("default.json"),
            storage.resolveStorageFilePath(),
        )
    }

    @Test
    fun load_ignores_unknown_keys() {
        val projectRoot = temporaryFolder.root.toPath()
        val storage = ReviewCommentStorage(
            projectRoot = projectRoot,
            branchNameProvider = { "main" },
        )
        val storageFile = projectRoot.resolve(".inline-review-notes").resolve("main.json")
        storageFile.parent.toFile().mkdirs()

        val expected = ReviewCommentDocument(
            comments = listOf(
                ReviewComment(
                    id = "123e4567-e89b-12d3-a456-426614174000",
                    filePath = "src/main/kotlin/Foo.kt",
                    lineStart = 12,
                    lineEnd = 14,
                    comment = "ここを調整する。",
                    createdAt = "2026-06-19T10:00:00+09:00",
                    resolvedAt = null,
                ),
            ),
        )

        storageFile.writeText(
            """
            {
              "comments": [
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "filePath": "src/main/kotlin/Foo.kt",
                  "lineStart": 12,
                  "lineEnd": 14,
                  "comment": "ここを調整する。",
                  "createdAt": "2026-06-19T10:00:00+09:00",
                  "resolvedAt": null
                }
              ],
              "unknownField": "should be ignored"
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        assertEquals(expected, storage.load())
    }

    @Test
    fun load_returns_empty_document_on_io_exception() {
        val storage = ReviewCommentStorage(
            projectRoot = temporaryFolder.root.toPath(),
            branchNameProvider = { "locked" },
            readTextFn = { throw IOException("boom") },
        )

        assertEquals(ReviewCommentDocument(), storage.load())
    }
}
