# Evidence Report: 005-inlay-comment-ui (Pre-Implementation)

調査日: 2026-06-30

---

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**: エディタ内で直接レビューノートを追加・表示・編集するインライン UI を実装する。現状はガターアイコンクリック時に `Messages.showInfoMessage` でダイアログを表示するだけで、Inlay による永続表示機能が存在しない。
- **[Scope]**:
  - `CommentInputPanel` (JPanel ベースの入力フォーム)
  - `CommentBlockRenderer` (Inlay ブロック要素として行の下に表示するレンダラー)
  - ガターアイコン → 入力パネル → Inlay 表示 の状態遷移ロジック
  - ストレージ（`ReviewCommentStorage`）との Save/Delete 連携
- **[Non-Goals]**:
  - Markdown レンダリング（プレーンテキストのみ）
  - Inlay 上への直接チェックボックス配置（ステータス変更）
- **[Constraints]**:
  - UI 操作・Inlay の追加/削除はすべて EDT 上で実行すること
  - IntelliJ 2025.2.6.2 をターゲット
  - Kotlin + JUnit4 テスト（`BasePlatformTestCase` 継承）
- **[Success Criteria]**:
  - ガターからトリガーして入力パネルが展開される
  - Save で入力パネルが消え、丸角ボックスのテキスト Inlay が表示される
  - シングルクリックで即座に入力パネルに切り替わる
  - Delete でコメントが消え、JSON からも削除される

---

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `editor/ReviewCommentGutterIconRenderer.kt:21-28`: ガターアイコンクリックで `Messages.showInfoMessage` を呼ぶ。ここを入力パネル表示に変更するのが起点
  - `editor/ReviewCommentEditorTracker.kt:65-87`: `refresh()` 内で `ReviewCommentGutterIconRenderer` を `highlighter.gutterIconRenderer` にセットしている。新しい Inlay 管理もここから連携する
  - `storage/ReviewCommentStorage.kt`: `save(document)` / `load()` が実装済み。`ReviewCommentDocument` と `ReviewComment` モデルも完備
  - `model/ReviewComment.kt`: `id`, `filePath`, `lineStart`, `lineEnd`, `comment`, `createdAt`, `resolvedAt` を持つ
  - `model/ReviewCommentDocument.kt`: `comments: List<ReviewComment>` を保持
  - `action/AddReviewCommentAction.kt`: ポップアップメニューからのエントリーポイント。同様に改修候補

- **[Architecture and Dependency Notes]**:
  - パッケージ構成: `editor/`（UI・エディタ連携）、`action/`（アクション）、`model/`（データモデル）、`storage/`（JSON I/O）
  - UI コンポーネントは `editor/` パッケージに配置する（責務境界を守る）
  - Inlay の追加/削除には `editor.inlayModel.addBlockElement(offset, showAbove, priority, renderer)` を使う
  - クリックイベントのハンドリングには `EditorCustomElementRenderer` を実装したクラスで `mousePressed` イベントをオーバーライドする

- **[Reusable Components]**:
  - `ReviewCommentStorage.save()` / `load()` をそのまま利用
  - `ReviewCommentLineRange` モデルをそのまま利用
  - `ReviewCommentEditorTracker` の refresh 機構は維持し、Inlay も同時に管理する拡張ポイントとして活用

- **[Estimated Impact Area]**:
  - 新規ファイル: `editor/ui/CommentInputPanel.kt`、`editor/ui/CommentBlockRenderer.kt`、`editor/CommentInlayManager.kt`
  - 変更ファイル: `editor/ReviewCommentGutterIconRenderer.kt`（クリックアクションを変更）
  - plugin.xml への追加は不要（既存の EditorFactoryListener / postStartupActivity を利用）

---

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. `CommentInputPanel` の Inlay は `showAbove = false`（行の下に表示）を使うか？ → spec より「行の真下に入力パネルを Inlay として挿入」なので `showAbove = false`
  2. コメント ID の生成方法は？ → 既存テストから `UUID` を使っていることが確認できる（`123e4567-...` 形式）。`java.util.UUID.randomUUID().toString()` を使う
  3. `createdAt` のフォーマット → 既存テストから ISO 8601 形式（`2026-06-19T10:00:00+09:00`）を使う

- **[User Answers / Delegations]**:
  1. Q1: `showAbove = false` で行の下に表示 → spec から確定
  2. Q2: UUID ランダム生成 → spec/既存実装から確定
  3. Q3: ISO 8601 形式 → 既存実装から確定

- **[Unresolved Items]**: なし（すべて spec と既存コードから確定）

---

## 4. 将来の修正で期待される挙動 (Expected Behavior)

1. **新規作成フロー**: ガターアイコンをクリック → 該当行直下に `CommentInputPanel` が Inlay として出現 → テキスト入力 → Save クリック → Inlay が `CommentBlockRenderer` に置き換わり → JSON に保存される
2. **再編集フロー**: `CommentBlockRenderer` のシングルクリック → `CommentInputPanel` に切り替わる（既存テキストが入力フィールドに入っている） → Save/Cancel で元の表示に戻る
3. **削除フロー**: 編集中に Delete クリック → Inlay 消去 → JSON からも削除
4. **キャンセルフロー**: 新規作成時は Inlay を削除、再編集時は `CommentBlockRenderer` に戻す

---

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes（最小変更）
- **[Change Targets]**:
  - `ReviewCommentGutterIconRenderer.kt` のクリックアクションを直接変更し、Inlay を追加するロジックを埋め込む
  - `CommentInputPanel`、`CommentBlockRenderer` をインラインクラスとして同ファイルに実装
- **[Pros]**: 変更ファイル数が最小
- **[Cons/Risks]**: 単一ファイルが大きくなりすぎる。テスト分離が困難。責務が混在
- **[Validation Plan]**: 手動でガタークリック → Inlay 表示確認

### Option B: Clean Architecture（分離優先）
- **[Change Targets]**:
  - `editor/ui/CommentInputPanel.kt` — JPanel ベース入力フォーム（独立クラス）
  - `editor/ui/CommentBlockRenderer.kt` — EditorCustomElementRenderer 実装（独立クラス）
  - `editor/CommentInlayManager.kt` — Inlay の挿入・置換・破棄を管理するオブジェクト
  - `ReviewCommentGutterIconRenderer.kt` — `CommentInlayManager` を呼ぶように変更
- **[Pros]**: 責務が明確。各クラスが独立してテスト可能。既存パッケージ構成を崩さない
- **[Cons/Risks]**: ファイル数が増える
- **[Validation Plan]**: 各クラスのユニットテスト + 手動検証

### Option C: Pragmatic Balance（均衡案）
- **[Change Targets]**:
  - `editor/ui/CommentInputPanel.kt` — JPanel ベース入力フォーム
  - `editor/ui/CommentBlockRenderer.kt` — EditorCustomElementRenderer 実装
  - `ReviewCommentGutterIconRenderer.kt` に Inlay 管理ロジックを含める（Manager クラスなし）
- **[Pros]**: ファイル数の増加を抑えつつ責務を分離
- **[Cons/Risks]**: GutterIconRenderer にロジックが集中しすぎる可能性

- **[Recommended Option]**: **Option B (Clean Architecture)**
- **[Reason]**: spec が明確に `CommentInputPanel` と `CommentBlockRenderer` を独立したコンポーネントとして定義している。また `CommentInlayManager` を独立させることで、ガターアイコン以外のエントリーポイント（例: `AddReviewCommentAction`）からも再利用可能になる。テスト分離も容易。

---

## 6. 推奨される実装方針 (Implementation Strategy)

### ファイル構成

```
src/main/kotlin/.../
  editor/
    ui/
      CommentInputPanel.kt         ← 新規: JPanel + JTextArea + Save/Cancel/Delete ボタン
      CommentBlockRenderer.kt      ← 新規: EditorCustomElementRenderer 実装（カスタム描画）
    CommentInlayManager.kt         ← 新規: Inlay の追加・置換・削除ロジック
    ReviewCommentGutterIconRenderer.kt ← 変更: クリック時に CommentInlayManager を呼ぶ

src/test/kotlin/.../
  editor/
    ui/
      CommentInputPanelTest.kt     ← 新規: コールバック検証
      CommentBlockRendererTest.kt  ← 新規: 描画計算・クリックイベント検証
    CommentInlayManagerTest.kt     ← 新規: 状態遷移検証
```

### API 利用方針

- **Inlay 追加**: `editor.inlayModel.addBlockElement(offset, false, false, 0, renderer)`
  - `showAbove = false` → 行の下に表示
  - `relatesToPrecedingText = false`
- **Inlay 削除**: `Disposer.dispose(inlay)` または `inlay.dispose()`
- **クリック検知 (CommentBlockRenderer)**: `EditorCustomElementRenderer` にはデフォルトでマウスリスナーなし。`mouseListener` プロパティを実装するか、`InlayMouseMotionListener` を利用
- **EDT 安全性**: `ApplicationManager.getApplication().invokeLater { ... }` を使用
- **テスト**: `BasePlatformTestCase` を継承し、`myFixture.editor` を利用

### 状態遷移ロジック

```
ガタークリック
  → CommentInlayManager.openInputPanel(editor, lineRange, existingComment?)
    → InputPanel の Inlay を addBlockElement で挿入

Save クリック
  → CommentInlayManager.saveComment(editor, lineRange, text)
    → Storage に保存
    → InputPanel Inlay を dispose
    → CommentBlockRenderer の Inlay を addBlockElement で挿入

Cancel クリック（新規）
  → InputPanel Inlay を dispose

Cancel クリック（再編集）
  → InputPanel Inlay を dispose
  → CommentBlockRenderer の Inlay を addBlockElement で挿入

Delete クリック
  → Storage から削除
  → InputPanel Inlay を dispose

CommentBlockRenderer シングルクリック
  → CommentInlayManager.openInputPanel(editor, lineRange, existingComment)
    → BlockRenderer Inlay を dispose
    → InputPanel の Inlay を addBlockElement で挿入
```

---

## 7. Evidence and Alignment

- **[Source URLs]**:
  - URL: https://plugins.jetbrains.com/docs/intellij/inlay-hints.html (IntelliJ Platform Inlay Hints)
  - URL: https://github.com/JetBrains/intellij-sdk-code-samples (SDK Code Samples)
- **[Research Date]**: 2026-06-30
- **[Key Findings]**:
  - `editor.inlayModel.addBlockElement(offset, relatesToPrecedingText, showAbove, priority, renderer)` で行の下にブロック要素を追加できる
  - `EditorCustomElementRenderer` を実装し `calcWidthInPixels`, `calcHeightInPixels`, `paint` をオーバーライドする
  - JComponent を Inlay として埋め込む場合、`paint` メソッドでコンポーネントの bounds を targetRegion と同期させ、`editor.contentComponent.add(component)` で UI 階層に追加する必要がある
  - すべての UI 操作は EDT で実行すること
  - テストは `BasePlatformTestCase` を継承（既存パターンから確認）
- **[Local Constraint Alignment]**:
  - `conductor/code_styleguides/`: パッケージ構成 `editor/`, `action/`, `model/`, `storage/` を維持
  - UI コンポーネントは `editor/ui/` サブパッケージに配置
- **[Potential Regressions]**:
  - `ReviewCommentGutterIconRenderer` の既存クリックアクション変更による既存 Inlay なし状態への影響（現状は `Messages.showInfoMessage` のみなので破壊的変更はなし）
- **[Residual Risks / Unknowns]**:
  - `CommentBlockRenderer` へのマウスクリック検知方法: `EditorCustomElementRenderer` にはデフォルトでクリックイベントハンドラーがないため、`InlayMouseListener` または `editor.addEditorMouseListener` を使う必要がある（要実装時確認）
  - Inlay 内に JPanel を埋め込む際、フォーカスとキーイベントが editor に横取りされないようにする対策が必要
