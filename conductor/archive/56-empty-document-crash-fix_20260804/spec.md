# Issue 56 空ドキュメントのクラッシュ修正 仕様

## 概要

GitHub Issue 56 で報告された、レビューコメント対象ファイルを開いた際のクラッシュを修正する。

Issue 56 のスタックトレースでは、空の `Document` に対して `lineCount - 1` が `-1` となり、`ReviewCommentEditorTracker.refresh` の `coerceIn(0, -1)` が `IllegalArgumentException` を発生させている。`CommentInlayManager` にも同じ前提の行位置計算があるため、空ファイルのコメント操作まで一貫して安全に扱う。

- Issue: [#56](https://github.com/naoyukik/intellij-plugin-inline-review-notes-for-ai/issues/56)
- 分類: Bug

## 機能要件

- 保存 JSON が存在しない場合は、既存の `ReviewCommentStorage.load()` の仕様どおり空のコメント一覧として扱う。ストレージ形式、ブランチ名解決、最初のコメント保存時のファイル作成は変更しない。
- 空の `Document` を論理上の 1 行目として扱い、行範囲を `1..1` として処理できるようにする。
- 空ファイルを開いたとき、`ReviewCommentEditorTracker` が行ハイライトとガター追加ボタンの準備で例外を発生させない。
- 空ファイルでガターまたはコンテキストメニューからコメント入力を開始したとき、入力ポップアップの表示位置計算で例外を発生させない。
- 空ファイルにコメントを保存または復元するとき、コメントインレイの位置計算で例外を発生させない。
- 非空ファイルの既存の行範囲、ガター表示、コメント入力、コメント表示の挙動を変更しない。

## 非機能要件

- 修正はエディタ追跡およびコメント表示の行位置計算に限定し、UI、ストレージ、モデルの責務分離を維持する。
- 空範囲 `0..-1` を生成する計算を残さず、IntelliJ エディタ API に渡す位置を有効範囲へ正規化する。
- 既存の Kotlin、IntelliJ Platform SDK、JUnit テスト構成を維持する。

## 受け入れ条件

1. 空ファイルかつ対応する JSON が存在しない状態でファイルを開いても、Issue 56 の `IllegalArgumentException` が発生しない。
2. 同じ状態でガター追加ボタンまたはコンテキストメニューからコメント入力を開始しても、位置計算の例外が発生しない。
3. 空ファイルの論理 1 行目に対するコメント保存・表示処理が既存の保存仕様を壊さない。
4. 非空ファイルに対する既存テストと関連テストがすべて成功する。
5. JSON のない状態を空コメント一覧として扱う既存仕様に変更がない。

## テスト観点

- 空ファイルを開いたときの `ReviewCommentEditorTracker` の追跡とガター準備。
- 空ファイルでの入力ポップアップ位置計算。
- 空ファイルでのコメントインレイ追加または復元。
- JSON 不在時の既存ストレージ動作。
- 非空ファイルの既存行範囲およびコメント操作の回帰。

## 対象ファイル候補

- `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/ReviewCommentEditorTracker.kt`
- `src/main/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/CommentInlayManager.kt`
- `src/test/kotlin/com/github/naoyukik/intellijplugininlinereviewnotesforai/editor/` 配下の関連テスト

## 対象外

- JSON スキーマ、ブランチ名ベースの保存先、コメントモデルの変更。
- 非空ファイルの行追跡アルゴリズムの変更。
- 新しいユーザー向け文言、拡張点、サービス、起動処理の追加。