# Implementation Plan: .gitignore への自動登録を削除する

## Phase 0: Research and Detailed Design

- [x] Task: Review existing code and create evidence report
    - [x] `ReviewCommentStorage.kt` の `ensureGitignoreEntry()` メソッドと `save()` の関係を確認
    - [x] `ReviewCommentStorageTest.kt` の `save_writes_json_and_updates_gitignore_once` テストの確認
    - [x] 他に `.gitignore` 操作を行っている箇所がないか確認
    - [x] `evidence_report.md` を作成
- [ ] Task: Conductor - User Manual Verification 'Phase 0' (Protocol in workflow.md)

## Phase 1: Implementation

- [ ] Task: Remove `ensureGitignoreEntry()` method and related constants from `ReviewCommentStorage.kt`
    - [ ] `ensureGitignoreEntry()` メソッド（107-129行目）を削除
    - [ ] `save()` メソッド（100行目）からの `ensureGitignoreEntry()` 呼び出しを削除
    - [ ] companion object の `GITIGNORE_FILE`（153行目）と `GITIGNORE_ENTRY`（154行目）定数を削除
    - [ ] `save()` の KDoc（93行目）から `.gitignore` に関する記述を削除
- [ ] Task: Update tests in `ReviewCommentStorageTest.kt`
    - [ ] `save_writes_json_and_updates_gitignore_once` テストを削除
    - [ ] `.gitignore` 操作を検証するテストコードを削除
    - [ ] 残りのテストが引き続きパスすることを確認
- [ ] Task: Run tests and static analysis
    - [ ] `./gradlew test` を実行し、全テストがパスすることを確認
    - [ ] `./gradlew detekt` を実行し、静的解析がパスすることを確認
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)
