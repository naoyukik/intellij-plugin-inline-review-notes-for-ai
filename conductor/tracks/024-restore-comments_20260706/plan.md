# 実装計画 (plan.md)

## 概要
本計画は、GitHub Issue 24 に基づき、ファイルオープン時およびエディタのアクティブ化時にインラインレビューコメントを自動復元する機能の実装手順を定義します。

---

## フェーズ 0: 設計と調査
- [ ] Task: 既存実装の調査と詳細設計
    - [ ] `ReviewCommentEditorTracker.kt` と `CommentInlayManager.kt` におけるエディタイベント検知およびインレイ描画処理の既存ロジックを詳細に調査する。
    - [ ] `FileEditorManagerListener` をどのコンポーネント（プロジェクトサービス等）で購読・管理すべきか設計する。
    - [ ] 絶対パスからプロジェクト相対パスへの変換方法（例: `project.basePath` からの相対化）の共通実装場所を特定・設計する。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 0' (Protocol in workflow.ja.md)
- [ ] Task: Phase 0 コミットし、本フェーズを完了とする

## フェーズ 1: コメントパスの相対化 (TDD)
- [ ] Task: コメントパス相対化のテスト追加 (Red)
    - [ ] コメント保存・復元処理において `filePath` がプロジェクト相対パスで処理されることを検証するテストを `CommentInlayManagerStorageTest.kt` または新規テストクラスに作成し、失敗することを確認する。
- [ ] Task: コメントパス相対化の実装 (Green)
    - [ ] `CommentInlayManager` でコメント保存時および復元（フィルタリング）時に、絶対パスをプロジェクトルートからの相対パスに変換する処理を実装する。
    - [ ] `CommentInlayManagerStorageTest.kt` などの既存テストで、絶対パスの保存を期待している箇所を相対パスの検証に修正する。
    - [ ] テストを実行し、すべてのテストがパスすることを確認する。
    - [ ] `./gradlew detekt` を実行し、コードスタイルが維持されていることを確認する。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 1' (Protocol in workflow.ja.md)
- [ ] Task: Phase 1 コミットし、本フェーズを完了とする

## フェーズ 2: 自動復元機能とイベントリスナーの実装 (TDD)
- [ ] Task: 自動復元トリガーのテスト追加 (Red)
    - [ ] ファイルが新規に開かれた時、およびタブが切り替わってエディタがアクティブになった時に、対応する未解決コメントが復元表示されることを検証する統合テストを作成し、失敗することを確認する。
- [ ] Task: 自動復元トリガーの実装 (Green)
    - [ ] `FileEditorManagerListener` を実装し、`plugin.xml` に登録するか、または適切なライフサイクルで購読を開始する。
    - [ ] `fileOpened` および `selectionChanged` のイベント発生時に、対象エディタに対して `CommentInlayManager.restoreComments` を呼び出す。
    - [ ] 未解決（`resolvedAt` が未設定）のコメントのみが復元対象となるように `restoreComments` 内のフィルタ処理を更新する。
    - [ ] すでにコメントが表示されているエディタに対して、重複してインレイが追加されないように保護処理を追加する。
    - [ ] テストを実行し、すべてのテストがパスすることを確認する。
    - [ ] `./gradlew detekt` および `./gradlew build` を実行し、ビルドと静的解析が正常に通ることを確認する。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2' (Protocol in workflow.ja.md)
- [ ] Task: Phase 2 コミットし、本フェーズを完了とする
