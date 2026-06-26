# Plan: GutterIconによるコメント追加トリガーとActionの実装

## Phase 0: 調査と詳細設計

- [ ] Task: Issue #4、既存コード、Conductor 文書を再確認し、実装対象を確定する。
    - [ ] `src/main/resources/META-INF/plugin.xml` の現状を確認する。
    - [ ] `src/main/kotlin/.../model` と `storage` の責務境界を確認する。
    - [ ] 既存テストの命名規則と JUnit 4 の書き方を確認する。
- [ ] Task: IntelliJ Platform SDK の editor / action / gutter 関連 API を調査し、`evidence_report.md` に記録する。
    - [ ] `AnAction` と `plugin.xml` action 登録方針を記録する。
    - [ ] `CommonDataKeys.EDITOR` によるエディタ文脈取得方針を記録する。
    - [ ] `RangeHighlighter` / `GutterIconRenderer` / editor event listener の採用可否を IDE 型情報で確認する。
- [ ] Task: 調査結果に基づき、後続タスクの対象ファイル、責務、検証方法を `plan.md` に必要なら追記する。
- [ ] Task: `./gradlew test` を実行し、Phase 0 時点の既存テスト状態を確認する。
- [ ] Task: Conductor - User Manual Verification 'Phase 0' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 0' の成果をコミット

## Phase 1: 行範囲算出とAction入力モデルの実装

- [ ] Task: 行範囲算出ロジックの unit test を作成し、Red を確認する。
    - [ ] 選択なしの場合、カーソル現在行が `lineStart == lineEnd` になることをテストする。
    - [ ] 単一行選択の場合、対象行が 1 始まりで返ることをテストする。
    - [ ] 複数行選択の場合、開始行と終了行が 1 始まりで返ることをテストする。
- [ ] Task: UI API から分離した行範囲モデルと算出ロジックを実装し、Green を確認する。
    - [ ] 新規ファイル候補: `src/main/kotlin/.../editor/ReviewCommentLineRange.kt`
    - [ ] 新規ファイル候補: `src/main/kotlin/.../editor/ReviewCommentLineRangeResolver.kt`
- [ ] Task: `AddReviewCommentAction` の unit test またはテスト可能な表示データ生成ロジックを作成し、Red を確認する。
    - [ ] 対象ファイルパスと行範囲をダミーダイアログ表示用メッセージに変換できることをテストする。
- [ ] Task: `AddReviewCommentAction` を実装する。
    - [ ] 新規ファイル候補: `src/main/kotlin/.../action/AddReviewCommentAction.kt`
    - [ ] `CommonDataKeys.EDITOR` と対象ファイルを取得する。
    - [ ] ダミーダイアログで対象ファイルと行範囲を表示する。
    - [ ] ユーザー向け文言が必要な場合は `MyBundle.properties` を追加または更新する。
- [ ] Task: `plugin.xml` に `AddReviewCommentAction` を登録する。
    - [ ] 必要最小限の `<actions>` 設定を追加する。
    - [ ] エディタコンテキスト上で Action が利用できる登録先を確認する。
- [ ] Task: `./gradlew test` を実行し、Phase 1 のテスト成功を確認する。
- [ ] Task: `detekt` が利用可能なら `./gradlew detekt` を実行し、未定義なら未実施理由を記録する。
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 1' の成果をコミット

## Phase 2: ガターアイコン表示とエディタイベント追従の実装

- [ ] Task: ガター表示状態管理の unit test を作成し、Red を確認する。
    - [ ] 現在行変更時に古い表示対象が置き換わることをテストする。
    - [ ] 複数行選択時に開始行をガター表示対象にすることをテストする。
- [ ] Task: ガター表示を管理するコンポーネントを実装する。
    - [ ] 新規ファイル候補: `src/main/kotlin/.../editor/ReviewCommentGutterIconRenderer.kt`
    - [ ] 新規ファイル候補: `src/main/kotlin/.../editor/ReviewCommentGutterController.kt`
    - [ ] `RangeHighlighter` と `GutterIconRenderer` を使って「＋」アイコンを表示する。
    - [ ] アイコンクリック時に `AddReviewCommentAction` 相当の処理を呼び出す。
    - [ ] 表示更新時に古い highlighter を破棄する。
- [ ] Task: エディタイベント購読を実装する。
    - [ ] 新規ファイル候補: `src/main/kotlin/.../editor/ReviewCommentEditorListener.kt`
    - [ ] `SelectionListener` または `CaretListener` により選択範囲・カーソル移動へ追従する。
    - [ ] エディタ生成・破棄に合わせて listener と highlighter を解放する。
    - [ ] 必要に応じて Project Service または ProjectActivity を追加し、`plugin.xml` を更新する。
- [ ] Task: `./gradlew test` を実行し、Phase 2 のテスト成功を確認する。
- [ ] Task: `detekt` が利用可能なら `./gradlew detekt` を実行し、未定義なら未実施理由を記録する。
- [ ] Task: `./gradlew build` を実行し、IntelliJ Platform plugin としてのビルド成功を確認する。
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 2' の成果をコミット
