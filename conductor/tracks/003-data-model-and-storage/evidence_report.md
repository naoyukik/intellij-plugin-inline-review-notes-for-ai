# Evidence Report: データモデルとJSON保存機能の実装

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**:
  - インラインレビューコメントを表現する `ReviewComment` を定義し、現在の Git ブランチ名に対応する JSON ファイルへ永続化するストレージを実装する。
- **[Scope]**:
  - コメントのデータモデル定義
  - ブランチ名のサニタイズ
  - `.inline-review-notes/{branch}.json` の読み書き
  - `.gitignore` への `.inline-review-notes/` 追記
  - ブランチ取得失敗時のフォールバック
- **[Non-Goals]**:
  - UI 実装
  - RangeMarker の追跡
  - チーム共有や外部 DB
  - 列単位の座標管理
- **[Constraints]**:
  - 既存のテンプレート構成を崩さない
  - JSON I/O はストレージ層に閉じる
  - 既存の Kotlin / IntelliJ Platform 構成に合わせる
- **[Success Criteria]**:
  - `ReviewComment` が JSON で正しく round-trip する
  - ブランチ名がファイル名として安全に変換される
  - 保存時に `.inline-review-notes/` が作成される
  - `.gitignore` に同パスが重複なく追記される
  - `./gradlew test` と `./gradlew build` が成功する

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/hooks/CodexHookResponse.kt`: `kotlinx.serialization` を使う既存例がある。
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/MyPluginTest.kt`: IntelliJ Platform テストの既存スタイルがある。
  - `build.gradle.kts`: 依存は最小で、serialization は未設定だった。
- **[Architecture and Dependency Notes]**:
  - モデルとストレージは `src/main/kotlin/.../model` と `src/main/kotlin/.../storage` に分離する。
  - Git ブランチ取得は外部 `git` コマンドを使い、失敗時は `default` にフォールバックする。
- **[Reusable Components]**:
  - `kotlinx.serialization` の既存利用パターン
  - `BasePlatformTestCase` を使う IntelliJ Platform テストの慣習
- **[Estimated Impact Area]**:
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `conductor/tech-stack.md`
  - 新規 `model/` と `storage/` パッケージ
  - 新規 unit test ファイル

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. Git ブランチ取得は IntelliJ Git API か外部 `git` コマンドか。
  2. ブランチ取得失敗時のフォールバック文字列は何か。
  3. `.gitignore` 追記は既存内容を保持したまま冪等に行うか。
- **[User Answers / Delegations]**:
  1. 外部 `git` コマンドを採用する。
  2. フォールバックは `default` とする。
  3. 既存内容を保持し、`.inline-review-notes/` は重複追加しない。
- **[Unresolved Items]**:
  - なし

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- `ReviewComment` は `id`, `filePath`, `lineStart`, `lineEnd`, `comment`, `createdAt`, `resolvedAt` を持つ。
- ファイル保存形式は `{"version":"1.0","comments":[...]}` である。
- ブランチ名の `/` や `#` などは `_` に置換される。
- `.inline-review-notes/` がなければ作成される。
- `.gitignore` に `.inline-review-notes/` が存在しなければ追記される。
- Git ブランチが取得できない場合は `default.json` を使う。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes
- **[Change Targets]**:
  - `model/ReviewComment.kt`
  - `model/ReviewCommentDocument.kt`
  - `storage/ReviewCommentStorage.kt`
  - `build.gradle.kts`
- **[Pros]**:
  - 差分が小さい
  - 既存構成を壊しにくい
- **[Cons/Risks]**:
  - ブランチ解決や JSON 周りが一体化しやすい
- **[Validation Plan]**:
  - unit test で serialization, sanitize, read/write を確認

### Option B: Clean Architecture
- **[Change Targets]**:
  - `model/`
  - `storage/`
  - `storage/branch/`
  - `storage/path/`
- **[Pros]**:
  - 責務分離が明確
- **[Cons/Risks]**:
  - 初期実装としては過剰
- **[Validation Plan]**:
  - 依存方向ごとに個別テストを追加

### Option C: Pragmatic Balance
- **[Change Targets]**:
  - `model/ReviewComment.kt`
  - `model/ReviewCommentDocument.kt`
  - `storage/ReviewCommentStorage.kt`
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `conductor/tech-stack.md`
- **[Pros]**:
  - 依存追加を最小化しつつ、仕様は満たせる
  - テストしやすい
- **[Cons/Risks]**:
  - Git CLI 依存が残る
- **[Validation Plan]**:
  - `./gradlew test`
  - `./gradlew build`

- **[Recommended Option]**:
  - Option C
- **[Reason]**:
  - 現状はテンプレート段階であり、機能の基盤となるデータ層を素早く安定化させる方が重要だ。Git API 依存を増やさず、JSON 保存とテストの整合を先に固めるのが最も実務的である。

## 6. 推奨される実装方針 (Implementation Strategy)

- **[Architecture Alignment]**: `model` は純粋データ、`storage` は I/O とブランチ解決だけを担当する。
- **[Logic Changes]**: `kotlinx.serialization` で JSON を保存し、保存時に `.gitignore` を冪等更新する。
- **[Validation Plan]**: unit test, `./gradlew test`, `./gradlew build`。

## 7. Evidence and Alignment

- **[Source URLs]**:
  - `project.md`
  - `conductor/product.md`
  - `conductor/tech-stack.md`
  - `conductor/tracks/003-data-model-and-storage/spec.md`
  - `conductor/tracks/003-data-model-and-storage/plan.md`
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/hooks/CodexHookResponse.kt`
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/MyPluginTest.kt`
- **[Research Date]**: 2026-06-19
- **[Key Findings]**:
  - 既存コードはテンプレートのままで、モデルとストレージは未実装だった。
  - `kotlinx.serialization` はプロジェクト方針に含まれていたが、Gradle 設定には未追加だった。
  - `detekt` はこのリポジトリではタスク未定義だった。
- **[Local Constraint Alignment]**:
  - `conductor/code_styleguides/` との整合: 一般原則に従い、単純で責務分離した構成にする。
- **[Potential Regressions]**:
  - 外部 `git` コマンド未導入環境でのブランチ取得失敗
  - `.gitignore` の改行形式差異
- **[Residual Risks / Unknowns]**:
  - `detekt` タスク未設定のため、静的解析は未実施
