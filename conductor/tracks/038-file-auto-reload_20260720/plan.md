# Track 038-file-auto-reload Implementation Plan

## Phase 0 - Research and Detailed Design
- [x] Task: 現在の `CommentInlayManager` の `restoreComments` フローを分析
- [x] Task: IntelliJ Platform の `BulkFileListener` API を調査
- [x] Task: ブランチ切替検知メカニズムを調査
- [x] Task: `evidence_report.md` を作成
- [x] Task: Conductor - ユーザー手動検証 'Phase 0'

## Phase 1 - Implementation
- [x] Task: `ReviewCommentFileWatcher` クラスを作成 (`src/main/kotlin/.../editor/ReviewCommentFileWatcher.kt`)
    - [x] `BulkFileListener` を実装し `VirtualFileManager.VFS_CHANGES` にサブスクライブ
    - [x] `after()` メソッドで `VFileEvent` をフィルタリング
    - [x] `.inline-review-notes/` 配下の変更のみ処理（プロジェクトフィルタリング）
    - [x] 現在のブランチ名に対応するファイルのみ監視 (`ReviewCommentStorage.currentBranchName()`)
    - [x] ファイル変更イベント: `CommentInlayManager.reloadComments()` を呼び出し
    - [x] ファイル削除イベント: `CommentInlayManager.clearAllComments()` を呼び出し
    - [x] デバウンス処理: 300ms (`javax.swing.Timer` を使用)
- [x] Task: `CommentInlayManager` にリロードメソッドを追加
    - [x] `reloadComments(editor: Editor, project: Project, filePath: String)` メソッドを追加
    - [x] 全 Inlay をクリアして `restoreComments()` を再呼び出し
    - [x] `clearAllComments(editor: Editor)` メソッドを追加
    - [x] 全 Inlay をクリアするのみ（再読み込みなし）
- [x] Task: ブランチ切替検知を実装 (`ReviewCommentFileWatcher` 内)
    - [x] `.git/HEAD` ファイルの変更を `BulkFileListener` で監視
    - [x] ブランチ名が変更された場合のみ `reloadComments()` を呼び出し
    - [x] 全エディタに対してコメントを再読み込み
- [x] Task: `plugin.xml` にBulkFileListenerを登録
    - [x] `ReviewCommentEditorProjectActivity` で `VirtualFileManager.VFS_CHANGES` にサブスクライブ
- [x] Task: テストを作成
    - [x] `ReviewCommentFileWatcherTest`: ファイル削除イベントのテスト
    - [x] `ReviewCommentFileWatcherTest`: リロードテスト
    - [x] `ReviewCommentFileWatcherTest`: クリアテスト
    - [x] `CommentInlayManagerTest`: `reloadComments()` メソッドのテスト
    - [x] `CommentInlayManagerTest`: `clearAllComments()` メソッドのテスト
- [~] Task: Conductor - ユーザー手動検証 'Phase 1'

## Phase 2 - Verification
- [x] Task: `./gradlew test` で全テストを実行
- [x] Task: `./gradlew detekt` で静的解析を実行
- [~] Task: 手動検証: 外部エディタで `.inline-review-notes/{branch}.json` を編集し、コメントが自動更新されることを確認
- [~] Task: 手動検証: ファイルを削除し、コメントがクリアされることを確認
- [~] Task: 手動検証: ブランチを切り替え、新しいブランチのコメントが表示されることを確認
- [~] Task: 手動検証: 既存のコメント作成・編集・削除機能が正常に動作することを確認
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2'
