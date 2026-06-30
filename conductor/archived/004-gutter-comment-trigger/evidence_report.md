# Evidence Report: GutterIconによるコメント追加トリガーとActionの実装

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**:
  - IntelliJ エディタ上で、コード行の選択またはカーソル位置を起点にレビューコメント追加を開始する UI トリガーが必要だ。
  - 現状の `model` / `storage` は JSON 永続化に寄っており、エディタ文脈からの起動口が分離されている。
- **[Scope]**:
  - `AddReviewCommentAction` の入口設計。
  - ガターの「＋」アイコン表示とクリック処理。
  - 行範囲算出ロジックの UI 非依存化。
  - `plugin.xml` と `MyBundle` の最小更新方針。
- **[Non-Goals]**:
  - コメント本文入力フォームの本実装。
  - `ReviewCommentStorage` への保存連携。
  - 既存コメントの表示、編集、削除。
  - severity / snippet / anchorText の追加。
- **[Constraints]**:
  - Kotlin / Gradle Kotlin DSL / IntelliJ Platform Plugin SDK 前提。
  - 既存の `model` / `storage` 層に UI 責務を混ぜない。
  - ユーザー向け文言は `MyBundle.properties` に寄せる。
  - テストは JUnit 4 系で既存の命名規則に合わせる。
- **[Success Criteria]**:
  - エディタ内で選択範囲またはカーソル行に対応するガターアイコンを表示できる設計が定義される。
  - クリック時に対象ファイルと行範囲をダミーダイアログで確認できる設計が定義される。
  - 既存の JSON 保存系と UI 系の責務境界が明文化される。

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewComment.kt:1-14`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewCommentDocument.kt:1-13`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/storage/ReviewCommentStorage.kt:1-162`
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewCommentSerializationTest.kt:1-34`
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/storage/ReviewCommentStorageTest.kt:1-140`
  - `src/main/resources/META-INF/plugin.xml:1-27`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/MyBundle.kt:1-12`
- **[Architecture and Dependency Notes]**:
  - `model` と `storage` は UI 依存を持たず、JSON 保存に責務が閉じている。
  - `plugin.xml` は現時点で resource bundle のみで、action / listener 登録は未構成だ。
  - `MyBundle` は `DynamicBundle` で実装されており、UI 文言追加の受け皿として再利用できる。
  - `conductor/code_styleguides/architecture_rules.md` は存在せず、現地のコードスタイル判断は `conductor/code_styleguides/general.md:1-23` を参照する必要がある。
  - `build.gradle.kts` と `settings.gradle.kts` には `detekt` の定義が見当たらないため、Phase 0 では未実施理由を記録する扱いが妥当だ。
- **[Reusable Components]**:
  - `ReviewComment` / `ReviewCommentDocument`。
  - `ReviewCommentStorage` の branch 単位 JSON 保存ロジック。
  - `TemporaryFolder` を使う JUnit 4 テストパターン。
  - `MyBundle` と `messages/MyBundle.properties` の resource bundle 追加方針。
- **[Estimated Impact Area]**:
  - `action` パッケージ。
  - `editor` パッケージ。
  - `src/main/resources/messages/`。
  - `src/main/resources/META-INF/plugin.xml`。
  - `src/test/kotlin/.../action` と `src/test/kotlin/.../editor`。

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. Phase 0 時点で設計判断を止める未確定事項は見つからなかった。
- **[User Answers / Delegations]**:
  1. ユーザー手動検証は必須であり、次段階で検証計画を提示して確認を取る前提にした。
- **[Unresolved Items]**:
  - `Conductor - User Manual Verification 'Phase 0'` は自動化できないため、ユーザー確認待ちだ。
  - Phase 0 の成果コミットは未実施だ。

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- エディタで選択範囲がある場合は、その開始行を起点にガターの「＋」アイコンを表示する。
- 選択がない場合は、カーソル現在行に「＋」アイコンを表示する。
- アイコンをクリックすると `AddReviewCommentAction` 相当の処理が起動し、対象ファイルと 1 始まりの行範囲をダミーダイアログで確認できる。
- 選択範囲やカーソル位置が変わったときは、古い highlighter を残さずに表示を更新する。
- 行範囲算出ロジックは UI API から分離され、純粋関数として unit test で検証できる。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes
- **[Change Targets]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListener.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRange.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRangeResolver.kt`
- **[Pros]**:
  - 差分が小さい。
  - UI と純粋ロジックの境界を維持しやすい。
  - 既存の `model` / `storage` に波及しない。
- **[Cons/Risks]**:
  - editor 状態管理が listener に寄り、責務が膨らみやすい。
  - caret / selection 更新の重複処理が発生しやすい。
- **[Validation Plan]**:
  - 行範囲の unit test。
  - action 表示文言の unit test。
  - IDE 上でのガター更新の手動確認。

### Option B: Clean Architecture
- **[Change Targets]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterController.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListener.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt`
- **[Pros]**:
  - highlighter 管理を controller に集約できる。
  - listener / renderer / action の責務がより明確になる。
- **[Cons/Risks]**:
  - ファイル数と配線が増える。
  - `plugin.xml` とプロジェクトサービス定義が必要になる可能性がある。
- **[Validation Plan]**:
  - controller 単体テスト。
  - listener 解放のテスト。
  - 手動で editor lifecycle を追う検証。

### Option C: Pragmatic Balance
- **[Change Targets]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt:1-41`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorLineRangeResolver.kt:1-18`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFactoryListener.kt:1-92`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt:1-42`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRange.kt:1-12`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentLineRangeResolver.kt:1-10`
- **[Pros]**:
  - 行範囲算出を純粋ロジックとして固定できる。
  - action と listener を分けつつ、controller を増やさない。
  - 既存構造への侵入が最小限だ。
- **[Cons/Risks]**:
  - listener へ UI 更新責務が残る。
  - 将来、ガター状態が増えると再分割が必要になる。
- **[Validation Plan]**:
  - line range の unit test。
  - presentation 文字列の unit test。
  - `./gradlew test` と `./gradlew build`。
  - IDE 上の manual verification。

- **[Recommended Option]**:
  - Option C
- **[Reason]**:
  - このリポジトリは既に model / storage を分離しており、UI 側も過剰設計せずに分割するのが自然だ。
  - 追加したいのは「起動口」と「ガター表示」であって、巨大な状態管理層ではない。
  - したがって、純粋ロジックだけを切り出して最小差分で進める Option C が最も妥当だ。

## 6. 推奨される実装方針 (Implementation Strategy)

- **[Architecture Alignment]**: `action`、`editor`、`model`、`storage`、`resources` を分離し、UI 依存は `action` / `editor` に閉じる。
- **[Logic Changes]**: 行範囲算出を `ReviewCommentLineRangeResolver` に分離し、editor 由来の selection / caret 変換は `ReviewCommentEditorLineRangeResolver` に集約する。
- **[Validation Plan]**: unit test で純粋ロジックを固定し、`./gradlew test`、`./gradlew build`、最後に IDE でガター表示と action 発火を手動確認する。

## 7. Evidence and Alignment

- **[Source URLs]**:
  - `https://github.com/jetbrains/intellij-sdk-docs/blob/main/topics/tutorials/editor_basics/editor_events.md`
  - `https://github.com/jetbrains/intellij-sdk-docs/blob/main/topics/tutorials/editor_basics/coordinates_system.md`
  - `https://github.com/jetbrains/intellij-sdk-docs/blob/main/topics/appendix/resources/ep_lists/_generated/generated_intellij_platform_extension_point_list.md`
- **[Research Date]**: 2026-06-27
- **[Key Findings]**:
  - `AnAction` は `plugin.xml` の `<actions>` で登録できる。
  - `CommonDataKeys.EDITOR` から editor 文脈を取得するのが標準だ。
  - editor 追従は `SelectionListener` / `CaretListener` と editor lifecycle 管理で扱うのが妥当だ。
  - gutter 表示は `RangeHighlighter` と `GutterIconRenderer` を使う方針が JetBrains の資料と整合する。
- **[Local Constraint Alignment]**:
  - `conductor/code_styleguides/general.md:1-23` の「読みやすさ」「一貫性」「単純さ」「保守性」に沿って、UI と純粋ロジックを分離する方針にした。
  - `AGENTS.local.md` が要求する `architecture_rules.md` は存在しなかったため、確認可能なローカル規約として `general.md` を参照した。
  - 既存の `model` / `storage` 層には UI 責務を持ち込まない設計にした。
- **[Potential Regressions]**:
  - caret / selection の更新頻度が高いので、古い highlighter の破棄漏れがあると表示が残留する。
  - action 登録を editor popup に入れる場合、`CommonDataKeys.VIRTUAL_FILE` が無いコンテキストでの無効化が必要だ。
  - `plugin.xml` に listener を足す場合、editor release 時の解放漏れを起こしやすい。
- **[Residual Risks / Unknowns]**:
  - IDE 上での見え方は自動テストだけでは完全に保証できないため、Phase 0 の manual verification が必須だ。
  - `detekt` は現時点で build scripts に定義がなく、実行可否は未整備だ。
