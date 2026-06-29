# Spec: データモデルとJSON保存機能の実装

## 1. 概要
インラインレビューコメントを表現するデータモデル `ReviewComment` を定義し、それを現在のGitブランチ名に対応したJSONファイルとして `.inline-review-notes/` ディレクトリ配下に永続化する `ReviewCommentStorage` を実装する。

## 2. 要件
### 2.1 データモデル
- スキーマバージョン: `"1.0"` (ファイル単位のトップレベルに保持)
- `ReviewComment` フィールド:
  - `id`: `String` (UUID v4)
  - `filePath`: `String` (プロジェクトルートからの相対パス)
  - `lineStart`: `Int` (1始まりの開始行)
  - `lineEnd`: `Int` (1始まりの終了行)
  - `comment`: `String` (コメント本文)
  - `createdAt`: `String` (ISO-8601形式の日時文字列)
  - `resolvedAt`: `String?` (未解決の場合は null、解決済みの場合はISO-8601形式の日時文字列)

### 2.2 ファイル保存要件
- 保存ディレクトリ: プロジェクトルート配下の `.inline-review-notes/` （存在しない場合は自动作成）
- ファイル名: `{サニタイズされたブランチ名}.json`
- サニタイズルール:
  - ブランチ名に含まれる英数字、ハイフン(`-`)、アンダースコア(`_`)以外のすべての文字を `_` に置換する。
  - 例: `feature/issue-3` -> `feature_issue-3.json`
  - 例: `bug/fix#123` -> `bug_fix_123.json`
- `.gitignore` への自動追加:
  - `.inline-review-notes/` をプロジェクトの `.gitignore` ファイルに自動的に登録する（すでに登録されている場合はスキップ）。

### 2.3 ブランチ名取得
- 現在のGitブランチ名を IntelliJ Platform SDK の Git API を用いて取得する。
- もしGitリポジトリが初期化されていない、あるいはブランチ名が取得できない場合のフォールバック（例: `"default"` または `"no-branch"`）を定義する。

## 3. テスト要件
- `ReviewComment` のシリアライズ・デシリアライズのテスト。
- ブランチ名サニタイズ処理 of `ReviewCommentStorage` のテスト。
- `ReviewCommentStorage` の読み書きテスト（一時ファイル・ディレクトリを用いたテスト）。
