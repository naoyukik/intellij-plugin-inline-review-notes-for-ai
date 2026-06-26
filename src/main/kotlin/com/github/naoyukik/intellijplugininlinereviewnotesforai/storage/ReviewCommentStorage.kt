package com.github.naoyukik.intellijplugininlinereviewnotesforai.storage

import com.github.naoyukik.intellijplugininlinereviewnotesforai.model.ReviewCommentDocument
import kotlinx.serialization.json.Json
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.Charsets
import java.io.IOException
import java.nio.file.Path

/**
 * インラインレビューコメントをJSON形式で保存・読み込みするためのストレージクラス。
 *
 * @property projectRoot プロジェクトのルートディレクトリ。
 * @property branchNameProvider ブランチ名を取得するためのプロバイダー（主にテスト用）。省略時はGitコマンドを使用して取得します。
 * @property json JSONのシリアライズ・デシリアライズ設定。
 */
class ReviewCommentStorage(
    private val projectRoot: Path,
    private val branchNameProvider: (() -> String?)? = null,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = true
    },
    private val readTextFn: (Path) -> String? = ::readTextOrNull,
) {

    /**
     * ブランチ名のサニタイズ。
     *
     * ブランチ名をファイル名として安全に使用できるように処理します。
     * 英数字、ハイフン、アンダースコア以外の文字はアンダースコアに置換されます。
     *
     * @param branchName サニタイズ対象のブランチ名。
     * @return サニタイズされたブランチ名。
     */
    fun sanitizeBranchName(branchName: String): String = buildString(branchName.length) {
        branchName.forEach { character ->
            append(character.takeIf { it.isLetterOrDigit() || it == '-' || it == '_' } ?: '_')
        }
    }

    /**
     * 現在のブランチ名の取得。
     *
     * 取得できない場合や detached HEAD の場合はデフォルトのブランチ名（"default"）を返します。
     *
     * @return 現在のブランチ名。
     */
    fun currentBranchName(): String =
        branchNameProvider?.let { normalizeBranchName(it()) }
            ?: normalizeBranchName(resolveGitBranch(projectRoot))

    /**
     * ストレージファイルのパス解決。
     *
     * 現在のブランチに対応するストレージファイルのパスを解決します。
     *
     * @return ストレージファイルのパス。
     */
    fun resolveStorageFilePath(): Path = projectRoot
        .resolve(STORAGE_DIRECTORY)
        .resolve("${sanitizeBranchName(currentBranchName())}.json")

    private fun normalizeBranchName(branchName: String?): String =
        branchName?.takeUnless { it.isBlank() || it == DETACHED_HEAD } ?: DEFAULT_BRANCH_NAME

    /**
     * レビューコメントの読み込み。
     *
     * ストレージファイルからレビューコメントを読み込みます。
     * ファイルが存在しない場合や読み込みに失敗した場合は、空の [ReviewCommentDocument] を返します。
     *
     * @return 読み込まれた [ReviewCommentDocument]。
     */
    fun load(): ReviewCommentDocument =
        try {
            resolveStorageFilePath()
                .let(readTextFn)
                ?.let { json.decodeFromString(ReviewCommentDocument.serializer(), it) }
                ?: ReviewCommentDocument()
        } catch (_: IOException) {
            ReviewCommentDocument()
        }

    /**
     * レビューコメントの保存。
     *
     * レビューコメントをストレージファイルに保存します。
     * 保存先のディレクトリが存在しない場合は作成し、`.gitignore` にストレージディレクトリが登録されているか確認します。
     *
     * @param document 保存するレビューコメントドキュメント。
     */
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
        val currentContent = readTextOrNull(gitignoreFile)

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

    companion object {
        private const val STORAGE_DIRECTORY = ".inline-review-notes"
        private const val GITIGNORE_FILE = ".gitignore"
        private const val GITIGNORE_ENTRY = ".inline-review-notes/"
        private const val DEFAULT_BRANCH_NAME = "default"
        private const val DETACHED_HEAD = "HEAD"
    }
}

private fun readTextOrNull(path: Path): String? =
    try {
        path.readText(Charsets.UTF_8)
    } catch (_: IOException) {
        null
    }
