# Track 038-file-auto-reload Implementation Plan

## Phase 0 - Research and Detailed Design
- [x] Task: 現在の `CommentInlayManager` の `restoreComments` フローを分析
- [x] Task: IntelliJ Platform の `BulkFileListener` API を調査
- [x] Task: ブランチ切替検知メカニズムを調査
- [x] Task: `evidence_report.md` を作成
- [x] Task: Conductor - ユーザー手動検証 'Phase 0'

## Phase 1 - Implementation
- [ ] Task: `ReviewCommentFileWatcher` クラスを作成 (`src/main/kotlin/.../editor/ReviewCommentFileWatcher.kt`)
    - [ ] `BulkFileListener` を実装し `VirtualFileManager.VFS_CHANGES` にサブスクライブ
    - [ ] `after()` メソッドで `VFileEvent` をフィルタリング
    - [ ] `.inline-review-notes/` 配下の変更のみ処理（プロジェクトフィルタリング）
    - [ ] 現在のブランチ名に対応するファイルのみ監視 (`ReviewCommentStorage.currentBranchName()`)
    - [ ] ファイル変更イベント: `CommentInlayManager.reloadComments()` を呼び出し
    - [ ] ファイル削除イベント: `CommentInlayManager.clearAllComments()` を呼び出し
    - [ ] デバウンス処理: 300ms (`kotlinx.coroutines` の `delay` を使用)
- [ ] Task: `CommentInlayManager` にリロードメソッドを追加
    - [ ] `reloadComments(editor: Editor, project: Project, filePath: String)` メソッドを追加
    - [ ] 全 Inlay をクリアして `restoreComments()` を再呼び出し
    - [ ] `clearAllComments(editor: Editor)` メソッドを追加
    - [ ] 全 Inlay をクリアするのみ（再読み込みなし）
- [ ] Task: ブランチ切替検知を実装 (`ReviewCommentFileWatcher` 内)
    - [ ] `.git/HEAD` ファイルの変更を `BulkFileListener` で監視
    - [ ] ブランチ名が変更された場合のみ `reloadComments()` を呼び出し
    - [ ] 全エディタに対してコメントを再読み込み
- [ ] Task: `plugin.xml` にBulkFileListenerを登録
    - [ ] `<applicationListeners>` に `ReviewCommentFileWatcher` を追加
    - [ ] `topic="com.intellij.openapi.vfs.newvfs.BulkFileListener"` を指定
- [ ] Task: テストを作成
    - [ ] `ReviewCommentFileWatcherTest`: ファイル変更イベントのテスト
    - [ ] `ReviewCommentFileWatcherTest`: ファイル削除イベントのテスト
    - [ ] `ReviewCommentFileWatcherTest`: ブランチ切替検知のテスト
    - [ ] `CommentInlayManagerTest`: `reloadComments()` メソッドのテスト
    - [ ] `CommentInlayManagerTest`: `clearAllComments()` メソッドのテスト
- [ ] Task: Conductor - ユーザー手動検証 'Phase 1'

## Phase 2 - Verification
- [ ] Task: `./gradlew test` で全テストを実行
- [ ] Task: `./gradlew detekt` で静的解析を実行
- [ ] Task: 手動検証: 外部エディタで `.inline-review-notes/{branch}.json` を編集し、コメントが自動更新されることを確認
- [ ] Task: 手動検証: ファイルを削除し、コメントがクリアされることを確認
- [ ] Task: 手動検証: ブランチを切り替え、新しいブランチのコメントが表示されることを確認
- [ ] Task: 手動検証: 既存のコメント作成・編集・削除機能が正常に動作することを確認
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2'
