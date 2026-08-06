# Issue 56 空ドキュメントのクラッシュ修正 実装計画

- Track ID: `56-empty-document-crash-fix_20260804`
- Type: Bug
- Source of Truth: `spec.md`、`conductor/workflow.md`
- 対象ブランチ: Track ID を含むブランチを使用する

## Phase 0: Discovery & Detailed Design

- [x] Task: Issue 56 の現状と実行環境を確認する
  - [x] `ReviewCommentEditorTracker.kt` の `refresh`、`CommentInlayManager.kt` の入力位置・インレイ位置計算、`ReviewCommentEditorLineRangeResolver.kt`、関連テストを読み、現在の処理経路を確定する
  - [x] 空の `Document`、JSON 不在、非空ファイルのそれぞれについて再現条件と期待結果を整理する
  - [x] `.\gradlew.bat tasks --all` の結果から `test`、`detekt`、`build` の利用可否を確認する

- [x] Task: IntelliJ Platform の空ドキュメント位置 API を調査する
  - [x] `lineCount == 0` のときに `getLineStartOffset`、`addLineHighlighter`、`addBlockElement`、ポップアップ位置計算へ渡せる安全なインデックス・オフセットを確認する
  - [x] 論理 `ReviewCommentLineRange(1, 1)` を維持しながら、空範囲 `0..-1` を生成しない正規化方法を比較する
  - [x] ガター、コンテキストメニュー、コメント保存・復元の全導線で同じ扱いにできるか検証する

- [x] Task: Phase 0 の `evidence_report.md` を作成し、計画を具体化する
  - [x] 調査結果、再現ログ、対象ファイルと予定変更箇所、テスト方針を `conductor/tracks/56-empty-document-crash-fix_20260804/evidence_report.md` に記録する
  - [x] 証拠に基づき Phase 1 の対象行、テストケース、実装方法を `plan.md` に追記・修正する
  - [x] 証拠レポートと更新済み計画についてユーザーの承認を得る

- [x] Task: Phase 0 Verification & Checkpoint (Refer to `workflow.md`)
  - [x] ベースラインとして `.\gradlew.bat test` を実行し、既存テストが成功することを確認する
  - [x] `.\gradlew.bat detekt` を実行し、静的解析結果を確認する
  - [x] Task: Conductor - User Manual Verification 'Phase 0 Discovery & Detailed Design' (Protocol in `workflow.md`)
  - [x] ユーザーに証拠レポートと Phase 1 計画が期待どおりか確認し、明示的な承認を得る
  - [x] Task: Conductor - 'Phase 0 Discovery & Detailed Design' の成果をコミットする
  - [x] 変更ファイルを確認し、`conductor(checkpoint): Checkpoint end of Phase 0` 相当のコミット（`c2fe0db`）を作成する
  - [x] コミット SHA `c2fe0db` を `plan.md` に記録し、計画更新を別コミットとして記録する

## Phase 1: Empty Document Safety Fix

- [x] Task: TDD で Issue 56 の回帰テストを追加する
  - [x] `ReviewCommentEditorFileListenerTest.kt` に空文字・JSON 不在で `ReviewCommentEditorTracker.track` を実行し、ガター highlighter が準備されることを確認する再現テストを追加する（Red を確認済み）
  - [x] `CommentInlayManagerTest.kt` に空文字で `openInputPanel(editor, ReviewCommentLineRange(1, 1), ...)` を実行し、入力パネルが作成されることを確認するテストを追加する（Red を確認済み）
  - [x] `CommentInlayManagerTest.kt` に同じ空 editor で保存して block renderer を追加するテストを追加し、`addBlockInlay` の位置計算を検証する（Red を確認済み）
  - [x] `ReviewCommentEditorLineRangeResolverTest.kt` に空ドキュメントが `ReviewCommentLineRange(1, 1)` へ解決される境界テストを追加し、非空ファイルと JSON 不在時の既存テストを維持する

- [x] Task: 空ドキュメントの最小安全化を実装する（回帰テストコミット: `18a8905`）
  - [x] `ReviewCommentEditorTracker.kt:83-84` の上限を `(document.lineCount - 1).coerceAtLeast(0)` で正規化し、行 0 を `addLineHighlighter` に渡す
  - [x] `CommentInlayManager.kt:262-264,351-352` の上限を同じ方針で正規化し、オフセット 0 を popup / block inlay API に渡す
  - [x] `ReviewCommentLineRange`、JSON スキーマ、ブランチ名ベースの保存処理は変更しない
  - [x] Red テストを再実行し、空ファイルのガター、コンテキストメニュー、保存・復元の例外が解消されたことを確認する（Green）

- [x] Task: 回帰確認とコード品質を検証する
  - [x] 空ドキュメントと通常の 1 行以上のドキュメントの境界ケースを含む関連テストを実行する
  - [x] `.\gradlew.bat test` を実行し、全テストが成功することを確認する
  - [x] `.\gradlew.bat detekt` を実行し、静的解析エラーがないことを確認する
  - [x] `.\gradlew.bat build` を実行し、プロダクションコードとテストコードがコンパイルされることを確認する
  - [x] 新規・変更コードのカバレッジが 80% 以上を満たすか、利用可能なレポートで確認する（JaCoCo/Kover 未設定のため測定不可）
  - [x] 必要なリファクタリング後に関連テストを再実行する（追加リファクタリング不要）
  - [x] Task: Conductor - 'Empty Document Safety Fix' の成果をコミットする
  - [x] Conventional Commits 形式でコード変更をコミットする（`18a8905`、`07622e8`）
  - [x] 完了したタスクの状態と直前のコミット先頭 7 文字を `plan.md` に記録し、計画更新を別コミットする

- [x] Task: Phase 1 Verification & Checkpoint (Refer to `workflow.md`)
  - [x] フェーズ開始時点のチェックポイント SHA `c2fe0db` から `git diff --name-only c2fe0db HEAD` を実行し、変更ファイルを確認する
  - [x] 変更された各コードファイルに対応するテストが存在し、Issue 56 の受け入れ条件を検証していることを確認する
  - [x] `.\gradlew.bat test` と `.\gradlew.bat detekt` を実行し、結果を確認する
  - [x] `.\gradlew.bat build` を実行し、ビルド成功を確認する
  - [x] Task: Conductor - User Manual Verification 'Phase 1 Empty Document Safety Fix' (Protocol in `workflow.md`)
  - [x] 空のファイルを開き、IDE ログに `IllegalArgumentException` が出ないことをユーザーが確認する
  - [x] 空ファイルでガターまたはコンテキストメニューからコメント入力を開き、コメントを保存できることをユーザーが確認する
  - [x] 非空ファイルで既存の行コメント操作が変わっていないことをユーザーが確認する
  - [x] ユーザーの明示的な手動検証承認を得る
  - [x] Task: Conductor - 'Phase 1 Empty Document Safety Fix' の成果をコミットする
  - [x] フェーズチェックポイントコミット `6b9074d` を作成し、その SHA を `plan.md` に記録する

## 変更予定ファイル

- `conductor/tracks/56-empty-document-crash-fix_20260804/spec.md`
- `conductor/tracks/56-empty-document-crash-fix_20260804/plan.md`
- `conductor/tracks/56-empty-document-crash-fix_20260804/evidence_report.md`（Phase 0）
- `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorTracker.kt`
- `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt`
- `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/` 配下の関連テスト

## 検証コマンド

- `.\gradlew.bat test`
- `.\gradlew.bat detekt`
- `.\gradlew.bat build`