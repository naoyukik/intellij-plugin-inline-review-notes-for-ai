# Issue 56 空ドキュメントのクラッシュ修正 調査レポート

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**: 空の `Document` では `document.lineCount - 1` が `-1` となり、`coerceIn(0, -1)` が `IllegalArgumentException` を発生させる。
- **[Scope]**: エディタ追跡の行ハイライト、入力ポップアップの表示位置、コメントブロックインレイの位置計算を空ドキュメントに対応させる。
- **[Non-Goals]**: `ReviewCommentLineRange`、JSON スキーマ、ブランチ名ベースの保存先、ストレージの JSON 不在時の仕様、非空ファイルの行追跡アルゴリズムは変更しない。
- **[Constraints]**: 既存の Kotlin / IntelliJ Platform SDK / JUnit 構成を維持し、UI・ストレージ・モデルの責務分離を崩さない。対象ブランチ要件については、ユーザーの明示承認により現在のブランチで継続する。
- **[Success Criteria]**: 空ファイルかつ対応 JSON 不在で、ファイルを開く、ガターまたはコンテキストメニューから入力を開く、コメントを保存・復元する各操作が例外なく完了し、論理行範囲 `1..1` と既存の非空ファイル挙動を維持する。

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorTracker.kt:72-94`: `refresh` が選択・キャレットから解決した行範囲を行ハイライトとガター renderer に接続する。`lineCount - 1` を上限にした `coerceIn` が直接のクラッシュ箇所である。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:31-59`: `openInputPanel` が `createPopupLocation` で位置を算出する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:257-274`: `addBlockInlay` が保存・復元時のコメント表示位置を算出する。同じ `coerceIn(0, lineCount - 1)` がある。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:347-356`: `createPopupLocation` が同じ行インデックスを visual position と画面座標に変換する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorLineRangeResolver.kt:7-16`: 空ドキュメントでもキャレットの論理行 0 を論理行 `1` として `ReviewCommentLineRange(1, 1)` に保つ責務を持つ。
- **[Architecture and Dependency Notes]**: アクション／ガター renderer → `CommentInlayManager` → `ReviewCommentStorage` の外側から内側への依存は維持されている。今回の修正はエディタ位置計算層だけに限定し、ストレージ層には波及させない。
- **[Reusable Components]**: `ReviewCommentEditorLineRangeResolver`、`ReviewCommentStorage.load()` の JSON 不在時の空一覧処理、既存の `BasePlatformTestCase` テスト基盤を再利用する。
- **[Estimated Impact Area]**: 本番コードは `ReviewCommentEditorTracker.kt` の 1 箇所と `CommentInlayManager.kt` の 2 箇所、テストは同じ editor パッケージの既存テストに限定する。

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. Track ID を含むブランチへ切り替えられない状態での継続可否。
- **[User Answers / Delegations]**:
  1. ユーザーは `56-fixed-json-file-was-missing` ブランチでの継続を明示承認した。
- **[Unresolved Items]**: 実装後の IntelliJ test fixture 上で、空ドキュメントの `getLineStartOffset(0)`、`addLineHighlighter(0, ...)`、`addBlockElement(0, ...)` が期待どおり受け付けられることは回帰テストで確認する。

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- 空ドキュメントは論理上の 1 行目として扱い、`ReviewCommentEditorLineRange(1, 1)` を維持する。
- IntelliJ API に渡す行インデックスは、空ドキュメントでは `0`、非空ドキュメントでは対象行の有効範囲に正規化する。
- 空ドキュメントを開いたとき、行ハイライトとガター追加ボタンの準備で例外を発生させない。
- 空ドキュメントで入力ポップアップを開いたとき、位置計算で例外を発生させない。
- 空ドキュメントへのコメント保存・復元時、ブロックインレイの位置計算で例外を発生させない。
- 対応 JSON が存在しない場合は、`ReviewCommentStorage.load()` の既存仕様どおり空コメント一覧として扱う。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes

- **[Change Targets]**: `ReviewCommentEditorTracker.kt:83`、`CommentInlayManager.kt:263`、`CommentInlayManager.kt:351` の上限値を、`(lineCount - 1).coerceAtLeast(0)` としてから行インデックスを正規化する。
- **[Pros]**: 変更範囲が最小で、既存の行範囲・ストレージ・UI責務を変更せず、Issue の直接原因を除去できる。
- **[Cons/Risks]**: 同じ正規化式が 3 箇所に残るため、将来の変更時に同期漏れのリスクがある。
- **[Validation Plan]**: 空ドキュメントの tracker、入力ポップアップ、保存・復元インレイの回帰テストと、既存の非空ファイルテストを実行する。

### Option B: Clean Architecture

- **[Change Targets]**: editor パッケージに共有の位置正規化コンポーネントを追加し、tracker と `CommentInlayManager` から利用する。
- **[Pros]**: 正規化ルールを一元化でき、重複を防げる。
- **[Cons/Risks]**: 新しい抽象化とファイルが増え、単純な境界値修正に対して変更範囲が広がる。今回の仕様にない責務分割を導入することになる。
- **[Validation Plan]**: 共有コンポーネントの境界値テストに加え、全 editor 導線の統合テストを実行する。

### Option C: Pragmatic Balance

- **[Change Targets]**: `CommentInlayManager` 内ではローカル helper で popup と block inlay の正規化を共有し、tracker には同じ境界条件を明示した式を置く。
- **[Pros]**: `CommentInlayManager` 内の重複を減らしつつ、新規の共有 API を増やさない。
- **[Cons/Risks]**: tracker と manager の間ではルールが重複し、最小案より差分がやや増える。
- **[Validation Plan]**: 空・1行・複数行の境界テスト、既存の保存・復元テスト、静的解析と build を実行する。

- **[Recommended Option]**: Option A: Minimal Changes
- **[Reason]**: 仕様が明確に位置計算 3 箇所の安全化へ限定されており、既存責務と非空ファイル挙動を最も安全に維持できる。3 箇所を同一の明示的な式で修正し、回帰テストで同期を検証する。

## 6. 推奨される実装方針 (Implementation Strategy)

- **[Architecture Alignment]**: `ReviewCommentEditorTracker` と `CommentInlayManager` の既存責務内で位置計算だけを修正し、`ReviewCommentLineRange`、JSON I/O、UI コンポーネントの責務は変更しない。
- **[Logic Changes]**: `maxLineIndex = (document.lineCount - 1).coerceAtLeast(0)` を使い、`(lineRange.startLine - 1).coerceIn(0, maxLineIndex)` を行ハイライト、ブロックインレイ、ポップアップ位置で使用する。
- **[Validation Plan]**: まず回帰テストを追加して現状の `IllegalArgumentException` を Red で確認し、修正後に同じテストを Green で確認する。続けて `test`、`detekt`、`build` と既存 editor/storage テストを実行する。

## 7. Evidence and Alignment

- **[Source URLs]**:
  - https://github.com/naoyukik/intellij-plugin-inline-review-notes-for-ai/issues/56
  - https://plugins.jetbrains.com/docs/intellij/documents.html
  - https://github.com/JetBrains/intellij-community/blob/master/platform/core-impl/src/com/intellij/util/DocumentUtil.java
  - https://github.com/JetBrains/intellij-community/blob/master/platform/editor-ui-api/src/com/intellij/openapi/editor/InlayModel.java
- **[Research Date]**: 2026-08-05
- **[Key Findings]**:
  - Issue #56 のスタックトレースは `ReviewCommentEditorTracker.refresh` の `coerceIn(0, -1)` を示している。
  - IntelliJ の `DocumentUtil.isValidLine` は `lineCount == 0` の場合に論理行 0 を有効扱いする。
  - `InlayModel.addBlockElement` はドキュメントオフセットを受け取り、空ドキュメントではオフセット 0 を使用する方針と整合する。
- **[Local Constraint Alignment]**:
  - `conductor/workflow.md` の Phase 0、TDD、`detekt`、`test`、`build` 手順に従う。
  - `spec.md` の対象外である JSON スキーマ、保存先、モデル、拡張点には変更を加えない。
- **[Potential Regressions]**: 行数が 1 以上のドキュメントで既存の行インデックスが変わらないこと、選択範囲と保存済みコメントの論理行が変わらないことを確認する。
- **[Residual Risks / Unknowns]**: IDE の実バージョン差による空ドキュメント API の細かな表示位置は、test fixture とユーザー手動検証で最終確認する。