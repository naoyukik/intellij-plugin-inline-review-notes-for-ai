# Issue 44: 編集時のコメント入力パネル幅を新規作成時と統一する

## 方針転換: リサイズ追従を再採用する

- 固定幅方針の実装コミット `47580c0` は、`ComponentListener` を削除して初期表示時にのみ `updatePanelSize` を呼び出した。しかしユーザーの手動検証で、編集時のパネル幅がエディタペイン幅の 60% にならないことが確認された。
- コミット `ad01118` には、`editor.contentComponent` の `ComponentAdapter.componentResized` から `CommentInputPanel.updatePanelSize(editor.contentComponent.width)` を呼び出す実装が存在する。
- ユーザーの手動検証では、このリサイズ追従方式で新規作成・編集の双方が期待する 60% 幅で表示された。したがって、固定幅方針を取消し、初期表示時とリサイズ時に同じ幅計算を適用する方式を採用する。
- リスナーはパネル再オープン時と `releaseEditor` 時に解除し、重複更新や解放済みエディタへの参照を残さない。

## 1. Discovery Summary (Phase 1)

- **[Problem Statement]**: 編集操作で開いたコメント入力パネルを、新規作成時と同じエディタペイン幅の 60% にそろえ、表示後のエディタリサイズにも追従させる。
- **[Scope]**: コメント入力パネルの初期幅設定と表示後のリサイズ監視に限定する。
- **[Non-Goals]**: コメントの保存・編集・削除処理、データモデル、JSON 保存、幅・高さの計算式そのもの、その他の UI レイアウトは変更しない。
- **[Constraints]**:
  - Kotlin / IntelliJ Platform の既存構成とテスト方式を維持する。
  - 新規作成と編集は同じ `CommentInlayManager.openInputPanel` 経路を使うため、既存の保存・キャンセル・削除状態遷移に影響を与えない。
  - 幅は既存の 60%・最小 300px・最大 800px の計算を再利用する。
- **[Success Criteria]**:
  - 新規作成・編集のいずれも、表示開始時のエディタペイン幅を基準に既存の 60% 計算結果が適用される。
  - 表示後のエディタペインリサイズでパネル幅が更新されない。
  - テキスト行数に応じた高さ、入力、保存、編集、削除の既存動作が維持される。

## 2. Codebase Findings (Phase 2)

- **[Similar Implementations]**:
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:33-77`: 新規作成と編集が共通する入力パネル生成経路。`existingCommentId` を状態へ保存し、`CommentInputPanel` を生成し、エディタ幅から初期サイズを設定してポップアップを表示する。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt:123-143`: 保存済みコメントのレンダラーから編集経路へ入り、本文とコメント ID を `openInputPanel` へ渡す。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/action/AddReviewCommentAction.kt:20-31`: 新規作成経路は `existingCommentId = null` で入力パネルを開く。
  - `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentGutterIconRenderer.kt:17-25`: ガター操作からも新規作成経路へ入る。
- **[Architecture and Dependency Notes]**:
  - ポップアップ生成は `CommentInlayManager`、サイズ計算と Swing テキスト領域の列数反映は `CommentInputPanel` に分離されている。
  - `openInputPanel` は現在、初期設定に加えて `ComponentAdapter.componentResized` から `updatePanelSize` を再度呼び出している。このリスナーは新規・編集を区別しないため、仕様の「表示後は幅を変更しない」と一致しない。
  - `createInputPopup` は `createComponentPopupBuilder` でパネルを表示し、`setResizable(false)` を設定しているため、ポップアップの可変サイズ機能を追加する必要はない。
- **[Reusable Components]**:
  - `CommentInputPanel.calculateInitialWidth(Int)` と `updatePanelSize(Int)` の既存計算をそのまま再利用する。
  - `CommentInlayManager.activeInputPanel(Editor)` と既存の `CommentInlayManagerTest` の状態検証を利用して、新規・編集の実経路を検証する。
  - `CommentInputPanelTest` の幅計算・高さ計算テストを既存仕様の回帰ガードとして維持する。
- **[Estimated Impact Area]**:
  - 実装: `CommentInlayManager.kt` の入力パネル表示時リサイズ監視。
  - テスト: `CommentInlayManagerTest.kt` の新規・編集初期幅および表示後固定の統合テスト、必要に応じて `CommentInputPanelTest.kt` の既存幅・高さテスト。
  - `plugin.xml`、ストレージ、モデル、アクション登録の変更は不要。

## 3. Clarifying Questions (Phase 3)

- **[Open Questions]**:
  1. 表示後のエディタペインリサイズで幅を固定する対象は新規作成だけか、編集も含むか。
  2. 幅の適用はポップアップ API の明示的なサイズ設定へ変更するか、既存の `JTextArea.columns` による内容幅設定を使うか。
- **[User Answers / Delegations]**:
  1. 仕様の機能要件と受け入れ基準で、新規作成・編集の両方を対象にすると明確化されている。表示後の幅固定を採用する。
  2. 変更範囲を表示サイズに限定し、既存の幅計算式を再設計しない要件から、既存の `CommentInputPanel.updatePanelSize` を再利用する。
- **[Unresolved Items]**: 実装方針の最終承認は Phase 0 のユーザー手動検証で確認する。

## 4. 将来の修正で期待される挙動 (Expected Behavior)

- 新規作成で入力パネルを開くと、開いた時点のエディタペイン幅を `CommentInputPanel.calculateInitialWidth` に渡し、既存の 60%・300〜800px の範囲で幅を設定する。
- 保存済みコメントを編集で開く場合も、本文や `existingCommentId` の有無にかかわらず、同じ初期幅計算を一度だけ適用する。
- ポップアップ表示後にエディタペイン幅が変化しても、表示中の入力パネルの幅は更新しない。
- パネルのテキスト行数に基づく高さ調整、入力内容、保存・キャンセル・削除、編集後のレンダラー復元は変更しない。
- 最小幅・最大幅の境界では既存の `coerceIn` の結果を維持する。

## 5. Architecture Options (Phase 4)

### Option A: Minimal Changes

- **[Change Targets]**: `CommentInlayManager.openInputPanel` の `ComponentAdapter` 登録と解除を削除し、既存の初期 `updatePanelSize` 呼び出しだけを残す。`CommentInlayManagerTest` に新規・編集の初期幅とリサイズ後固定のテストを追加する。
- **[Pros]**: 変更ファイルと実行時挙動の差分が最小で、既存の幅・高さ計算を完全に再利用できる。
- **[Cons/Risks]**: `EditorState.componentListener` と解除処理が不要になり、状態クラスの責務を整理する必要がある。ポップアップ表示前の幅取得が 0 の環境では既存の最小幅へフォールバックする。
- **[Validation Plan]**: 新規・編集の `activeInputPanel` の列数を初期エディタ幅から検証し、表示後にエディタ幅を変えて列数が変わらないことを統合テストで検証する。既存の高さ・状態遷移テストと `./gradlew test`、`detekt` を実行する。

### Option B: Clean Architecture

- **[Change Targets]**: パネル幅を表す専用のサイズ方針または値オブジェクトを新設し、ポップアップ生成へ明示的な `Dimension` を渡す。表示とリサイズの責務を別コンポーネントへ分離する。
- **[Pros]**: ポップアップの実サイズを明示的に扱え、将来のサイズ方針変更を独立して拡張しやすい。
- **[Cons/Risks]**: 既存の計算式と Swing レイアウトに対して新しい抽象化を導入するため、今回の限定された不具合に対して変更範囲と回帰リスクが大きい。仕様の「計算式を再設計しない」に反する可能性がある。
- **[Validation Plan]**: サイズ方針の単体テスト、ポップアップ実サイズの統合テスト、新規・編集・リサイズ・高さの回帰テスト、`./gradlew test`、`detekt` を実行する。

### Option C: Pragmatic Balance

- **[Change Targets]**: サイズ計算は `CommentInputPanel` に残し、`CommentInlayManager` は表示開始時に一度だけ `updatePanelSize` を呼ぶ。リサイズリスナーの管理を削除し、`CommentInlayManagerTest` に共通ヘルパーを使った新規・編集・固定テストを追加する。
- **[Pros]**: Option A と同じ小さな実装差分を保ちつつ、新規・編集の両経路と固定要件を統合テストで明確に表現できる。
- **[Cons/Risks]**: パネルの実サイズが `JTextArea.columns` と Swing の preferred size に依存する既存設計は残る。ヘッドレス統合テストではコンポーネント幅の初期化タイミングに注意が必要である。
- **[Validation Plan]**: パネル表示前のエディタ幅を固定して初期列数を検証し、表示後の幅変更イベント後も列数が保持されることを確認する。既存の UI 単体テスト、状態遷移テスト、`./gradlew test`、`detekt` を実行する。

- **[Recommended Option]**: Option C: Pragmatic Balance
- **[Reason]**: 既存の `CommentInputPanel` の責務と計算式を維持しながら、仕様に反するリサイズ監視だけを除去できる。新規・編集の共通経路を保ち、データモデルや保存層へ波及しないため、今回の非機能要件と最小変更方針に最も整合する。

## 6. 推奨される実装方針 (Implementation Strategy)

- **[Architecture Alignment]**: UI のサイズ計算は `editor/ui/CommentInputPanel.kt`、ポップアップのライフサイクルは `editor/CommentInlayManager.kt` に残す。表示後のエディタリサイズ監視は持たず、状態に不要となるリスナー参照・解除処理を整理する。
- **[Logic Changes]**:
  - `openInputPanel` の初期 `updatePanelSize(editor.contentComponent.width)` は維持する。
  - 表示後の `componentResized` による `updatePanelSize` 呼び出しを削除する。
  - 新規・編集の両経路で初期列数が同じ計算結果になるテストを追加し、編集時に本文と ID を渡しても幅計算が変わらないことを検証する。
  - 表示後のエディタ幅変更で列数が変わらないこと、既存の高さ計算・状態遷移が維持されることを検証する。
- **[Validation Plan]**: まず `CommentInlayManagerTest` の新規・編集初期幅およびリサイズ固定テストを追加して修正前の失敗を確認する。その後最小変更を実装し、対象テスト、`./gradlew test`、`./gradlew detekt`、必要に応じて `./gradlew build` を実行する。手動検証では新規作成・編集・表示後リサイズ・複数行高さを確認する。

## 7. Evidence and Alignment

- **[Source URLs]**:
  - https://plugins.jetbrains.com/docs/intellij/popups.html
  - https://github.com/JetBrains/intellij-community/blob/idea/261.26222.65/platform/platform-api/src/com/intellij/openapi/ui/popup/ComponentPopupBuilder.java
- **[Research Date]**: 2026-07-19
- **[Key Findings]**:
  - IntelliJ Platform の公式ドキュメントは `JBPopupFactory.createComponentPopupBuilder` を任意の Swing コンポーネント向けの標準経路として説明している。
  - 本実装はすでに `setResizable(false)` を指定しており、ポップアップのリサイズ機能を追加する必要はない。
  - 現在の `ComponentAdapter` は表示後にも `updatePanelSize` を呼び出すため、仕様の幅固定を満たすには初期設定と分離する必要がある。
- **[Local Constraint Alignment]**:
  - UI サイズ計算とポップアップ管理の責務分離を維持し、コメント保存層・モデル・プラグイン設定を変更しない。
  - `build.gradle.kts` の既存 JUnit 4 / IntelliJ Platform / Detekt 構成に従う。利用可能な検証タスクは `test`、`detekt`、`build` である。
- **[Potential Regressions]**:
  - リサイズリスナー削除後に、表示前の初期エディタ幅が適用されないケースがないかを新規・編集の両テストで確認する。
  - `EditorState` のリスナー解除を削除する場合、`releaseEditor` とパネル再オープン時に残存リスナーがないことを確認する。
  - `JTextArea.columns` の変更が高さ計算や既存のフォーカス・保存処理に影響しないことを回帰テストで確認する。
- **[Residual Risks / Unknowns]**:
  - ヘッドレス IntelliJ テストにおける `editor.contentComponent` の幅初期化と Swing イベント処理のタイミングは、テスト実行時に確認が必要である。
  - ポップアップの実ピクセル幅は Swing の preferred size に依存するため、統合テストでは列数と表示状態を中心に検証し、最終的な視覚確認は手動検証で行う。