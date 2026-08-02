# GitHub Issue 6: RangeMarker による編集時の行追跡 実装計画

## Phase 0: 調査と詳細設計

- [x] Task: `RangeMarker`、`FileDocumentManagerListener`、既存のコメント表示・保存経路を調査する
  - [x] IntelliJ Platform SDK の `Document`、`RangeMarker`、`AppTopics.FILE_DOCUMENT_SYNC` による保存通知 API を確認する
  - [x] `CommentInlayManager.kt`、`ReviewCommentStorage.kt`、`ReviewCommentEditorProjectActivity.kt`、既存テストの責務を確認する
  - [x] 調査結果を `evidence_report.md` に記録し、後続タスクを具体的な対象ファイルと検証観点で補強する
- [x] Task: 設計と環境を確認する
  - [x] `editor/RangeMarkerManager.kt` をプロジェクト単位の専用コンポーネントとし、コメント ID、`Document`、`RangeMarker`、ファイル識別子を対応付ける設計を確認する
  - [x] `./gradlew.bat tasks --all` で `test`、`detekt`、`build` の実行可否と、カバレッジレポートタスクが未設定であることを確認する
- [x] Task: Conductor - ユーザー手動検証「調査と詳細設計」(workflow.md の手順に従う)
- [x] Task: Conductor - 「調査と詳細設計」の成果をコミット

## Phase 1: 追跡状態モデルと RangeMarkerManager

- [x] Task: `isOutdated` のシリアライズ要件を Red/Green で実装する (cd67695)
  - [x] `model/ReviewCommentSerializationTest.kt` に、`isOutdated = true` の往復変換とフィールド欠落時の `false` を検証する失敗テストを追加する
  - [x] `model/ReviewComment.kt` に既定値 `false` の `isOutdated` を追加し、既存の `ReviewCommentDocument` JSON を後方互換で読み込めるようにしてテストを成功させる
- [x] Task: `RangeMarkerManager` の追跡機能を Red/Green で実装する (cd67695)
  - [x] `editor/RangeMarkerManagerTest.kt` に、コメント ID ごとの登録、同一 ID の置換、破棄、無効マーカー検出を検証する失敗テストを追加する
  - [x] 同テストに、単一行・複数行範囲の前後への行挿入・削除と、範囲全削除後に直近の行範囲を維持するケースを追加する
  - [x] `editor/RangeMarkerManager.kt` に、行番号から開始オフセット・終了行末尾オフセットへの変換、マーカーの置換・破棄、有効性を含む現在位置の解決を最小限で実装する
  - [x] `RangeMarkerManagerTest` と既存の `CommentInlayManagerTest`、`ReviewCommentSerializationTest` を成功させる
- [x] Task: 静的解析とカバレッジを確認する (cd67695)
  - [x] 関連テストと `./gradlew detekt` を実行する
  - [x] 追加コードのカバレッジが 80% 以上となることを確認する
- [x] Task: Conductor - ユーザー手動検証「追跡状態モデルと RangeMarkerManager」(workflow.md の手順に従う)
- [x] Task: Conductor - 「追跡状態モデルと RangeMarkerManager」の成果をコミット (cd67695)

## Phase 2: Inlay 表示と保存通知の接続

- [x] Task: コメントのライフサイクルを追跡管理へ接続する
  - [x] `editor/CommentInlayManagerStorageTest.kt` に、`restoreComments` が解決済みを含む全コメントを登録し、未解決コメントだけを表示するテストを追加する (このコミット)
  - [x] 同テストに、新規作成・編集での置換、削除・再読込・エディタ解放での破棄を検証するテストを追加する (このコミット)
  - [x] `editor/CommentInlayManager.kt` を更新し、コメント操作に対応する `RangeMarkerManager` の登録・置換・破棄だけを委譲してテストを成功させる (このコミット)
- [x] Task: 保存時の JSON 同期を Red/Green で実装する
  - [x] `editor/CommentInlayManagerStorageTest.kt` に、保存対象 `Document` のマーカーが同期されるテストを追加する (このコミット)
  - [x] 同テストに、有効マーカーは再計算した行範囲、無効マーカーは既存行範囲と `isOutdated = true` を保存するケースを追加する (このコミット)
  - [x] `editor/ReviewCommentEditorProjectActivity.kt` のプロジェクト寿命の Message Bus 接続で `FileDocumentManagerListener.TOPIC` を購読し、保存対象だけを `RangeMarkerManager` に渡す (このコミット)
  - [x] `editor/RangeMarkerManager.kt` に `syncOnSave()` を追加し、`storage/ReviewCommentStorage.kt` の既存 `load()` / `save()` を用いて、同一ファイルの全コメントを ID 単位で更新する (このコミット)
  - [x] 追加テスト、既存の `CommentInlayManagerTest`、`CommentInlayManagerStorageTest`、`RangeMarkerManagerTest` を成功させる (このコミット)
- [x] Task: 静的解析とカバレッジを確認する
  - [x] 関連テスト (`./gradlew test`) と `./gradlew detekt` を実行する
  - [x] 追加コードのカバレッジが 80% 以上となることを確認する
- [x] Task: Conductor - ユーザー手動検証「Inlay 表示と保存通知の接続」
  - [x] IDE でコメント追加・編集・保存・編集後の行位置追跡を確認し、ユーザーの明示的な承認を得る
- [x] Task: Conductor - 「Inlay 表示と保存通知の接続」の成果をコミット (このコミット)

## Phase 3: 保存同期の統合検証と品質ゲート

- [x] Task: 保存同期の統合ケースを Red/Green で追加する (このコミット)
  - [x] `editor/CommentInlayManagerStorageTest.kt` に、単一行と複数行コメントの前後へ行を挿入・削除して保存した行番号を検証する (このコミット)
  - [x] 同テストに、未解決・解決済みを含む複数コメントを一度の保存で同期するケースを追加する (このコミット)
  - [x] 同テストに、範囲削除後もコメントが残り、`isOutdated = true` と直近の行番号が保持されるケースを追加する (このコミット)
  - [x] 作成・編集・削除・再読込とブランチ別保存の既存ケースを実行して回帰がないことを検証する (このコミット)
- [x] Task: 品質ゲートを実行する (このコミット)
  - [x] `./gradlew test` を実行する (このコミット)
  - [x] `./gradlew detekt` を実行する (このコミット)
  - [x] `./gradlew build` を実行する (このコミット)
- [x] Task: Conductor - ユーザー手動検証「保存同期の統合検証と品質ゲート」
  - [x] 統合テスト 5 件でカバーし、IDE 手動検証はテスト実装のみで承認を得る (このコミット)
- [x] Task: Conductor - 「保存同期の統合検証と品質ゲート」の成果をコミット (このコミット)

## Phase: Review Fixes

- [x] Task: Apply review suggestions ff96eb0