# Implementation Plan: 005-inlay-comment-ui

## Phase 0: Discovery & Detailed Design
- [ ] Task: コードベースの調査と詳細設計
    - [ ] `autonomous-researcher` スキルを活用して、既存のエディタ表示、Inlay管理、ストレージ関連 of コードを探索・調査する
    - [ ] `evidence_report.md` を作成し、既存のコンポーネントや IntelliJ Inlay API の具体的な利用方法を特定する
- [ ] Task: 計画の具体化
    - [ ] 調査結果に基づいて、Phase 1 以降のタスクを対象ファイル名や具体的な行数、クラス定義などを反映した形に書き換える
- [ ] Task: Conductor - User Manual Verification 'Phase 0: Discovery & Detailed Design' (Protocol in workflow.md)
- [ ] Task: Phase 0 チェックポイントの作成とコミット
    - [ ] 変更をステージし、チェックポイントコミットを作成する
    - [ ] コミットハッシュを plan.md の見出しに `[checkpoint: <sha>]` として追記する
    - [ ] plan.md をコミットし、本フェーズを完了とする

## Phase 1: CommentInputPanel & Inlay Infrastructure
- [ ] Task: CommentInputPanel UI とコールバックの実装
    - [ ] `CommentInputPanel` のテスト（テストクラス作成、UI要素の存在確認、保存・キャンセル・削除ボタンによるコールバック呼び出しの検証）を記述する（TDD Red Phase）
    - [ ] `CommentInputPanel` (JPanel) を実装し、テストをパスさせる（TDD Green Phase）
    - [ ] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [ ] Task: CommentBlockRenderer (Inlay) の実装
    - [ ] `CommentBlockRenderer` のテスト（Inlay レンダラーの描画計算、クリック時の再編集イベント発火の検証）を記述する（TDD Red Phase）
    - [ ] `CommentBlockRenderer` を実装し、テストをパスさせる（TDD Green Phase）
    - [ ] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [ ] Task: Conductor - User Manual Verification 'Phase 1: CommentInputPanel & Inlay Infrastructure' (Protocol in workflow.md)
- [ ] Task: Phase 1 チェックポイントの作成とコミット
    - [ ] 変更をステージし、チェックポイントコミットを作成する
    - [ ] コミットハッシュを plan.md の見出しに `[checkpoint: <sha>]` として追記する
    - [ ] plan.md をコミットし、本フェーズを完了とする

## Phase 2: Interaction Logic
- [ ] Task: 入力パネルと Inlay 表示の連携ロジックの実装
    - [ ] コメント入力フォームから表示 Inlay への状態遷移ロジック（Save / Cancel / Delete時の遷移）のテストを記述する（TDD Red Phase）
    - [ ] エディタ上で Inlay を挿入・置換・破棄する制御ロジックを実装し、テストをパスさせる（TDD Green Phase）
    - [ ] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Interaction Logic' (Protocol in workflow.md)
- [ ] Task: Phase 2 チェックポイントの作成とコミット
    - [ ] 変更をステージし、チェックポイントコミットを作成する
    - [ ] コミットハッシュを plan.md の見出しに `[checkpoint: <sha>]` として追記する
    - [ ] plan.md をコミットし、本フェーズを完了とする

## Phase 3: Storage Integration
- [ ] Task: ローカル JSON ストレージ連携
    - [ ] コメントの保存・削除時にストレージサービスをトリガーする処理のテストを記述する（TDD Red Phase）
    - [ ] コメント永続化（保存先: `.inline-review-notes/{branch}.json`）との連携を実装し、テストをパスさせる（TDD Green Phase）
    - [ ] `./gradlew detekt` を実行し、コードスタイルと静的解析を確認する
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Storage Integration' (Protocol in workflow.md)
- [ ] Task: Phase 3 チェックポイントの作成とコミット
    - [ ] 変更をステージし、チェックポイントコミットを作成する
    - [ ] コミットハッシュを plan.md の見出しに `[checkpoint: <sha>]` として追記する
    - [ ] plan.md をコミットし、本フェーズを完了とする
