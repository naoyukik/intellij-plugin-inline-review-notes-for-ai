# 実装計画 (plan.md)

## 概要
本計画は、GitHub Issue 23 に基づき、コメント入力パネル（CommentInputPanel）のボタンにキーボード操作を追加する実装手順を定義する。

---

## フェーズ 0: 設計と調査

- [ ] Task: 既存実装の調査と詳細設計
    - [ ] CommentInputPanel のコンポーネント構造とフォーカス順序の調査を実施する
    - [ ] Swing の InputMap/ActionMap によるキーバインド設定方法を確認する
    - [ ] macOS / Windows のキーコード差異（Ctrl vs Meta）の対応方針を確定する
- [ ] Task: Conductor - ユーザー手動検証 'Phase 0' (Protocol in workflow.md)
- [ ] Task: Phase 0 コミットし、本フェーズを完了とする

## フェーズ 1: Save ショートカット Ctrl+Enter/Cmd+Enter の実装 (TDD)

### 設計方針
- CommentInputPanel の textArea に InputMap（WHEN_FOCUSED）で Ctrl+Enter の KeyStroke を登録
- 対応する ActionMap で saveButton.doClick() を呼び出す
- プラットフォーム判定（OS.name もしくは KeyStroke.getKeyStroke の自動解決）で Ctrl と Meta を切り替える

- [ ] Task: Save ショートカットのテスト追加 (Red)
    - [ ] textArea で Ctrl+Enter（Windows）を押下すると onSave が呼ばれるテストを CommentInputPanelTest に追加し Red を確認
- [ ] Task: Save ショートカットの実装 (Green)
    - [ ] CommentInputPanel の init 内で textArea の InputMap/ActionMap に Ctrl+Enter/Cmd+Enter のキーバインドを追加
    - [ ] 全てのテストがパスすることを確認
    - [ ] `./gradlew detekt` を実行しコードスタイルが維持されていることを確認
- [ ] Task: Conductor - ユーザー手動検証 'Phase 1' (Protocol in workflow.md)
- [ ] Task: Phase 1 コミットし、本フェーズを完了とする

## フェーズ 2: Tab フォーカス移動の実装 (TDD)

### 設計方針
- CommentInputPanel にカスタム FocusTraversalPolicy を設定
- フォーカス順: textArea → saveButton → cancelButton → deleteButton（表示時のみ）→ textArea の循環
- Shift+Tab はデフォルトの逆方向フォーカス移動で対応
- deleteButton 非表示時はフォーカスサイクルから自動的に除外

- [ ] Task: Tab フォーカス移動のテスト追加 (Red)
    - [ ] Tab キーで textArea → saveButton → cancelButton → textArea とフォーカスが循環するテストを追加し Red を確認
    - [ ] Shift+Tab で逆順にフォーカスが移動するテストを追加し Red を確認
    - [ ] 編集モード（deleteButton 表示時）では deleteButton がフォーカスサイクルに含まれるテストを追加し Red を確認
- [ ] Task: Tab フォーカス移動の実装 (Green)
    - [ ] CommentInputPanel にカスタム FocusTraversalPolicy を実装・設定
    - [ ] deleteButton の表示状態に応じてフォーカスサイクルを動的に変更
    - [ ] 全てのテストがパスすることを確認
    - [ ] `./gradlew detekt` を実行しコードスタイルが維持されていることを確認
- [ ] Task: Conductor - ユーザー手動検証 'Phase 2' (Protocol in workflow.md)
- [ ] Task: Phase 2 コミットし、本フェーズを完了とする
