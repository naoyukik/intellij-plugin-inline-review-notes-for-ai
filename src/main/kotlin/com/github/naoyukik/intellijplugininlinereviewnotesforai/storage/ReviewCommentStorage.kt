package com.github.naoyukik.intellijplugininlinereviewnotesforai.storage

import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import kotlinx.serialization.json.Json
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.Charsets
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class ReviewCommentStorage(
    private val projectRoot: Path,
    private val branchNameProvider: (() -> String?)? = null,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
        explicitNulls = true
    },
) {

    fun sanitizeBranchName(branchName: String): String = buildString(branchName.length) {
        branchName.forEach { character ->
            append(character.takeIf { it.isLetterOrDigit() || it == '-' || it == '_' } ?: '_')
        }
    }

    fun currentBranchName(): String =
        branchNameProvider?.let { normalizeBranchName(it()) }
            ?: normalizeBranchName(resolveGitBranch(projectRoot))

    fun resolveStorageFilePath(): Path = projectRoot
        .resolve(STORAGE_DIRECTORY)
        .resolve("${sanitizeBranchName(currentBranchName())}.json")

    private fun normalizeBranchName(branchName: String?): String =
        branchName?.takeUnless { it.isBlank() || it == DETACHED_HEAD } ?: DEFAULT_BRANCH_NAME

    fun load(): ReviewCommentDocument =
        resolveStorageFilePath()
            .readTextOrNull()
            ?.let { json.decodeFromString(ReviewCommentDocument.serializer(), it) }
            ?: ReviewCommentDocument()

    fun save(document: ReviewCommentDocument) {
        val storageFile = resolveStorageFilePath()
        storageFile.createParentDirectories()
        ensureGitignoreEntry()
        storageFile.writeText(
            json.encodeToString(ReviewCommentDocument.serializer(), document),
            Charsets.UTF_8,
        )
    }

    private fun ensureGitignoreEntry() {
        val gitignoreFile = projectRoot.resolve(GITIGNORE_FILE)
        val currentContent = gitignoreFile.readTextOrNull()

        when {
            currentContent == null -> gitignoreFile.writeText("$GITIGNORE_ENTRY${System.lineSeparator()}", Charsets.UTF_8)
            currentContent.lineSequence().any { it.trim() == GITIGNORE_ENTRY } -> Unit
            else -> gitignoreFile.writeText(
                buildString {
                    append(currentContent)
                    if (currentContent.isNotEmpty() && !currentContent.endsWith(System.lineSeparator())) {
                        append(System.lineSeparator())
                    }
                    append(GITIGNORE_ENTRY)
                    append(System.lineSeparator())
                },
                Charsets.UTF_8,
            )
        }
    }

    private fun resolveGitBranch(projectRoot: Path): String? =
        try {
            val process = ProcessBuilder("git", "-C", projectRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val output = reader.readText().trim()
                val exitCode = process.waitFor()

                if (exitCode != 0 || output.isBlank() || output == DETACHED_HEAD) {
                    null
                } else {
                    output
                }
            }
        } catch (_: Exception) {
            null
        }

    private fun Path.readTextOrNull(): String? =
        try {
            readText(Charsets.UTF_8)
        } catch (_: NoSuchFileException) {
            null
        }

    companion object {
        private const val STORAGE_DIRECTORY = ".inline-review-notes"
        private const val GITIGNORE_FILE = ".gitignore"
        private const val GITIGNORE_ENTRY = ".inline-review-notes/"
        private const val DEFAULT_BRANCH_NAME = "default"
        private const val DETACHED_HEAD = "HEAD"
    }
}
