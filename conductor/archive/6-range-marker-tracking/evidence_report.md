# GitHub Issue 6: Phase 0 調査報告

## 1. Discovery Summary (Phase 1)

- **Problem Statement**: コード編集後も、インラインレビューコメントが作成時・読込時に指定された行範囲を追跡し、ファイル保存時に現在位置をブランチ別 JSON へ同期する。
- **Scope**: `Document` 上の `RangeMarker` による行範囲追跡、未解決・解決済みコメントの保存同期、無効化された範囲の `isOutdated` 遷移、既存 JSON との後方互換性を対象とする。
- **Non-Goals**: outdated 状態の視覚表示、列単位の追跡、保存形式の変更、新規 IntelliJ 拡張点の追加は対象外とする。
- **Constraints**: UI・ストレージ・モデル・起動処理の責務を分離し、JSON I/O はストレージ層に留める。範囲削除時もコメントと直近の永続化済み行番号を残す。`RangeMarker` は `Document` に属するため、エディタ単位の Inlay 状態と混在させない。
- **Success Criteria**: 行の増減と複数行編集後に行範囲を保存でき、範囲削除後は `isOutdated = true` を保存できる。同一ファイルの解決済みを含む全コメントを一度に同期し、`isOutdated` を持たない JSON を通常状態として読み込める。

## 2. Codebase Findings (Phase 2)

- **Similar Implementations**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorFileListener.kt:18-33`: ファイルを開いた時点でエディタ追跡を開始する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorTracker.kt:55-61`: エディタの復元と解放を `CommentInlayManager` に委譲する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:31-74`: 新規コメント入力とエディタ状態の解放を扱う。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:100-235`: 保存済みコメントの復元、編集、削除を扱う。復元時は未解決コメントだけを Inlay として表示する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentFileWatcher.kt:25-40`: ブランチ変更または JSON 外部変更時にコメントを再読込する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/storage/ReviewCommentStorage.kt:22-27,53-128`: ブランチ名の決定と `.inline-review-notes/{branch}.json` の全ドキュメントを読み書きする。`encodeDefaults = true` と `ignoreUnknownKeys = true` を設定済みである。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewComment.kt:6`: `ReviewComment` は `@Serializable` で、現在は行範囲と `resolvedAt` を持つ。
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManagerStorageTest.kt:8-104`: `BasePlatformTestCase` 上で作成・削除・編集と JSON 永続化を検証する。
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/model/ReviewCommentSerializationTest.kt:14-47`: JSON の往復変換を検証する。
- **Architecture and Dependency Notes**: Inlay は `CommentInlayManager` のエディタ単位状態に属する一方、保存ファイルは `ReviewCommentStorage` がブランチ単位で管理する。したがって追跡情報は `Document`・コメント ID・ファイル識別子により独立管理し、保存時だけストレージへ更新を依頼する。
- **Reusable Components**: `ReviewCommentStorage.load()` / `save()`、既存の JSON シリアライゼーション、`BasePlatformTestCase`、プロジェクト起動時の Message Bus 接続を再利用する。
- **Estimated Impact Area**: `ReviewComment.kt`、シリアライゼーションテスト、新規 `RangeMarkerManager.kt` とテスト、`CommentInlayManager.kt`、`ReviewCommentEditorProjectActivity.kt`、ストレージ統合テスト。`plugin.xml` とユーザー向け文言は変更不要である。

## 3. Clarifying Questions (Phase 3)

- **Open Questions**: なし。範囲無効化時は位置を保持して `isOutdated` に遷移すること、解決済みを含む全件同期、outdated の UI 非表示はユーザーが確定済みである。
- **User Answers / Delegations**:
  1. 位置追跡と保存同期は、プロジェクト単位の専用トラッカーで分離する。
  2. 無効なマーカーはコメントを削除せず、最後の行番号を保持して `isOutdated = true` を保存する。
  3. 同一ファイルの解決済みを含む全コメントを保存時に同期する。
- **Unresolved Items**: なし。保存コールバックでは UI 操作を行わず、追跡情報の変換とストレージ更新だけを行う。

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- コメント作成または読込時に、行範囲の先頭オフセットから終了行末尾までを `RangeMarker` として登録する。
- コメント前後の行挿入・削除後、保存時に有効なマーカーを現在の開始・終了行へ変換し、対象ファイルのコメント ID に対応する JSON 項目だけを更新する。
- 範囲全体が削除されてマーカーが無効な場合、行範囲を変更せず、同じ JSON 項目の `isOutdated` だけを `true` にする。
- 解決済みコメントは Inlay を表示しないが、マーカーを登録して保存同期の対象に含める。
- コメントの編集・削除・再読込・エディタ解放では、対応するマーカーを置換または破棄して古い追跡情報を残さない。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes

- **Change Targets**: `CommentInlayManager.kt` にコメント ID ごとの `RangeMarker` を追加し、起動処理から保存通知をこの管理クラスへ渡す。
- **Pros**: 新規クラスが少なく、既存のコメント操作に近い。
- **Cons/Risks**: エディタ単位の Inlay 状態に `Document` 単位の追跡と保存責務が混在し、解決済みコメントとエディタ解放後の追跡を正しく扱いにくい。
- **Validation Plan**: 作成・編集・削除・エディタ解放後の追跡破棄と、解決済みコメントの保存同期を統合テストで確認する。

### Option B: Clean Architecture

- **Change Targets**: `RangeMarkerManager` をプロジェクトサービスとして実装し、表示・ストレージ・起動処理からサービス API を呼び出す。
- **Pros**: 追跡状態の所有者が明確で、テスト時にも取得しやすい。
- **Cons/Risks**: サービス登録などの構成変更が必要となり、本トラックの「新規拡張点を追加しない」制約に対して変更が過大になる。
- **Validation Plan**: サービス取得を含む Platform Test で、複数エディタと保存通知を確認する。

### Option C: Pragmatic Balance

- **Change Targets**: 新規 `editor/RangeMarkerManager.kt` をプロジェクト単位の専用コンポーネントとして追加する。`CommentInlayManager.kt` はライフサイクル通知だけを行い、`ReviewCommentEditorProjectActivity.kt` がプロジェクト寿命の Message Bus 接続で保存通知をマネージャーへ渡す。ストレージ更新は `ReviewCommentStorage.kt` の既存 `load()` / `save()` を使用する。
- **Pros**: ユーザーが選択した分離構成を満たし、既存の拡張点を増やさずに Inlay と追跡・永続化の責務を分離できる。
- **Cons/Risks**: 登録置換・破棄の呼び忘れと、保存通知での無効マーカー処理が主なリスクとなる。
- **Validation Plan**: マネージャー単体で行挿入・削除・複数行・無効化を、統合テストで作成・編集・削除・再読込・解決済みを含む保存同期を検証する。

- **Recommended Option**: Option C: Pragmatic Balance。
- **Reason**: 明示された専用トラッカー方針、既存の責務分離、拡張点追加なしという要件を同時に満たすためである。

## 6. 推奨される実装方針 (Implementation Strategy)

- **Architecture Alignment**: `RangeMarkerManager` はコメント ID、`Document`、`RangeMarker`、ファイル識別子を対応付ける。Inlay を作らない解決済みコメントも登録し、UI 状態は保持しない。
- **Logic Changes**: `ReviewComment.isOutdated` を既定値 `false` で追加する。マネージャーは行範囲をオフセットに変換して登録し、保存時に `isValid` を確認して有効なら行範囲を、無効なら `isOutdated` を返す。呼び出し側は対象ファイルの全コメントに ID 単位で反映して保存する。
- **Validation Plan**: まずシリアライゼーションとマネージャーの失敗テストを追加し、Red/Green を確認する。次にストレージ統合テストを追加し、`test` と `detekt` を実行する。Phase 3 では `build` と IDE 上の挿入・削除・保存・再読込の手動検証を行う。

## 7. Evidence and Alignment

- **Source URLs**:
  - URL: https://plugins.jetbrains.com/docs/intellij/documents.html
    - `Document` は Virtual File に対応する編集可能な文字列であり、`FileDocumentManager` から取得できる。文書変更の通知には `FileDocumentManagerListener` または `AppTopics.FILE_DOCUMENT_SYNC` の購読を利用できる。
  - URL: https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html
    - Message Bus のリスナーはアプリケーションまたはプロジェクト単位で接続できる。既存のプロジェクト起動処理で購読を作る方針と整合する。
- **Research Date**: 2026-07-27
- **Key Findings**:
  - `RangeMarker` の位置は `Document` の編集に追従するため、行番号ではなくオフセットを登録時の基準とする。
  - 無効なマーカーではオフセットを参照せず、直近の JSON 行範囲を保持する。
  - `test`、`detekt`、`build` の Gradle タスクを実行できる。Kover 等のカバレッジレポートタスクは未設定である。
- **Local Constraint Alignment**:
  - `conductor/code_styleguides/general.md` の単純性、既存パターン、低結合の原則に、Option C の専用コンポーネントが整合する。
  - `conductor/workflow.md:100-111` の Phase 0 における調査、報告、計画具体化、ユーザー承認の順序に従う。
- **Potential Regressions**: 再読込・編集・削除・エディタ解放時に登録を破棄しない場合、古いマーカーが保存結果を上書きする。保存同期時に解決済みを除外すると受入条件に違反する。
- **Residual Risks / Unknowns**: `RangeMarker` の端点に対する挿入の包含規則は、Phase 1 の行挿入・削除テストで実際のターゲット SDK 上の挙動を固定する。カバレッジ率は専用ツール未導入のため、対象シナリオの自動テストで代替確認する。