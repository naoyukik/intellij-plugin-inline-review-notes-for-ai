# 実装計画 (plan.md)

## 概要
本計画は、GitHub Issue 24 に基づき、ファイルオープン時およびエディタのアクティブ化時にインラインレビューコメントを自動復元する機能の実装手順を定義します。

---

## フェーズ 0: 設計と調査 [checkpoint: 1756e5d]
- [x] Task: 既存実装の調査と詳細設計
    - [x] `ReviewCommentEditorTracker.kt` と `CommentInlayManager.kt` におけるエディタイベント検知およびインレイ描画処理の既存ロジックを詳細に調査する。
    - [x] `FileEditorManagerListener` をどのコンポーネント（プロジェクトサービス等）で購読・管理すべきか設計する。→ 新規 `ReviewCommentEditorFileListener` クラス + `plugin.xml` 登録
    - [x] 絶対パスからプロジェクト相対パスへの変換方法（例: `project.basePath` からの相対化）の共通実装場所を特定・設計する。→ `CommentInlayManager` 内に private 関数として実装
- [x] Task: Conductor - ユーザー手動検証 'Phase 0' (Protocol in workflow.ja.md)
- [x] Task: Phase 0 コミットし、本フェーズを完了とする

## フェーズ 1: コメントパスの相対化 (TDD)

### 設計方針
- `CommentInlayManager` 内に `Project.toRelativePath(absolutePath: String): String?` を private 関数として追加
- `saveComment` 内で `ReviewComment` 作成時に絶対パス→相対パス変換
- `restoreComments` 内でフィルタリング時に引数の絶対パスを相対パスに変換し、保存済みの相対パスと比較
- 呼び出し元（`AddReviewCommentAction.kt`, `ReviewCommentEditorTracker.kt`, `ReviewCommentGutterIconRenderer.kt`）は変更不要（内部で自動変換）

- [ ] Task: コメントパス相対化のテスト追加 (Red)
    - [ ] `CommentInlayManagerStorageTest.kt` の `test_save_persists_comment_to_storage` で `assertEquals(file.path, savedComment.filePath)` の期待値を相対パス（例: `"Foo.kt"`）に変更し、Red を確認する。
    - [ ] `CommentInlayManagerStorageTest.kt` の `test_save_with_existing_comment_updates_storage` でも保存後のコメント取得時に相対パス検証に変更する。
- [ ] Task: コメントパス相対化の実装 (Green)
    - [ ] `CommentInlayManager` に `Project.toRelativePath(absolutePath: String): String?` を private 拡張関数として追加する。
    - [ ] `saveComment` 内で `ReviewComment` 作成時の `filePath` に相対パスを使用する。
    - [ ] `restoreComments` 内のフィルタ条件 `it.filePath == filePath` を相対パス比較に変更する。
    - [ ] `CommentInlayManagerStorageTest.kt` のアサーション（3箇所）を相対パスに修正する。
    - [ ] テストを実行し、すべてのテストがパスすることを確認する。
    - [ ] `./gradlew detekt` を実行し、コードスタイルが維持されていることを確認する。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 1' (Protocol in workflow.ja.md)
- [ ] Task: Phase 1 コミットし、本フェーズを完了とする

## フェーズ 2: 自動復元機能とイベントリスナーの実装 (TDD)

### 設計方針
- 新規クラス `ReviewCommentEditorFileListener` を作成し `FileEditorManagerListener` を実装
- `plugin.xml` に `editorFileEditorManagerListener` 拡張として登録
- `restoreComments` 内のガード条件を `state.filePath == filePath && state.commentInlays.isNotEmpty()` に変更 — 同一 Editor インスタンスでタブ切替（別ファイル）の場合にも復元を可能にする
- `resolvedAt == null` フィルタも同時に追加

- [ ] Task: 自動復元トリガーのテスト追加 (Red)
    - [ ] ファイルが新規に開かれた時、対応する未解決コメントが復元されることを検証する統合テストを作成し、Red を確認する。
    - [ ] エディタタブが切り替わった時、新しいファイルに対応する未解決コメントが復元されることを検証するテストを作成し、Red を確認する。
    - [ ] 解決済み（`resolvedAt` 設定済み）コメントは復元されないことを検証するテストを作成し、Red を確認する。
- [ ] Task: 自動復元トリガーの実装 (Green)
    - [ ] `ReviewCommentEditorFileListener` クラスを作成し `FileEditorManagerListener` を実装する。
        - `fileOpened`: 開かれたファイルに対応するエディタを取得し `CommentInlayManager.restoreComments` を呼ぶ
        - `selectionChanged`: 新しく選択されたファイルに対応するエディタを取得し `CommentInlayManager.restoreComments` を呼ぶ
    - [ ] `plugin.xml` に `editorFileEditorManagerListener` 拡張を追加する。
    - [ ] `restoreComments` 内のガード条件を `state.filePath == filePath && state.commentInlays.isNotEmpty()` に変更する。
    - [ ] `restoreComments` 内のフィルタに `&& it.resolvedAt == null` を追加する。
    - [ ] テストを実行し、すべてのテストがパスすることを確認する。
    - [ ] `./gradlew detekt` および `./gradlew build` を実行し、ビルドと静的解析が正常に通ることを確認する。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2' (Protocol in workflow.ja.md)
- [ ] Task: Phase 2 コミットし、本フェーズを完了とする
