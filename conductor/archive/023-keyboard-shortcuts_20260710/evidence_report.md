# Evidence Report Template (Pre-Implementation)

`autonomous-researcher` の Phase 1-4 を完了した後に提出する実装前レポートのテンプレートである。

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**: CommentInputPanelのDelete/Cancel/Saveボタンにキーボード操作を対応させ、キーボード主体のユーザーでも快適に操作できるようにする。
- **[Scope]**:
  - Saveボタンのキーボードショートカット（Ctrl+Enter/Cmd+Enter）
  - Tabキーによるフォーカス移動（textArea → saveButton → cancelButton → deleteButton（表示時のみ））
  - Shift+Tabによる逆順フォーカス移動
  - フォーカス時のEnterキーによるAction実行（JButtonのデフォルト動作）
- **[Non-Goals]**:
  - Deleteボタンへの専用ショートカットキー割り当て
  - IntelliJ Keymap（actionId）へのショートカット登録
  - Editorペインからの直接のショートカット起動
- **[Constraints]**:
  - 既存のマウスクリック操作に影響を与えない
  - IntelliJの標準Keymap設定を汚染しない（actionIdを新規登録しない）
  - macOS / Windows / Linuxで一貫した動作を提供する
- **[Success Criteria]**:
  - テキストエリアでCtrl+Enter（macOSではCmd+Enter）を押下するとSaveが実行されること
  - Escapeキーを押下するとポップアップが閉じられること（既存動作の確認）
  - Tabキーでテキストエリアからボタンへフォーカスが移動すること
  - 各ボタンがフォーカス状態でEnterキーを押下すると対応するActionが実行されること
  - 新規コメントモードではDeleteボタンが非表示であり、Tabフォーカス順からも除外されること
  - 既存のマウスクリック操作が引き続き正常に動作すること
  - CommentInputPanelTestにキーボード操作のテストを追加し、全てのテストがパスすること

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanel.kt:11`: CommentInputPanelクラスの定義。JPanelを継承し、textArea、saveButton、cancelButton、deleteButtonを保持。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:50`: CommentInputPanelのインスタンス生成。JBPopupとして作成され、setCancelKeyEnabled(true)でEscapeキー対応済み。
  - `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanelTest.kt:8`: 既存テスト。コンポーネントの暴露とクリック操作を検証。
- **[Architecture and Dependency Notes]**:
  - CommentInputPanelはUIコンポーネントで、.editor.uiパッケージに存在。
  - CommentInlayManagerはeditorパッケージにあり、CommentInputPanelを使用。
  - JBPopupとして作成され、Escapeキーで閉じる機能は実装済み。
- **[Reusable Components]**:
  - CommentInputPanelのtextArea、saveButton、cancelButton、deleteButtonは既存。
  - JBPopupのsetCancelKeyEnabled(true)はEscapeキー対応に活用可能。
- **[Estimated Impact Area]**:
  - CommentInputPanel.kt: InputMap/ActionMapの追加、FocusTraversalPolicyの実装
  - CommentInputPanelTest.kt: キーボード操作のテスト追加

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. macOSとWindowsでCtrlとMetaを切り替える方法は？
     - IntelliJ Platformでは`SystemInfo.isMac`でプラットフォーム判定が可能か確認が必要
     - または`KeyStroke.getKeyStroke`でプラットフォームに応じたモディファイアキーを取得可能か
  2. Tabフォーカス移動の実装方法は？
     - カスタムFocusTraversalPolicyを実装する
     - フォーカス順を定義する（textArea → saveButton → cancelButton → deleteButton（表示時のみ））
  3. テストの作成方法は？
     - 既存のCommentInputPanelTestを参考に、キーボード操作のテストを追加
     - IntelliJ Platform Test Frameworkを使用
- **[User Answers / Delegations]**:
  1. プラットフォーム判定: SystemInfo.isMacを使用してプラットフォーム判定を行い、CtrlとMetaを切り替える
  2. フォーカス管理: カスタムFocusTraversalPolicyを実装し、フォーカス順を定義する
  3. テスト方法: 既存のCommentInputPanelTestを参考に、キーボード操作のテストを追加する
- **[Unresolved Items]**:
  - なし

## 4. 将来の修正で期待される挙動 (Expected Behavior)
本調査結果に基づき、将来的な修正（実装トラック）において実現すべき挙動を定義する。
- **Saveショートカット**: テキストエリアでCtrl+Enter（Windows/Linux）またはCmd+Enter（macOS）を押下すると、SaveボタンのActionが実行される。
- **Tabフォーカス移動**: TabキーでtextArea → saveButton → cancelButton → deleteButton（表示時のみ）→ textAreaとフォーカスが循環する。Shift+Tabで逆順にフォーカスが移動する。
- **EnterキーによるAction実行**: Save/Cancel/Delete各ボタンがフォーカス状態でEnterキーを押下すると、対応するActionが実行される（JButtonのデフォルト動作）。
- **Escapeキー**: 既存のJBPopup.setCancelKeyEnabled(true)により、Escapeキーでポップアップが閉じられる。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes
- **[Change Targets]**:
  - CommentInputPanel.kt: InputMap/ActionMapの追加、FocusTraversalPolicyの実装
  - CommentInputPanelTest.kt: キーボード操作のテスト追加
- **[Pros]**:
  - 変更範囲が最小限
  - 既存コードの構造を維持
- **[Cons/Risks]**:
  - フォーカス管理が複雑になる可能性
  - プラットフォーム判定のロジックが散在する可能性
- **[Validation Plan]**:
  - 既存テストの実行
  - キーボード操作のテスト追加
  - 手動検証（macOS/Windows/Linux）

### Option B: Clean Architecture
- **[Change Targets]**:
  - CommentInputPanel.kt: InputMap/ActionMapの追加、FocusTraversalPolicyの実装
  - CommentInputPanelTest.kt: キーボード操作のテスト追加
  - 新規ヘルパークラス: プラットフォーム判定ロジックの分離
- **[Pros]**:
  - 責務の分離が明確
  - テスト容易性の向上
- **[Cons/Risks]**:
  - 新規ファイルの追加
  - 過剰な抽象化の可能性
- **[Validation Plan]**:
  - 既存テストの実行
  - キーボード操作のテスト追加
  - 手動検証（macOS/Windows/Linux）

### Option C: Pragmatic Balance
- **[Change Targets]**:
  - CommentInputPanel.kt: InputMap/ActionMapの追加、FocusTraversalPolicyの実装
  - CommentInputPanelTest.kt: キーボード操作のテスト追加
- **[Pros]**:
  - 実用性と保守性のバランス
  - 既存パターンへの準拠
- **[Cons/Risks]**:
  - フォーカス管理の複雑さ
- **[Validation Plan]**:
  - 既存テストの実行
  - キーボード操作のテスト追加
  - 手動検証（macOS/Windows/Linux）

- **[Recommended Option]**: Option A: Minimal Changes
- **[Reason]**: 既存コードの構造を維持し、変更範囲を最小限に抑えることが最適。IntelliJ Platformの標準的なSwing機能を使用し、過剰な抽象化を避ける。ユーザーがOption Aを採用すると回答。

## 6. 推奨される実装方針 (Implementation Strategy)
- **[Architecture Alignment]**: CommentInputPanel.ktにInputMap/ActionMapを追加し、FocusTraversalPolicyを実装。既存のパッケージ構造を維持。
- **[Logic Changes]**:
  - textAreaにInputMap（WHEN_FOCUSED）でCtrl+EnterのKeyStrokeを登録
  - 対応するActionMapでsaveButton.doClick()を呼び出す
  - プラットフォーム判定（SystemInfo.isMac）でCtrlとMetaを切り替える
  - カスタムFocusTraversalPolicyを実装し、フォーカス順を定義
  - deleteButtonの表示状態に応じてフォーカスサイクルを動的に変更
- **[Validation Plan]**:
  - 既存テストの実行（./gradlew test）
  - キーボード操作のテスト追加
  - 静的解析（./gradlew detekt）
  - 手動検証（macOS/Windows/Linux）

## 7. Evidence and Alignment

- **[Source URLs]**:
  - Kotlin Reference: https://kotlinlang.org/docs/
  - IntelliJ Plugin Reference: https://plugins.jetbrains.com/docs/
  - Swing InputMap/ActionMap: https://docs.oracle.com/javase/tutorial/uiswing/misc/keyin.html
- **[Research Date]**: 2026-07-11
- **[Key Findings]**:
  - CommentInputPanelはJPanelを継承し、textArea、saveButton、cancelButton、deleteButtonを保持
  - JBPopupとして作成され、Escapeキー対応は実装済み
  - 既存テストはコンポーネントの暴露とクリック操作を検証
  - IntelliJ PlatformではSystemInfo.isMacでプラットフォーム判定が可能
- **[Local Constraint Alignment]**:
  - `conductor/code_styleguides/` との整合: 未確認
- **[Potential Regressions]**:
  - 既存のマウスクリック操作への影響
  - フォーカス管理の変更によるUI操作性の低下
- **[Residual Risks / Unknowns]**:
  - プラットフォーム判定の正確な方法
  - FocusTraversalPolicyの実装複雑さ
  - テスト環境でのキーボード操作のシミュレーション方法