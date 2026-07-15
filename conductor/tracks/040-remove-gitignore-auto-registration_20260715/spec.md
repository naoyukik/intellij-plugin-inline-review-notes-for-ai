# Specification: .gitignore への自動登録を削除する

## Overview

`.inline-review-notes/` ディレクトリが `.gitignore` に自動で追加される動作を削除する。
ユーザーの意図しない `.gitignore` 変更を防止し、gitignore の管理をユーザーに委ねる。

## Background

現状、`ReviewCommentStorage.save()` が呼ばれるたびに `.gitignore` に `.inline-review-notes/` が自動登録される。
これにより、ユーザーの意図しない `.gitignore` 変更が発生する。

## Functional Requirements

- [ ] `ReviewCommentStorage` の `ensureGitignoreEntry()` メソッドを削除する
- [ ] `save()` メソッドから `ensureGitignoreEntry()` の呼び出しを削除する
- [ ] `ensureGitignoreEntry()` 関連の定数（`GITIGNORE_FILE`、`GITIGNORE_ENTRY`）を削除する
- [ ] `save()` の KDoc から `.gitignore` に関する記述を削除する

## Non-Functional Requirements

- 既存の保存・読み込み機能に影響を与えない
- テストカバレッジを維持する（削除した機能のテストも削除する）

## Acceptance Criteria

- [ ] `save()` 呼び出し時に `.gitignore` が作成・変更されない
- [ ] 既存のストレージ機能（保存・読み込み）が正常に動作する
- [ ] 全テストがパスする
- [ ] detekt がパスする

## Out of Scope

- ユーザーが手動で `.gitignore` に追加する機能の提供
- `.inline-review-notes/` の git 管理方針の変更
