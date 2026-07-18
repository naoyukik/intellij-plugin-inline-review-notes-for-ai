# Track 037-panel-resize 調査報告書

## Phase 0 調査結果

### 1. CommentInputPanel の構造

**ファイル:** `src/main/kotlin/.../editor/ui/CommentInputPanel.kt`

- **現在のサイズ設定:**
  - `defaultRows = 5`（固定高さ）
  - `defaultColumns = 40`（固定幅）
  - JTextArea は `rows` と `columns` プロパティで固定サイズを使用

- **パネル構成要素:**
  - JTextArea（中央）+ JScrollPane
  - ボタンパネル（下部）: Save/Cancel/Delete ボタン
  - キーボードナビゲーション用フォーカス巡回ポリシー

### 2. エディタペインとの統合

**ファイル:** `src/main/kotlin/.../editor/CommentInlayManager.kt`

- **ポップアップ表示:**
  - CommentInputPanel は JBPopup 内で表示
  - `createInputPopup()` でポップアップ作成:
    - `.setResizable(false)` — 手動リサイズ無効
    - `.setMovable(false)` — 手動移動無効
    - `.setRequestFocus(true)` — 表示時にオートフォーカス

- **ポップアップ配置:**
  - `createPopupLocation()` で対象行の下に配置
  - `editor.visualPositionToXY()` で座標計算

### 3. エディタリサイズイベントメカニズム

**現状:**
- エディタリサイズリスナーは未実装
- パネルサイズは静的で、エディタリサイズイベントに応答しない

**必要なメカニズム:**
- IntelliJ Platform は `EditorFactory.getInstance().addEditorListener()` を提供
- コンポーネントレベルのリサイズには `java.awt.event.ComponentListener` を `editor.contentComponent` に追加する必要がある
- 監視すべきキーイベント:
  - `componentResized` — エディタペインサイズ変更時
  - `EditorFactoryListener` — エディタ作成/破棄時

### 4. 動的サイズ計算アルゴリズム設計

**幅計算:**
- 目標: エディタペイン幅の60%
- 式: `panelWidth = (editorWidth * 0.6).toInt()`
- 更新トリガー: エディタコンテンツコンポーネントの `componentResized` イベント

**高さ計算:**
- 目標: テキスト行数に応じて動的調整
- 式: `panelHeight = (lineCount * lineHeight) + (2 * componentPadding)`
- 制約:
  - 最小: 3行分の高さ（短いコメント用）
  - 最大: 15行分の高さ（ポップアップ過大化防止）
- 更新トリガー: JTextArea のドキュメント変更

**実装戦略:**
1. `openInputPanel()` で `editor.contentComponent` に `ComponentListener` を追加
2. CommentInputPanel にサイズ更新メソッドを追加:
   - `updateWidth(newWidth: Int)` — JTextArea の列数を更新
   - `updateHeight(newLineCount: Int)` — JTextArea の行数を更新
3. JTextArea にドキュメントリスナーを登録し、行数変更を追跡
4. ポップアップ破棄時にリスナーをクリーンアップ

### 5. Phase 1 への提言

1. **固定サイズ設定の削除:**
   - `defaultRows` と `defaultColumns` 定数を削除
   - 現在のエディタサイズに基づいて初期サイズを計算

2. **リサイズリスナーの実装:**
   - `editor.contentComponent` に `ComponentAdapter` を追加
   - リサイズイベント時にパネル幅を更新

3. **動的高さの実装:**
   - JTextArea に `DocumentListener` を追加
   - テキスト変更時に最小/最大制約付きで高さを再計算

4. **JBPopup 設定の更新:**
   - `.setResizable(false)` を変更してプログラムによるリサイズを許可
   - または false のまま `popup.setSize()` でポップアップサイズを更新

5. **クリーンアップ:**
   - ポップアップ破棄時にリスナーを削除
   - `disposeInputInlay()` でリスナークリーンアップを処理

## 次のステップ

上記の設計決定に基づいて、Phase 1 の実装に進む。
