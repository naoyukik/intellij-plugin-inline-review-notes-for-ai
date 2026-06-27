# Plan: GutterIconによるコメント追加トリガーとActionの実装

## Phase 0: 調査と詳細設計

- [x] Task: Issue #4、既存コード、Conductor 文書を再確認し、実装対象を確定する。
  - [x] `src/main/resources/META-INF/plugin.xml:1-27` の現状を確認する。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewComment.kt:1-14`、`ReviewCommentDocument.kt:1-13`、`storage/ReviewCommentStorage.kt:1-162` の責務境界を確認する。
  - [x] `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewCommentSerializationTest.kt:1-34`、`src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/storage/ReviewCommentStorageTest.kt:1-140` の命名規則と JUnit 4 の書き方を確認する。
- [x] Task: IntelliJ Platform SDK の editor / action / gutter 関連 API を調査し、`evidence_report.md` に記録する。
  - [x] `AnAction` と `plugin.xml` action 登録方針を記録する。
  - [x] `CommonDataKeys.EDITOR` によるエディタ文脈取得方針を記録する。
  - [x] `RangeHighlighter` / `GutterIconRenderer` / editor event listener の採用可否を IDE 型情報で確認する。
- [x] Task: 調査結果に基づき、後続タスクの対象ファイル、責務、検証方法を `plan.md` に具体化する。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt:1-41` を `CommonDataKeys.EDITOR` ベースのダミーダイアログ起点として扱う。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRange.kt:1-12` と `ReviewCommentLineRangeResolver.kt:1-10` を UI 非依存の行範囲算出に使う。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorLineRangeResolver.kt:1-18` を editor 由来の selection / caret 変換に使う。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt:1-42` をクリック処理と tooltip 表示に使う。
  - [x] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListener.kt:1-92` を editor lifecycle と highlighter 破棄に使う。
  - [x] `src/main/resources/META-INF/plugin.xml:1-27` に resource bundle / action / listener を登録する。
  - [x] `src/main/resources/messages/MyBundle.properties:1-5` にユーザー向け文言を置く。
  - [x] `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRangeResolverTest.kt:1-31` と `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentPresentationTest.kt:1-19` で純粋ロジックを検証する。
- [x] Task: `./gradlew test` を実行し、Phase 0 時点の既存テスト状態を確認する。
- [x] Task: Conductor - User Manual Verification 'Phase 0' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 0' の成果をコミット

## Phase 1: 行範囲算出とAction入力モデルの実装

- [ ] Task: 行範囲算出ロジックの unit test を作成し、Red を確認する。
  - [ ] `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRangeResolverTest.kt:1-31` で、選択なし / 単一行 / 複数行のケースを固定する。
- [ ] Task: UI API から分離した行範囲モデルと算出ロジックを実装し、Green を確認する。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRange.kt:1-12` と `ReviewCommentLineRangeResolver.kt:1-10` を追加し、1 始まりの範囲型にする。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorLineRangeResolver.kt:1-18` で editor 由来の selection / caret を 1 始まりの範囲へ変換する。
- [ ] Task: `AddReviewCommentAction` の unit test またはテスト可能な表示データ生成ロジックを作成し、Red を確認する。
  - [ ] `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentPresentationTest.kt:1-19` で、ファイルパスと行範囲をダイアログ表示用文字列へ変換できることを検証する。
- [ ] Task: `AddReviewCommentAction` を実装する。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt:1-41` を追加し、`CommonDataKeys.EDITOR` と対象ファイルを取得してダミーダイアログを表示する。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentPresentation.kt` で、表示メッセージの組み立てを UI から分離する。
  - [ ] `src/main/resources/messages/MyBundle.properties:1-5` と `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/MyBundle.kt:1-12` を使って文言を外出しする。
- [ ] Task: `plugin.xml` に `AddReviewCommentAction` を登録する。
  - [ ] `src/main/resources/META-INF/plugin.xml:1-27` に最小限の `<actions>` を追加し、`EditorPopupMenu` から呼べるようにする。
- [ ] Task: `./gradlew test` を実行し、Phase 1 のテスト成功を確認する。
- [ ] Task: `detekt` が利用可能なら `./gradlew detekt` を実行し、未定義なら未実施理由を記録する。
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 1' の成果をコミット

## Phase 2: ガターアイコン表示とエディタイベント追従の実装

- [ ] Task: ガター表示状態管理の unit test を作成し、Red を確認する。
  - [ ] `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListenerTest.kt` を追加し、caret / selection 更新で古い表示が置き換わるケースを固定する。
- [ ] Task: ガター表示を管理するコンポーネントを実装する。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt:1-42` で「＋」アイコンの click / tooltip を担う。
  - [ ] `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListener.kt:1-92` で highlighter の更新と破棄を担う。
  - [ ] 追加の controller は原則として作らず、listener と renderer の最小構成で状態管理する。
- [ ] Task: エディタイベント購読を実装する。
  - [ ] `SelectionListener` と `CaretListener` により選択範囲・カーソル移動へ追従する。
  - [ ] エディタ生成・破棄に合わせて listener と highlighter を解放する。
  - [ ] 必要に応じて `src/main/resources/META-INF/plugin.xml:14-25` を更新する。
- [ ] Task: `./gradlew test` を実行し、Phase 2 のテスト成功を確認する。
- [ ] Task: `detekt` が利用可能なら `./gradlew detekt` を実行し、未定義なら未実施理由を記録する。
- [ ] Task: `./gradlew build` を実行し、IntelliJ Platform plugin としてのビルド成功を確認する。
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)
- [ ] Task: Conductor - 'Phase 2' の成果をコミット

