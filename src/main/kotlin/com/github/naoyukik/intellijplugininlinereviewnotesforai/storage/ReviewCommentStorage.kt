package com.github.naoyukik.intellijplugininlinereviewnotesforai.storage

import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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

    fun sanitizeBranchName(branchName: String): String = branchName.map { character ->
        if (character.isLetterOrDigit() || character == '-' || character == '_') {
            character
        } else {
            '_'
        }
    }.joinToString("")

    fun currentBranchName(): String {
        val providedBranchName = branchNameProvider?.invoke()
        if (branchNameProvider != null) {
            return normalizeBranchName(providedBranchName)
        }

        return normalizeBranchName(resolveGitBranch(projectRoot))
    }

    fun resolveStorageFilePath(): Path = projectRoot
        .resolve(STORAGE_DIRECTORY)
        .resolve("${sanitizeBranchName(currentBranchName())}.json")

    private fun normalizeBranchName(branchName: String?): String =
        branchName?.takeUnless { it.isBlank() || it == DETACHED_HEAD } ?: DEFAULT_BRANCH_NAME

    fun load(): ReviewCommentDocument {
        val storageFile = resolveStorageFilePath()
        if (Files.notExists(storageFile)) {
            return ReviewCommentDocument()
        }
        return json.decodeFromString(ReviewCommentDocument.serializer(), Files.readString(storageFile))
    }

    fun save(document: ReviewCommentDocument) {
        val storageFile = resolveStorageFilePath()
        Files.createDirectories(storageFile.parent)
        ensureGitignoreEntry()
        Files.writeString(
            storageFile,
            json.encodeToString(ReviewCommentDocument.serializer(), document),
            StandardCharsets.UTF_8,
        )
    }

    private fun ensureGitignoreEntry() {
        val gitignoreFile = projectRoot.resolve(GITIGNORE_FILE)
        val entry = GITIGNORE_ENTRY

        if (Files.notExists(gitignoreFile)) {
            Files.writeString(gitignoreFile, entry + System.lineSeparator(), StandardCharsets.UTF_8)
            return
        }

        val currentContent = Files.readString(gitignoreFile)
        if (currentContent.lineSequence().any { it.trim() == entry }) {
            return
        }

        val updatedContent = buildString {
            append(currentContent)
            if (currentContent.isNotEmpty() && !currentContent.endsWith(System.lineSeparator())) {
                append(System.lineSeparator())
            }
            append(entry)
            append(System.lineSeparator())
        }
        Files.writeString(gitignoreFile, updatedContent, StandardCharsets.UTF_8)
    }

    private fun resolveGitBranch(projectRoot: Path): String? {
        return try {
            val process = ProcessBuilder("git", "-C", projectRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader -> reader.readText().trim() }
            val exitCode = process.waitFor()

            if (exitCode != 0 || output.isBlank() || output == DETACHED_HEAD) {
                null
            } else {
                output
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val STORAGE_DIRECTORY = ".inline-review-notes"
        private const val GITIGNORE_FILE = ".gitignore"
        private const val GITIGNORE_ENTRY = ".inline-review-notes/"
        private const val DEFAULT_BRANCH_NAME = "default"
        private const val DETACHED_HEAD = "HEAD"
    }
}
