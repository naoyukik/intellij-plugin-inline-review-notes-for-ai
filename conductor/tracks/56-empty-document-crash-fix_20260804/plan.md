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

- [~] Task: Phase 0 Verification & Checkpoint (Refer to `workflow.md`)
  - [x] ベースラインとして `.\gradlew.bat test` を実行し、既存テストが成功することを確認する
  - [x] `.\gradlew.bat detekt` を実行し、静的解析結果を確認する
  - [ ] Task: Conductor - User Manual Verification 'Phase 0 Discovery & Detailed Design' (Protocol in `workflow.md`)
  - [x] ユーザーに証拠レポートと Phase 1 計画が期待どおりか確認し、明示的な承認を得る
  - [ ] Task: Conductor - 'Phase 0 Discovery & Detailed Design' の成果をコミットする
  - [ ] 変更ファイルを確認し、`conductor(checkpoint): Checkpoint end of Phase 0` 相当のコミットを作成する
  - [ ] コミット SHA を `plan.md` に記録し、計画更新を別コミットとして記録する

## Phase 1: Empty Document Safety Fix

- [ ] Task: TDD で Issue 56 の回帰テストを追加する
  - [ ] `ReviewCommentEditorFileListenerTest.kt` に空文字・JSON 不在で `ReviewCommentEditorTracker.track` を実行し、ガター highlighter が準備されることを確認する再現テストを追加する（Red）
  - [ ] `CommentInlayManagerTest.kt` に空文字で `openInputPanel(editor, ReviewCommentLineRange(1, 1), ...)` を実行し、入力パネルが作成されることを確認するテストを追加する（Red）
  - [ ] `CommentInlayManagerTest.kt` に同じ空 editor で保存して block renderer を追加するテストを追加し、`addBlockInlay` の位置計算を検証する（Red）
  - [ ] `ReviewCommentEditorLineRangeResolverTest.kt` に空ドキュメントが `ReviewCommentLineRange(1, 1)` へ解決される境界テストを追加し、非空ファイルと JSON 不在時の既存テストを維持する

- [ ] Task: 空ドキュメントの最小安全化を実装する
  - [ ] `ReviewCommentEditorTracker.kt:83-84` の上限を `(document.lineCount - 1).coerceAtLeast(0)` で正規化し、行 0 を `addLineHighlighter` に渡す
  - [ ] `CommentInlayManager.kt:262-264,351-352` の上限を同じ方針で正規化し、オフセット 0 を popup / block inlay API に渡す
  - [ ] `ReviewCommentLineRange`、JSON スキーマ、ブランチ名ベースの保存処理は変更しない
  - [ ] Red テストを再実行し、空ファイルのガター、コンテキストメニュー、保存・復元の例外が解消されたことを確認する（Green）

- [ ] Task: 回帰確認とコード品質を検証する
  - [ ] 空ドキュメントと通常の 1 行以上のドキュメントの境界ケースを含む関連テストを実行する
  - [ ] `.\gradlew.bat test` を実行し、全テストが成功することを確認する
  - [ ] `.\gradlew.bat detekt` を実行し、静的解析エラーがないことを確認する
  - [ ] `.\gradlew.bat build` を実行し、プロダクションコードとテストコードがコンパイルされることを確認する
  - [ ] 新規・変更コードのカバレッジが 80% 以上を満たすか、利用可能なレポートで確認する
  - [ ] 必要なリファクタリング後に関連テストを再実行する
  - [ ] Task: Conductor - 'Empty Document Safety Fix' の成果をコミットする
  - [ ] Conventional Commits 形式でコード変更をコミットする
  - [ ] 完了したタスクの状態と直前のコミット先頭 7 文字を `plan.md` に記録し、計画更新を別コミットする

- [ ] Task: Phase 1 Verification & Checkpoint (Refer to `workflow.md`)
  - [ ] フェーズ開始時点のチェックポイント SHA から `git diff --name-only <previous_checkpoint_sha> HEAD` を実行し、変更ファイルを確認する
  - [ ] 変更された各コードファイルに対応するテストが存在し、Issue 56 の受け入れ条件を検証していることを確認する
  - [ ] `.\gradlew.bat test` と `.\gradlew.bat detekt` を実行し、結果を確認する
  - [ ] `.\gradlew.bat build` を実行し、ビルド成功を確認する
  - [ ] Task: Conductor - User Manual Verification 'Phase 1 Empty Document Safety Fix' (Protocol in `workflow.md`)
  - [ ] 空のファイルを開き、IDE ログに `IllegalArgumentException` が出ないことをユーザーが確認する
  - [ ] 空ファイルでガターまたはコンテキストメニューからコメント入力を開き、コメントを保存できることをユーザーが確認する
  - [ ] 非空ファイルで既存の行コメント操作が変わっていないことをユーザーが確認する
  - [ ] ユーザーの明示的な手動検証承認を得る
  - [ ] Task: Conductor - 'Phase 1 Empty Document Safety Fix' の成果をコミットする
  - [ ] フェーズチェックポイントコミットを作成し、その SHA を `plan.md` に記録する

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