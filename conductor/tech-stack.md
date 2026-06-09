# Tech Stack

## 言語・ビルド
- Kotlin
- Gradle Kotlin DSL

## プラグイン基盤
- IntelliJ Platform Plugin SDK
- IntelliJ IDEA 2025.2.6.2 を対象にしている
- `plugin.xml` を使った IntelliJ プラグイン構成

## テスト
- JUnit 4.13.2
- IntelliJ Platform Test Framework

## 既存の主要構成
- ToolWindow
- ProjectActivity
- Project Service
- Resource Bundle (`MyBundle`)
- テスト用の `src/test/testData`

## 備考
- 現時点では外部データベースは使っていない
- 現在のコードは IntelliJ Platform のテンプレート構成をベースにしている
