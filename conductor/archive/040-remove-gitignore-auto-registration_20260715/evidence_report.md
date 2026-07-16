# Evidence Report: .gitignore への自動登録を削除する

## 調査結果

### 1. 対象コード

**`ReviewCommentStorage.kt`** (165行)

- **`save()` メソッド** (97-105行目): ストレージファイルにコメントを保存する。100行目で `ensureGitignoreEntry()` を呼び出している。
- **`ensureGitignoreEntry()` メソッド** (107-129行目): `.gitignore` ファイルに `.inline-review-notes/` エントリを自動追加する。
- **companion object** (151-157行目): `GITIGNORE_FILE` (153行目) と `GITIGNORE_ENTRY` (154行目) 定数を含む。

### 2. テストコード

**`ReviewCommentStorageTest.kt`** (140行)

- **`save_writes_json_and_updates_gitignore_once` テスト** (36-67行目): `save()` 呼び出し時に `.gitignore` が正しく更新されることを検証する。

### 3. 他に `.gitignore` 操作を行っている箇所

grep 検索の結果、`.gitignore` を参照しているのは以下のファイルのみ:
- `ReviewCommentStorage.kt` (実装)
- `ReviewCommentStorageTest.kt` (テスト)

他のファイルで `.gitignore` 操作は確認されなかった。

### 4. 変更影響範囲

- **削除対象**: `ensureGitignoreEntry()` メソッド、`GITIGNORE_FILE` 定数、`GITIGNORE_ENTRY` 定数
- **修正対象**: `save()` メソッドから `ensureGitignoreEntry()` 呼び出しを削除、KDoc から `.gitignore` 記述を削除
- **テスト削除対象**: `save_writes_json_and_updates_gitignore_once` テスト
