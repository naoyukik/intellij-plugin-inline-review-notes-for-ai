# Spec: GutterIconによるコメント追加トリガーとActionの実装

## 1. 概要

GitHub Issue #4 に基づき、IntelliJ エディタ上でレビューコメント追加を開始するための UI トリガーを実装する。

ユーザーがコード行を選択している場合は選択範囲の開始行、選択がない場合はカーソル現在行のガター領域に「＋」アイコンを表示する。アイコンをクリックすると `AddReviewCommentAction` を発火し、初回実装ではコメント入力・保存までは行わず、ダミーダイアログで選択されたファイルと行範囲を確認できる状態にする。

## 2. 背景

既存トラック #3 で `ReviewComment` と `ReviewCommentStorage` は実装済みだが、ユーザーが IDE 上でコメント追加を開始する入口はまだ存在しない。Issue #4 は、エディタ左側のガターからコメント追加処理を起動する基礎 UI を作ることを目的とする。

## 3. 機能要件

### 3.1 `AddReviewCommentAction`

- `AnAction` として `AddReviewCommentAction` を追加する。
- Action は IntelliJ のエディタ文脈から現在の `Editor`、対象ファイル、選択範囲またはカーソル位置を取得する。
- 行番号は既存モデル `ReviewComment.lineStart` / `lineEnd` と同じく 1 始まりで扱う。
- 選択範囲が複数行にまたがる場合、開始行と終了行を検出する。
- 選択がない場合、カーソル現在行を `lineStart == lineEnd` として扱う。
- 初回実装では永続化を行わず、ダミーダイアログで対象ファイルと行範囲を表示してアクション発火を確認できるようにする。

### 3.2 ガターアイコン表示

- エディタのガター領域に「＋」アイコンを表示する。
- 表示対象は、選択範囲がある場合は選択範囲の開始行、選択がない場合はカーソル現在行とする。
- アイコンをクリックすると `AddReviewCommentAction` と同等の処理を呼び出す。
- アイコンは現在行または選択範囲の変化に追従し、不要になった古い表示を残さない。

### 3.3 エディタイベント連携

- `SelectionListener` または `CaretListener` 相当のイベント購読により、選択範囲やカーソル位置の変化を検出する。
- エディタ生成・破棄に合わせてリスナーやハイライトを管理し、プロジェクト終了後に参照が残らないようにする。
- 実装では IntelliJ Platform の editor / markup API を利用し、必要に応じて `RangeHighlighter` と `GutterIconRenderer` を使う。

## 4. 非機能要件

- 既存の `model` / `storage` 層に UI 責務を混ぜない。
- Action、ガター表示、イベント購読は責務を分ける。
- `plugin.xml` に必要な Action または起動時リスナー登録を追加する場合は最小限に留める。
- ユーザー向け文言を追加する場合は `MyBundle.properties` を使う。
- テスト可能な行範囲算出ロジックは UI API から分離する。

## 5. 調査結果

- IntelliJ Platform SDK の Action System では、`AnAction` を `plugin.xml` の `<actions>` に登録できる。
- エディタ文脈は `CommonDataKeys.EDITOR` から取得できる。
- ガター表示は IntelliJ editor markup API の `RangeHighlighter` / `GutterIconRenderer` を使う方針で実装フェーズに検証する。

## 6. 受け入れ条件

- エディタ内でコード行を選択すると、該当行のガター領域に「＋」アイコンが表示される。
- 選択がない場合でも、カーソル現在行に「＋」アイコンが表示される。
- 複数行選択時、Action は開始行と終了行を 1 始まりで取得できる。
- 「＋」アイコンをクリックすると `AddReviewCommentAction` 相当の処理が呼び出され、ダミーダイアログで対象ファイルと行範囲を確認できる。
- 古いガターアイコンが残留しない。
- `./gradlew test` が成功する。
- `./gradlew build` が成功する。

## 7. テスト要件

- 行範囲算出ロジックの unit test を追加する。
- 選択なし、単一行選択、複数行選択のケースを検証する。
- Action 発火時に期待する行範囲表示データを組み立てられることをテストする。
- IntelliJ UI 実体が必要な箇所は、可能な範囲で IntelliJ Platform Test Framework を使い、難しい箇所は手動検証計画に明記する。

## 8. 非対象

- コメント本文入力フォームの本実装。
- `ReviewCommentStorage` への保存接続。
- 既存コメントの表示、編集、削除。
- ホバー時のみのアイコン表示。
- severity、snippet、anchorText の追加。
