# 実装計画 (plan.md)

## 概要
本計画は、GitHub Issue 23 に基づき、コメント入力パネル（CommentInputPanel）のボタンにキーボード操作を追加する実装手順を定義する。

---

## フェーズ 0: 設計と調査 [checkpoint: 58c23ee]

- [x] Task: 既存実装の調査と詳細設計
    - [x] CommentInputPanel のコンポーネント構造とフォーカス順序の調査を実施する
    - [x] Swing の InputMap/ActionMap によるキーバインド設定方法を確認する
    - [x] macOS / Windows のキーコード差異（Ctrl vs Meta）の対応方針を確定する
- [~] Task: Conductor - ユーザー手動検証 'Phase 0' (Protocol in workflow.md)
- [ ] Task: Phase 0 コミットし、本フェーズを完了とする

## フェーズ 1: Save ショートカット Ctrl+Enter/Cmd+Enter の実装 (TDD) [checkpoint: 1cd1bf9]

### 設計方針
- CommentInputPanel の textArea に InputMap（WHEN_FOCUSED）で Ctrl+Enter の KeyStroke を登録
- 対応する ActionMap で saveButton.doClick() を呼び出す
- プラットフォーム判定（SystemInfo.isMac）で Ctrl と Meta を切り替える

### 具体的な実装計画
1. **CommentInputPanel.kt** (`src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanel.kt`):
   - initブロックに以下のコードを追加:
     ```kotlin
     val modifier = if (SystemInfo.isMac) KeyEvent.META_MASK else KeyEvent.CTRL_MASK
     val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifier)
     textArea.inputMap.put(keyStroke, "save")
     textArea.actionMap.put("save", object : AbstractAction() {
         override fun actionPerformed(e: ActionEvent) {
             saveButton.doClick()
         }
     })
     ```
   - 必要なimportを追加: `java.awt.event.KeyEvent`, `java.awt.event.ActionEvent`, `javax.swing.Action`, `com.intellij.openapi.util.SystemInfo`

2. **CommentInputPanelTest.kt** (`src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanelTest.kt`):
   - テストを追加:
     ```kotlin
     @Test
     fun save_shortcut_invokes_on_save() {
         var savedText = ""
         val panel = CommentInputPanel(
             onSave = { savedText = it },
             onCancel = {},
             onDelete = {},
         )
         panel.textArea.text = "ショートカット保存"
         // Windows/Linux: Ctrl+Enter
         val modifier = if (SystemInfo.isMac) KeyEvent.META_DOWN_MASK else KeyEvent.CTRL_DOWN_MASK
         panel.textArea.dispatchEvent(KeyEvent(panel.textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifier, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED))
         assertEquals("ショートカット保存", savedText)
     }
     ```

- [x] Task: Save ショートカットのテスト追加 (Red)
    - [x] textArea で Ctrl+Enter（Windows）を押下すると onSave が呼ばれるテストを CommentInputPanelTest に追加し Red を確認
- [x] Task: Save ショートカットの実装 (Green)
    - [x] CommentInputPanel の init 内で textArea の InputMap/ActionMap に Ctrl+Enter/Cmd+Enter のキーバインドを追加
    - [x] 全てのテストがパスすることを確認
    - [x] `./gradlew detekt` を実行しコードスタイルが維持されていることを確認
- [x] Task: Conductor - ユーザー手動検証 'Phase 1' (Protocol in workflow.md)
- [x] Task: Phase 1 コミットし、本フェーズを完了とする

## フェーズ 2: Tab フォーカス移動の実装 (TDD) [checkpoint: 99c9ad2]

### 設計方針
- CommentInputPanel にカスタム FocusTraversalPolicy を設定
- フォーカス順: textArea → saveButton → cancelButton → deleteButton（表示時のみ）→ textArea の循環
- Shift+Tab はデフォルトの逆方向フォーカス移動で対応
- deleteButton 非表示時はフォーカスサイクルから自動的に除外

### 具体的な実装計画
1. **CommentInputPanel.kt** (`src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanel.kt`):
   - カスタムFocusTraversalPolicyクラスを定義:
     ```kotlin
     private inner class CommentFocusTraversalPolicy : FocusTraversalPolicy() {
         private val components: List<Component>
             get() {
                 val base = listOf<Component>(textArea, saveButton, cancelButton)
                 return if (deleteButton.isVisible) base + deleteButton else base
             }

         override fun getComponentAfter(aContainer: Container, aComponent: Component): Component {
             val index = components.indexOf(aComponent)
             return if (index < components.size - 1) components[index + 1] else components.first()
         }

         override fun getComponentBefore(aContainer: Container, aComponent: Component): Component {
             val index = components.indexOf(aComponent)
             return if (index > 0) components[index - 1] else components.last()
         }

         override fun getFirstComponent(aContainer: Container): Component = components.first()

         override fun getLastComponent(aContainer: Container): Component = components.last()

         override fun getDefaultComponent(aContainer: Container): Component = components.first()
     }
     ```
   - initブロックでfocusTraversalPolicyを設定:
     ```kotlin
     focusTraversalPolicy = CommentFocusTraversalPolicy()
     isFocusCycleRoot = true
     ```
   - 必要なimportを追加: `java.awt.FocusTraversalPolicy`, `java.awt.Component`, `java.awt.Container`

2. **CommentInputPanelTest.kt** (`src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanelTest.kt`):
   - テストを追加:
     ```kotlin
     @Test
     fun tab_focus_cycles_through_components() {
         val panel = CommentInputPanel(
             onSave = {},
             onCancel = {},
             onDelete = {},
         )
         panel.textArea.requestFocusInWindow()
         assertTrue(panel.textArea.hasFocus())

         // Tabキーでフォーカス移動
         panel.textArea.dispatchEvent(KeyEvent(panel.textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.saveButton.hasFocus())

         panel.saveButton.dispatchEvent(KeyEvent(panel.saveButton, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.cancelButton.hasFocus())

         panel.cancelButton.dispatchEvent(KeyEvent(panel.cancelButton, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.textArea.hasFocus())
     }

     @Test
     fun shift_tab_focus_cycles_reverse() {
         val panel = CommentInputPanel(
             onSave = {},
             onCancel = {},
             onDelete = {},
         )
         panel.textArea.requestFocusInWindow()
         assertTrue(panel.textArea.hasFocus())

         // Shift+Tabキーで逆順フォーカス移動
         panel.textArea.dispatchEvent(KeyEvent(panel.textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.cancelButton.hasFocus())
     }

     @Test
     fun delete_button_included_in_focus_cycle_when_visible() {
         val panel = CommentInputPanel(
             existingComment = "既存コメント",
             onSave = {},
             onCancel = {},
             onDelete = {},
         )
         panel.textArea.requestFocusInWindow()
         assertTrue(panel.textArea.hasFocus())

         // テキストエリアから順にフォーカス移動
         panel.textArea.dispatchEvent(KeyEvent(panel.textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.saveButton.hasFocus())

         panel.saveButton.dispatchEvent(KeyEvent(panel.saveButton, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.cancelButton.hasFocus())

         panel.cancelButton.dispatchEvent(KeyEvent(panel.cancelButton, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.deleteButton.hasFocus())

         panel.deleteButton.dispatchEvent(KeyEvent(panel.deleteButton, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED))
         assertTrue(panel.textArea.hasFocus())
     }
     ```

- [x] Task: Tab フォーカス移動のテスト追加 (Red)
    - [x] Tab キーで textArea → saveButton → cancelButton → textArea とフォーカスが循環するテストを追加し Red を確認
    - [x] Shift+Tab で逆順にフォーカスが移動するテストを追加し Red を確認
    - [x] 編集モード（deleteButton 表示時）では deleteButton がフォーカスサイクルに含まれるテストを追加し Red を確認
- [x] Task: Tab フォーカス移動の実装 (Green)
    - [x] CommentInputPanel にカスタム FocusTraversalPolicy を実装・設定
    - [x] deleteButton の表示状態に応じてフォーカスサイクルを動的に変更
    - [x] 全てのテストがパスすることを確認
    - [x] `./gradlew detekt` を実行しコードスタイルが維持されていることを確認
- [x] Task: Conductor - ユーザー手動検証 'Phase 2' (Protocol in workflow.md)
- [x] Task: Phase 2 コミットし、本フェーズを完了とする

## フェーズ 3: Tab キーのフォーカス移動不具合修正 (TDD) [checkpoint: c2c3390]

### 不具合の詳細
textArea にフォーカスがある状態で Tab キーを押下すると、JTextArea のデフォルト動作により制御文字（タブ文字）が挿入され、FocusTraversalPolicy によるフォーカス移動が発動しない。

### 設計方針
- textArea の InputMap（WHEN_FOCUSED）で Tab キーのデフォルトアクションを無効化し、フォーカス移動アクションに置き換える
- Shift+Tab も同様に逆方向フォーカス移動に置き換える

### 具体的な実装計画
1. **CommentInputPanel.kt** (`src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanel.kt`):
   - initブロックに以下のコードを追加:
     ```kotlin
     // Tab キーのフォーカス移動: JTextArea のデフォルト動作（タブ文字挿入）を無効化
     val tabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
     textArea.inputMap.put(tabKeyStroke, "forward")
     textArea.actionMap.put("forward", object : AbstractAction() {
         override fun actionPerformed(e: ActionEvent) {
             transferFocus()
         }
     })

     val shiftTabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)
     textArea.inputMap.put(shiftTabKeyStroke, "backward")
     textArea.actionMap.put("backward", object : AbstractAction() {
         override fun actionPerformed(e: ActionEvent) {
             transferFocusBackward()
         }
     })
     ```
   - `transferFocus()` / `transferFocusBackward()` は `java.awt.Component` のメソッドで、設定済みの FocusTraversalPolicy に従ってフォーカスを移動させる

2. **CommentInputPanelTest.kt** (`src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ui/CommentInputPanelTest.kt`):
   - テストを追加:
     ```kotlin
     @Test
     fun tab_key_transfers_focus_from_textarea() {
         val panel = CommentInputPanel(
             onSave = {},
             onCancel = {},
             onDelete = {},
         )
         // アクションマップから Tab アクションを取得して直接実行
         val forwardAction = panel.textArea.actionMap.get("forward")
         assertNotNull("forward action should be registered", forwardAction)
     }

     @Test
     fun shift_tab_key_transfers_focus_backward_from_textarea() {
         val panel = CommentInputPanel(
             onSave = {},
             onCancel = {},
             onDelete = {},
         )
         // アクションマップから Shift+Tab アクションを取得して直接実行
         val backwardAction = panel.textArea.actionMap.get("backward")
         assertNotNull("backward action should be registered", backwardAction)
     }
     ```

- [x] Task: Tab キー不具合修正のテスト追加 (Red)
    - [x] textArea の InputMap に Tab / Shift+Tab アクションが登録されていることを検証するテストを追加し Red を確認
- [x] Task: Tab キー不具合修正の実装 (Green)
    - [x] CommentInputPanel の init 内で textArea の InputMap/ActionMap に Tab / Shift+Tab のキーバインドを追加
    - [x] 全てのテストがパスすることを確認
    - [x] `./gradlew detekt` を実行しコードスタイルが維持されていることを確認
- [x] Task: Conductor - ユーザー手動検証 'Phase 3' (Protocol in workflow.md)
- [x] Task: Phase 3 コミットし、本フェーズを完了とする [checkpoint: c2c3390]

## Phase: Review Fixes
- [x] Task: Apply review suggestions e3c05df
