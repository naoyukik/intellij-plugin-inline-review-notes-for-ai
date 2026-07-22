# Track 038-file-auto-reload Implementation Plan

## Phase 0 - Research and Detailed Design
- [ ] Task: 現在の `CommentInlayManager` の `restoreComments` フローを分析
- [ ] Task: IntelliJ Platform の `BulkFileListener` API を調査
- [ ] Task: ブランチ切替検知メカニズムを調査
- [ ] Task: `evidence_report.md` を作成
- [ ] Task: Conductor - ユーザー手動検証 'Phase 0'

## Phase 1 - Implementation
- [ ] Task: `ReviewCommentFileWatcher` クラスを作成
    - [ ] `BulkFileListener` を使用して `.inline-review-notes/` の変更を監視
    - [ ] 現在のブランチ名に対応するファイルのみ監視
    - [ ] ファイル変更・削除イベントを処理
- [ ] Task: `CommentInlayManager` にリロードメソッドを追加
    - [ ] `reloadComments()` メソッドを追加
    - [ ] 全 Inlay をクリアして再読み込み
- [ ] Task: ブランチ切替検知を実装
    - [ ] `VirtualFileListener` または Git 状態変更を監視
    - [ ] ブランチ切替時にコメントを再読み込み
- [ ] Task: テストを作成
    - [ ] `ReviewCommentFileWatcher` の単体テスト
    - [ ] ブランチ切替検知のテスト
- [ ] Task: Conductor - ユーザー手動検証 'Phase 1'

## Phase 2 - Verification
- [ ] Task: 結合テストを実行
- [ ] Task: パフォーマンステストを実行
- [ ] Task: ドキュメントを更新
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2'
