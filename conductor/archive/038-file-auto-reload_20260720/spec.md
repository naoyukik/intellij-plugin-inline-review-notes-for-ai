# Track 038-file-auto-reload Specification

## Overview
`.inline-review-notes/` 配下のファイルが外部から変更・削除された際に、エディタ上のコメントを自動的に再読み込みする機能を実装する。

## Functional Requirements

### FR1: ファイル変更監視
- `.inline-review-notes/{branch}.json` ファイルの変更・削除イベントを監視する
- 監視対象は現在のブランチ名に一致するファイルのみ
- IntelliJ Platform の `BulkFileListener` を使用して VFS 変更イベントを取得する

### FR2: 即時コメント再読み込み
- ファイル変更検知時に、全コメントを再読み込みする
- 再読み込みは `CommentInlayManager.restoreComments()` を再呼び出しすることで実現する
- 削除イベント検知時は、表示中のコメントをすべてクリアする

### FR3: ブランチ切替対応
- Git ブランチ切替時に、新しいブランチに対応するコメントを自動的に読み込む
- ブランチ切替は `VirtualFileListener` または Git 状態の変更検知で実現する

## Non-Functional Requirements

### NFR1: パフォーマンス
- ファイル変更検知から UI 更新までの遅延は 500ms 以内
- 頻繁なファイル変更に対するデバウンス処理を実装する

### NFR2: 既存機能への影響
- 既存のコメント作成・編集・削除機能に影響を与えない
- エディタのパフォーマンスに悪影響を与えない

## Acceptance Criteria
1. `.inline-review-notes/{branch}.json` を外部エディタで編集した場合、エディタ上のコメントが自動更新される
2. ファイルを削除した場合、表示中のコメントがすべてクリアされる
3. ブランチを切替えた場合、新しいブランチのコメントが自動的に表示される
4. 既存のコメント作成・編集・削除機能が正常に動作する

## Out of Scope
- プロジェクト全体のファイル変更監視
- リアルタイム同期（ファイルシステムのポーリング）
- ネットワーク経由のファイル共有対応
