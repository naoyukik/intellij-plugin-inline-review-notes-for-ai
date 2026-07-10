# 仕様書 (spec.md)

## 概要 (Overview)
コメント入力パネル（CommentInputPanel）の Delete / Cancel / Save ボタンにキーボード操作を対応させ、キーボード主体のユーザーでも快適に操作できるようにする。

## 機能要件 (Functional Requirements)
1. **Save ボタンのキーボードショートカット**
   - テキストエリアフォーカス時に **Ctrl+Enter（Windows/Linux）/ Cmd+Enter（macOS）** で Save ボタンの Action を実行する。
2. **Escape キーによるキャンセル**
   - 既存の `JBPopup.setCancelKeyEnabled(true)` により実現済みのため、現状維持で対応完了とみなす。
3. **Tab キーによるフォーカス移動**
   - Tab キーでテキストエリア → Save ボタン → Cancel ボタン → Delete ボタン（表示時のみ）の順にフォーカスを移動できる。
   - Shift+Tab で逆順にフォーカス移動できる。
4. **フォーカス時の Enter キー**
   - Save / Cancel / Delete 各ボタンがフォーカスされている状態で Enter キーを押下すると、対応する Action が実行される（JButton のデフォルト動作）。

## 非機能要件 (Non-Functional Requirements)
- 既存のマウスクリック操作に影響を与えない。
- IntelliJ の標準 Keymap 設定を汚染しない（actionId を新規登録しない）。
- macOS / Windows / Linux で一貫した動作を提供する。

## 受け入れ条件 (Acceptance Criteria)
- テキストエリアで Ctrl+Enter（macOS では Cmd+Enter）を押下すると Save が実行されること。
- Escape キーを押下するとポップアップが閉じられること（既存動作の確認）。
- Tab キーでテキストエリアからボタンへフォーカスが移動すること。
- 各ボタンがフォーカス状態で Enter キーを押下すると対応する Action が実行されること。
- 新規コメントモードでは Delete ボタンが非表示であり、Tab フォーカス順からも除外されること。
- 既存のマウスクリック操作が引き続き正常に動作すること。
- CommentInputPanelTest にキーボード操作のテストを追加し、全てのテストがパスすること。

## 対象外 (Out of Scope)
- Delete ボタンへの専用ショートカットキー割り当て
- IntelliJ Keymap（actionId）へのショートカット登録
- Editor ペインからの直接のショートカット起動
