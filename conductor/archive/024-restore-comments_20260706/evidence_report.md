# 実装前調査レポート (evidence_report.md)

## Phase 1: Discovery Summary

### 要求再記述
保存済みインラインレビューコメントを、ファイルを開いた際やエディタタブ切り替え時に自動復元する。
また、コメントの `filePath` をプロジェクトルートからの相対パスに統一する。

### 制約
- 既存のコメント保存・復元・編集・削除の動作を壊さない
- 復元は未解決コメント（`resolvedAt == null`）のみ対象
- 二重復元を防止する

### 成功条件
- ファイルを閉じて再度開くと未解決コメントが復元される
- タブ切り替えでコメントが二重に表示されない
- `filePath` が相対パスで保存・比較される

## Phase 2: Codebase Findings

### 既存パターン
- **エディタイベント検知**: `EditorFactoryListener` 経由で `ReviewCommentEditorTracker.track()` が呼ばれる（`plugin.xml` に `applicationListeners` として登録済み）
- **インレイ描画**: `CommentInlayManager` が一元管理。`EditorState` を `IdentityHashMap<Editor, EditorState>` で保持。`restoreComments()` は `commentInlays.isNotEmpty()` でガードし冪等性を担保
- **ストレージ**: `ReviewCommentStorage(projectRoot)` が `.inline-review-notes/{branch}.json` を読み書き

### 現在の乖離
| 項目 | 現状 | 仕様上の要求 |
|---|---|---|
| `filePath` | 絶対パス (`file.path`) | プロジェクト相対パス |
| 復元トリガー | `EditorFactoryListener.editorCreated()` のみ | ファイルオープン・タブ切替でも復元 |
| 復元フィルタ | `filePath` 一致のみ（`resolvedAt` 未フィルタ） | 未解決 (`resolvedAt == null`) のみ |

### 変更影響範囲
- **Phase 1 (相対化)**: `CommentInlayManager.kt`, `ReviewCommentEditorTracker.kt`, `AddReviewCommentAction.kt`, `ReviewCommentGutterIconRenderer.kt`, `AddReviewCommentPresentation.kt`, `CommentInlayManagerStorageTest.kt`, `CommentInlayManagerTest.kt`
- **Phase 2 (復元)**: 新規クラス `ReviewCommentEditorFileListener.kt`, `plugin.xml`, `CommentInlayManager.kt`

## Phase 3: Architecture Design

### 案1: 相対パス変換の共通化

**方針**: `project.basePath` から相対パスを計算する拡張関数（またはユーティリティ関数）を `CommentInlayManager` 内に追加。絶対パスを受け取る既存APIは維持し、内部で相対化する。

```kotlin
private fun Project.toRelativePath(absolutePath: String): String? {
    val base = basePath ?: return null
    return Path.of(base).relativize(Path.of(absolutePath)).toString()
}
```

**根拠**:
- 最小限の変更で既存の呼び出し元（`ReviewCommentEditorTracker`, `AddReviewCommentAction`）に影響を与えない
- `CommentInlayManager` 内で一元管理できる

### 案2: FileEditorManagerListener の実装

**方針**: `ReviewCommentEditorFileListener` クラスを作成し、`plugin.xml` に登録する。

```kotlin
internal class ReviewCommentEditorFileListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) { ... }
    override fun selectionChanged(event: FileEditorManagerSelectionEvent) { ... }
}
```

**根拠**:
- `plugin.xml` の `editorFileEditorManagerListener` 拡張点で登録可能
- プロジェクトライフサイクルに自動でバインドされる

### 推奨案

両方の設計を採用。相対パス変換は `CommentInlayManager` 内の private 関数とし、リスナーは新規クラスとして外部登録する。

## Key Files To Read (file:line)

全主要ファイルは既読済み。以下に設計判断の根拠として特筆すべき箇所を列挙する。

- `CommentInlayManager.kt:81-113` — `restoreComments` の実装。冪等性ガード (`commentInlays.isNotEmpty()`) あり
- `CommentInlayManager.kt:92` — `filePath == filePath` の評価。ここを相対パス比較に変更
- `CommentInlayManager.kt:142` — `saveComment` での `filePath` 保存。ここで相対化
- `ReviewCommentEditorTracker.kt:55` — `restoreComments` の呼び出し。引数は `file.path`（絶対パス）
- `plugin.xml:15` — `EditorFactoryListener` の登録箇所。同様の方式で新リスナーを追加
- `CommentInlayManagerStorageTest.kt:41` — `assertEquals(file.path, ...)` 絶対パス検証。相対化後に修正必要

## Evidence

- **調査日**: 2026-07-06
- **調査対象**: 全ソースファイル（16ファイル）、全テストファイル（11ファイル）
- **確認方法**: JetBrains MCP によるファイル直接読み込み
