# Implementation Plan: 005-inlay-comment-ui

## Phase 0: Discovery & Detailed Design
- [x] Task: コードベースの調査と詳細設計
    - [x] `autonomous-researcher` スキルを活用して、既存のエディタ表示、Inlay管理、ストレージ関連 of コードを探索・調査する
    - [x] `evidence_report.md` を作成し、既存のコンポーネントや IntelliJ Inlay API の具体的な利用方法を特定する
- [x] Task: 計画の具体化
    - [x] 調査結果に基づいて、Phase 1 以降のタスクを対象ファイル名や具体的なクラス定義などを反映した形に書き換える
- [x] Task: Conductor - User Manual Verification 'Phase 0: Discovery & Detailed Design' (Protocol in workflow.md)
- [x] Task: Phase 0 をコミット
 
## Phase 1: CommentInputPanel & Inlay Infrastructure
<!-- 新規ファイル: src/main/kotlin/.../editor/ui/CommentInputPanel.kt -->
<!-- 新規テスト:  src/test/kotlin/.../editor/ui/CommentInputPanelTest.kt -->
- [x] Task: CommentInputPanel UI とコールバックの実装
    - [x] `src/test/kotlin/.../editor/ui/CommentInputPanelTest.kt` を新規作成し、以下を検証するテストを記述する（TDD Red Phase）:
        - `CommentInputPanel` がインスタンス化でき、`textArea`（JTextArea）、`saveButton`、`cancelButton`、`deleteButton` を持つこと
        - Save クリック時に `onSave(text: String)` コールバックが呼ばれること
        - Cancel クリック時に `onCancel()` コールバックが呼ばれること
        - Delete クリック時に `onDelete()` コールバックが呼ばれること
        - 新規作成モード（`existingComment = null`）では Delete ボタンが非表示であること
        - 再編集モード（`existingComment != null`）では Delete ボタンが表示され、既存テキストが `textArea` に入力済みであること
    - [x] `src/main/kotlin/.../editor/ui/CommentInputPanel.kt` に `CommentInputPanel`（`JPanel` 継承）を実装し、テストをパスさせる（TDD Green Phase）:
        - コンストラクタ: `CommentInputPanel(existingComment: String? = null, onSave: (String) -> Unit, onCancel: () -> Unit, onDelete: () -> Unit)`
        - `JTextArea` による複数行入力フィールド
        - Save / Cancel / Delete ボタン
        - Delete は `existingComment == null` の場合に `isVisible = false`
    - [x] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する

<!-- 新規ファイル: src/main/kotlin/.../editor/ui/CommentBlockRenderer.kt -->
<!-- 新規テスト:  src/test/kotlin/.../editor/ui/CommentBlockRendererTest.kt -->
- [x] Task: CommentBlockRenderer (Inlay) の実装
    - [x] `src/test/kotlin/.../editor/ui/CommentBlockRendererTest.kt` を新規作成し、以下を検証するテストを記述する（TDD Red Phase）:
        - `CommentBlockRenderer` が `EditorCustomElementRenderer` を実装すること
        - `calcWidthInPixels()` が 0 より大きい値を返すこと
        - `calcHeightInPixels()` が 0 より大きい値を返すこと
        - クリックコールバック `onClick` がクラスに保持されること
    - [x] `src/main/kotlin/.../editor/ui/CommentBlockRenderer.kt` に `CommentBlockRenderer`（`EditorCustomElementRenderer` 実装）を実装し、テストをパスさせる（TDD Green Phase）:
        - コンストラクタ: `CommentBlockRenderer(text: String, onClick: () -> Unit)`
        - `calcWidthInPixels(inlay)`: エディタ幅に基づく幅を返す
        - `calcHeightInPixels(inlay)`: テキスト行数に基づく高さを返す
        - `paint(inlay, g, targetRegion, textAttributes)`: 丸角ボックスとテキストを描画
        - `mousePressed` イベントで `onClick()` を呼ぶ（`EditorCustomElementRenderer.mousePressed` オーバーライド）
    - [x] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [x] Task: Conductor - User Manual Verification 'Phase 1: CommentInputPanel & Inlay Infrastructure' (Protocol in workflow.md)
- [x] Task: Phase 1 コミットし、本フェーズを完了とする [checkpoint: 03f5c8d]

## Phase 2: Interaction Logic
<!-- 新規ファイル: src/main/kotlin/.../editor/CommentInlayManager.kt -->
<!-- 変更ファイル: src/main/kotlin/.../editor/ReviewCommentGutterIconRenderer.kt -->
<!-- 変更ファイル: src/main/kotlin/.../action/AddReviewCommentAction.kt -->
<!-- 新規テスト:  src/test/kotlin/.../editor/CommentInlayManagerTest.kt -->
- [x] Task: 入力パネルと Inlay 表示の連携ロジックの実装
    - [x] `src/test/kotlin/.../editor/CommentInlayManagerTest.kt` を新規作成し、以下を検証するテストを記述する（TDD Red Phase）:
        - `openInputPanel(editor, lineRange, existingComment?)` を呼ぶと Inlay が追加されること
        - Save コールバック後に InputPanel Inlay が破棄され、BlockRenderer Inlay が追加されること
        - Cancel コールバック後に InputPanel Inlay が破棄されること（新規作成時）
        - Cancel コールバック後に BlockRenderer Inlay が復元されること（再編集時）
        - Delete コールバック後にすべての Inlay が破棄されること
        - `openInputPanel` を BlockRenderer クリックから呼ぶと既存 BlockRenderer Inlay が破棄されること
    - [x] `src/main/kotlin/.../editor/CommentInlayManager.kt` に `CommentInlayManager` を実装し、テストをパスさせる（TDD Green Phase）:
        - `object CommentInlayManager` として実装
        - 内部で `IdentityHashMap<Editor, InlayState>` を保持し、エディタごとに InputPanel / BlockRenderer の Inlay を管理
        - `openInputPanel(editor, lineRange, existingComment?)`: 入力パネル Inlay を `editor.inlayModel.addBlockElement` で追加
        - Save 時: InputPanel Inlay を `Disposer.dispose()` し、`addBlockElement` で BlockRenderer を追加
        - Cancel 時: InputPanel Inlay を `Disposer.dispose()` し、再編集なら BlockRenderer を復元
        - Delete 時: すべて of Inlay を `Disposer.dispose()`
    - [x] `src/main/kotlin/.../editor/ReviewCommentGutterIconRenderer.kt` の `getClickAction()` を変更:
        - `Messages.showInfoMessage` を削除し、`CommentInlayManager.openInputPanel(editor, lineRange, null)` を呼ぶように変更
    - [x] Task: 右クリックメニュー（AddReviewCommentAction）からエディタ上のインライン入力パネルを呼び出すように変更する
        - [x] `src/test/kotlin/.../action/AddReviewCommentActionTest.kt` に、アクション実行時に `CommentInlayManager.openInputPanel` が呼び出されること、および既存のダイアログ表示が削除されていることを検証するテストを記述する（TDD Red Phase）
        - [x] `src/main/kotlin/.../action/AddReviewCommentAction.kt` を変更して `CommentInlayManager.openInputPanel` を呼び出すようにし、テストをパスさせる（TDD Green Phase）
      - [x] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [x] Task: Conductor - User Manual Verification 'Phase 2: Interaction Logic' (Protocol in workflow.md)
- [x] Task: Phase 2 をコミットし、本フェーズを完了とする [checkpoint: 03f5c8d]

## Phase 3: Storage Integration
<!-- 変更ファイル: src/main/kotlin/.../editor/CommentInlayManager.kt (Save/Delete でストレージ連携) -->
<!-- 新規テスト:  src/test/kotlin/.../editor/CommentInlayManagerStorageTest.kt -->
- [x] Task: ローカル JSON ストレージ連携
    - [x] `src/test/kotlin/.../editor/CommentInlayManagerStorageTest.kt` を新規作成し、以下を検証するテストを記述する（TDD Red Phase）:
        - Save コールバック時に `ReviewCommentStorage.save()` が呼ばれること（モック使用）
        - 保存される `ReviewComment` の `filePath`, `lineStart`, `lineEnd`, `comment` が正しいこと
        - Delete コールバック時に `ReviewCommentStorage.save()` が呼ばれ、対象コメントが除外されたドキュメントが保存されること
    - [x] `CommentInlayManager.kt` の Save/Delete ロジックに `ReviewCommentStorage` 連携を実装し、テストをパスさせる（TDD Green Phase）:
        - `openInputPanel` 呼び出し元（`ReviewCommentGutterIconRenderer` または `AddReviewCommentAction`）が `project` を渡し、`ReviewCommentStorage` を初期化
        - Save 時: `UUID.randomUUID().toString()` で ID 生成、`ReviewComment` を作成し、`storage.load()` に追加して `storage.save()`
        - Delete 時: `storage.load()` から該当コメント（ID 一致）を除去して `storage.save()`
        - `createdAt` は `java.time.OffsetDateTime.now()` を ISO 8601 フォーマットで文字列化
    - [x] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [x] Task: Conductor - User Manual Verification 'Phase 3: Storage Integration' (Protocol in workflow.md)
- [x] Task: Phase 3 をコミットし、本フェーズを完了とする [checkpoint: 863ab92]

## Phase 4: 複数コメント表示の不具合修正
<!-- 変更ファイル: src/main/kotlin/.../editor/CommentInlayManager.kt -->
<!-- 変更ファイル: src/main/kotlin/.../editor/ReviewCommentEditorTracker.kt -->
<!-- 変更ファイル: src/main/kotlin/.../editor/ReviewCommentEditorProjectActivity.kt -->
<!-- 変更テスト: src/test/kotlin/.../editor/CommentInlayManagerTest.kt -->
<!-- 変更テスト: src/test/kotlin/.../editor/CommentInlayManagerStorageTest.kt -->
- [x] Task: CommentInlayManager の複数コメント対応リファクタリング [240bc11]
    - [x] EditorState の blockInlay / blockRenderer を単一からマップ管理（commentId → Inlay）に変更する
    - [x] saveComment() で既存ブロックを全破棄せず該当コメントのみ追加/更新するよう修正する
    - [x] cancelComment() / deleteComment() を複数Inlay対応に修正する
    - [x] installInlayClickListener() を複数Inlayのクリック検出に対応させる
    - [x] openInputPanel() の既存コメント編集時にコメントIDを受け渡せるようにする
- [ ] Task: エディタ起動時に保存済み全コメントをInlay表示する処理を追加する
    - [ ] CommentInlayManager.restoreComments() を実装し、保存済みコメントをすべてInlayブロックとして復元する
    - [ ] ReviewCommentEditorTracker.track() または適切なタイミングで復元処理を呼び出す
- [ ] Task: 既存テストの修正と複数コメント表示のテストを追加する
    - [ ] 既存テストが複数コメント対応の新しい内部APIに合うよう修正する
    - [ ] 複数コメント保存・表示の結合テストを追加する
- [ ] Task: Conductor - User Manual Verification 'Phase 4: 複数コメント表示の不具合修正'
