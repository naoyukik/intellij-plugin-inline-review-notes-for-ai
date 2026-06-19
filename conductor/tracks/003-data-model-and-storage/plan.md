# Plan: データモデルとJSON保存機能の実装

## Phase 0: 調査と環境確認
- [~] Task: `autonomous-researcher` による詳細調査と `evidence_report.md` の作成。
- [ ] Task: 調査結果に基づき `plan.md` の後続タスクを、対象ファイル・期待挙動・変更内容が分かる粒度まで具体化する。
- [ ] Task: `intellij-plugin-inline-review-notes-for-ai` での Gradle ビルドとテスト実行ができることを確認。 `.\gradlew test` を実行。
- [ ] Task: Conductor - ユーザー手動検証 'Phase 0' (調査結果と具体化した計画の承認)

## Phase 1: データモデル `ReviewComment` の実装 (TDD)
- [ ] Task: `ReviewComment` のシリアライズ・デシリアライズをテストする unit test を作成し、コンパイルエラー（Red）を確認する。
- [ ] Task: `ReviewComment` および関連データ構造のデータクラスを `kotlinx.serialization` を用いて実装する。
- [ ] Task: テストを実行して成功（Green）を確認する。

## Phase 2: ブランチ名サニタイズとファイル名解決ロジックの実装 (TDD)
- [ ] Task: 特殊文字を含むブランチ名のサニタイズルールをテストする unit test を作成し、失敗（Red）を確認する。
- [ ] Task: サニタイズロジックおよびファイル名決定ロジックを `ReviewCommentStorage` に実装する。
- [ ] Task: テストを実行して成功（Green）を確認する。

## Phase 3: `ReviewCommentStorage` による保存と読み込み機能の実装 (TDD)
- [ ] Task: JSONファイルの読み書きと `.gitignore` への自動追記をテストする統合テストを作成し、失敗（Red）を確認する。
- [ ] Task: `ReviewCommentStorage` にファイルの読み書き機能、`.inline-review-notes/` の自動生成、`.gitignore` への追記処理を実装する。
- [ ] Task: テストを実行して成功（Green）を確認する。
- [ ] Task: IntelliJ Platform の Git API または外部 `git` コマンドを使用して、カレントブランチ名を取得するロジックを実装し、フォールバックも含めた結合テストをパスさせる。

## Phase 4: 完了フェーズ
- [ ] Task: Conductor - `./gradlew test` と、利用可能なら `./gradlew detekt` を実行してテストと静的解析の成功を確認。
- [ ] Task: Conductor - `./gradlew build` を実行してビルドと全テストの成功を確認。
- [ ] Task: Conductor - 成果の検証とレビュー
- [ ] Task: Conductor - 成果をコミット (`feat(storage): Implement ReviewComment data model and storage`)
