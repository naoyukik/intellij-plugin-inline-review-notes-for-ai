# GitHub Issue 6: RangeMarker による編集時の行追跡 実装計画

## Phase 0: 調査と詳細設計

- [ ] Task: `RangeMarker`、`FileDocumentManagerListener`、既存のコメント表示・保存経路を調査する
  - [ ] IntelliJ Platform SDK の `Document`、`RangeMarker`、保存通知 API を確認する
  - [ ] `CommentInlayManager`、`ReviewCommentStorage`、`ReviewCommentEditorProjectActivity`、既存テストの責務を確認する
  - [ ] 調査結果を `evidence_report.md` に記録し、以後のタスクを具体的な対象ファイルと検証観点で補強する
- [ ] Task: 設計と環境を確認する
  - [ ] `RangeMarkerManager` をプロジェクト単位で管理し、コメント ID、`Document`、`RangeMarker`、ファイル識別子を対応付ける設計を確認する
  - [ ] 既存の Gradle タスク、テスト基盤、`detekt` の実行可否を確認する
- [ ] Task: Conductor - ユーザー手動検証「調査と詳細設計」(workflow.md の手順に従う)
- [ ] Task: Conductor - 「調査と詳細設計」の成果をコミット

## Phase 1: 追跡状態モデルと RangeMarkerManager

- [ ] Task: `isOutdated` のシリアライズ要件を Red/Green で実装する
  - [ ] `ReviewCommentSerializationTest` に、`isOutdated` を含む往復変換とフィールド欠落時の既定値を検証する失敗テストを追加する
  - [ ] `ReviewComment` に既定値 `false` の `isOutdated` を追加してテストを成功させる
- [ ] Task: `RangeMarkerManager` の追跡機能を Red/Green で実装する
  - [ ] コメント ID ごとの登録、置換、破棄、無効マーカー検出を検証する失敗テストを追加する
  - [ ] 行挿入・削除、複数行範囲、範囲全削除後の位置解決を検証する失敗テストを追加する
  - [ ] `Document` 上の行範囲を `RangeMarker` に変換・再計算する `RangeMarkerManager` を最小限で実装する
  - [ ] 失敗テストと既存の関連テストを成功させる
- [ ] Task: 静的解析とカバレッジを確認する
  - [ ] 関連テストと `./gradlew detekt` を実行する
  - [ ] 追加コードのカバレッジが 80% 以上となることを確認する
- [ ] Task: Conductor - ユーザー手動検証「追跡状態モデルと RangeMarkerManager」(workflow.md の手順に従う)
- [ ] Task: Conductor - 「追跡状態モデルと RangeMarkerManager」の成果をコミット

## Phase 2: Inlay 表示と保存通知の接続

- [ ] Task: コメントのライフサイクルを追跡管理へ接続する
  - [ ] 読込時に全コメントを登録し、未解決コメントだけを従来どおり Inlay 表示する失敗テストを追加する
  - [ ] 新規作成、編集、削除、再読込、エディタ解放で登録を更新または破棄する失敗テストを追加する
  - [ ] `CommentInlayManager` を更新してテストを成功させる
- [ ] Task: 保存時の JSON 同期を Red/Green で実装する
  - [ ] 保存通知が対象ドキュメントのマーカーを同期する失敗テストを追加する
  - [ ] 有効マーカーは再計算した行範囲、無効マーカーは既存行範囲と `isOutdated = true` を更新する失敗テストを追加する
  - [ ] `ReviewCommentEditorProjectActivity` のプロジェクト寿命に紐付けて保存通知を購読する
  - [ ] `ReviewCommentStorage` を通じて同一ファイルのコメントを ID 単位で保存する
  - [ ] 失敗テストと既存の関連テストを成功させる
- [ ] Task: 静的解析とカバレッジを確認する
  - [ ] 関連テストと `./gradlew detekt` を実行する
  - [ ] 追加コードのカバレッジが 80% 以上となることを確認する
- [ ] Task: Conductor - ユーザー手動検証「Inlay 表示と保存通知の接続」(workflow.md の手順に従う)
- [ ] Task: Conductor - 「Inlay 表示と保存通知の接続」の成果をコミット

## Phase 3: 保存同期の統合検証と品質ゲート

- [ ] Task: 保存同期の統合ケースを Red/Green で追加する
  - [ ] 保存後の単一行・複数行コメントの行番号同期を検証する
  - [ ] 未解決・解決済みを含む複数コメントの一括同期を検証する
  - [ ] 範囲削除後もコメントが残り、`isOutdated = true` と直近の行番号が保持されることを検証する
  - [ ] 既存のコメント操作とブランチ別保存に回帰がないことを検証する
- [ ] Task: 品質ゲートを実行する
  - [ ] `./gradlew test` を実行する
  - [ ] `./gradlew detekt` を実行する
  - [ ] `./gradlew build` を実行する
- [ ] Task: Conductor - ユーザー手動検証「保存同期の統合検証と品質ゲート」(workflow.md の手順に従う)
  - [ ] IDE で行の挿入・削除、保存、再読込を確認し、ユーザーの明示的な承認を得る
- [ ] Task: Conductor - 「保存同期の統合検証と品質ゲート」の成果をコミット