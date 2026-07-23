# Evidence Report: Track 038-file-auto-reload

## 1. Discovery Summary (Phase 1)

- **Problem Statement**: `.inline-review-notes/` 配下のファイルが外部から変更・削除された際に、エディタ上のコメントを自動的に再読み込みする必要がある。現在はエディタを再起動しないと変更が反映されない。
- **Scope**: `.inline-review-notes/{branch}.json` の変更・削除イベント監視、現在のブランチ名に一致するファイルのみ監視、ファイル変更検知時のコメント再読み込み、ブランチ切替時の自動読み込み
- **Non-Goals**: プロジェクト全体のファイル変更監視、リアルタイム同期（ポーリング）、ネットワーク経由のファイル共有対応
- **Constraints**: IntelliJ Platform の `BulkFileListener` を使用、既存機能への影響不可、UI更新遅延500ms以内
- **Success Criteria**: 外部エディタでファイル編集時にコメント自動更新、ファイル削除時にコメントクリア、ブランチ切替時に新しいコメント自動表示

## 2. Codebase Findings (Phase 2)

- **Similar Implementations**:
  - `CommentInlayManager.kt:82-125`: `restoreComments()` メソッドがコメント再読み込みロジックを実装
  - `ReviewCommentEditorFileListener.kt:13-36`: ファイル切替時に `restoreComments()` を呼び出し
  - `ReviewCommentEditorTracker.kt:50-56`: エディタ作成時に `restoreComments()` を呼び出し
- **Architecture and Dependency Notes**:
  - BulkFileListenerはapplication-levelで、全プロジェクトのイベントを受け取る
  - プロジェクトフィルタリングが必要
  - VirtualFileManager.VFS_CHANGES topicにサブスクライブ
- **Reusable Components**:
  - `CommentInlayManager.restoreComments()`: 再読み込みロジックの再利用可能
  - `ReviewCommentStorage.currentBranchName()`: ブランチ名取得の再利用可能
  - `ReviewCommentStorage.resolveStorageFilePath()`: ストレージファイルパス解決の再利用可能
- **Estimated Impact Area**:
  - 新規: `ReviewCommentFileWatcher.kt`
  - 変更: `CommentInlayManager.kt` (リロードメソッド追加)
  - 変更: `plugin.xml` (BulkFileListener追加)

## 3. Clarifying Questions (Phase 3)

- **Open Questions**:
  1. デバウンス処理の待ち時間はどれくらいが適切か
  2. ブランチ切替検知はどのような方法で実現するか
  3. ファイル削除時にユーザーに通知するか
- **User Answers / Delegations**:
  1. デバウンス時間: 300ms
  2. ブランチ切替検知: `.git/HEAD` 監視
  3. ファイル削除通知: なし
- **Unresolved Items**: なし

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- `.inline-review-notes/{branch}.json` ファイルが外部から変更された場合、エディタ上のコメントが300ms以内に自動更新される
- ファイルが削除された場合、表示中のコメントがすべてクリアされる（ユーザー通知なし）
- Gitブランチを切替えた場合、新しいブランチのコメントが自動的に表示される
- 複数エディタが同一ファイルを開いている場合、すべてのエディタでコメントが同期更新される

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes
- **Change Targets**: `CommentInlayManager.kt` にリロードメソッド追加、`plugin.xml` にBulkFileListener追加
- **Pros**: 変更範囲が最小限、既存コードの再利用
- **Cons**: ファイル監視ロジックとコメント管理が密結合
- **Validation Plan**: 既存テスト + 新規単体テスト

### Option B: Clean Architecture
- **Change Targets**: 新規 `ReviewCommentFileWatcher` クラス作成、`CommentInlayManager` にリロードメソッド追加
- **Pros**: 責務分離が明確、テスト容易性が高い
- **Cons**: 新規クラス追加のオーバーヘッド
- **Validation Plan**: 単体テスト + 結合テスト

### Option C: Pragmatic Balance (推奨)
- **Change Targets**: `ReviewCommentFileWatcher` クラス作成（簡易版）、`CommentInlayManager` にリロードメソッド追加
- **Pros**: 責務分離とシンプルさのバランス
- **Cons**: Option Bより分離性が若干低い
- **Validation Plan**: 単体テスト

- **Recommended Option**: Option C (Pragmatic Balance)
- **Reason**: 責務分離を保ちつつ、既存パターンに沿ったシンプルな実装にするため

## 6. 推奨される実装方針 (Implementation Strategy)

- **Architecture Alignment**: `editor` パッケージに `ReviewCommentFileWatcher` を配置、既存のリスナーパターンに準拠
- **Logic Changes**:
  1. `ReviewCommentFileWatcher` クラス作成（BulkFileListener実装）
  2. `.inline-review-notes/` 配下のファイル変更を監視
  3. 現在のブランチ名に一致するファイルのみ処理
  4. デバウンス処理（300ms）でパフォーマンスを確保
  5. `.git/HEAD` ファイル変更を監視してブランチ切替を検知
  6. `CommentInlayManager` に `reloadComments()` メソッドを追加
- **Validation Plan**: 
  - 既存テストの実行（`./gradlew test`）
  - 新規単体テストの作成
  - 手動検証: 外部エディタでファイル編集時の動作確認

## 7. Evidence and Alignment

- **Source URLs**:
  - https://plugins.jetbrains.com/docs/intellij/virtual-file-system.html
  - https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html
- **Research Date**: 2026-07-23
- **Key Findings**:
  - BulkFileListenerはVirtualFileManager.VFS_CHANGES topicにサブスクライブ
  - application-levelリスナーのためプロジェクトフィルタリングが必要
  - `.git/HEAD` ファイル変更監視でブランチ切替検知が可能
- **Local Constraint Alignment**:
  - `conductor/code_styleguides/general.md` との整合: シンプルさと一貫性を重視
- **Potential Regressions**:
  - 既存のCommentInlayManager.restoreComments()メソッドの動作変更なし
- **Residual Risks / Unknowns**:
  - BulkFileListenerのパフォーマンス影響（デバウンスで対応）
  - Gitプラグイン非インストール環境でのブランチ切替検知（.git/HEAD監視で対応済み）
