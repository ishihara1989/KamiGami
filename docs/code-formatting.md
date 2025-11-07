# コードフォーマッティングガイド

このプロジェクトでは、**Spotless**を使用してJavaコードとJSONファイルの自動フォーマットを行います。

## 🎯 概要

- **Java**: Eclipse JDT Formatter（4スペースインデント）
- **JSON**: Gson Formatter（2スペースインデント）
- **その他**: Gradle、Markdownファイルの末尾空白削除

## 📝 利用可能なGradleタスク

### フォーマットをチェック（修正しない）

```bash
./gradlew.bat spotlessCheck
```

コードがフォーマット規則に従っているか確認します。違反があればエラーを報告します。

### フォーマットを自動適用

```bash
./gradlew.bat spotlessApply
```

全てのJavaファイルとJSONファイルを自動的にフォーマットします。

### 特定のフォーマットのみ実行

```bash
# Javaのみ
./gradlew.bat spotlessJava

# JSONのみ
./gradlew.bat spotlessJson

# その他ファイル（.gradle, .md など）のみ
./gradlew.bat spotlessMisc
```

## 🪝 Git Pre-commitフック

### 自動フォーマット機能

このプロジェクトでは、**コミット前に自動的にコードをフォーマット**するpre-commitフックが設定されています。

#### 動作の流れ

1. `git commit` を実行
2. pre-commitフックが自動実行
3. `spotlessApply` でコードを自動フォーマット
4. フォーマットされたファイルを自動的にステージング
5. コミット完了

#### フックの場所

- `.git/hooks/pre-commit` - Bash/Git Bashで実行
- `.git/hooks/pre-commit.bat` - Windowsコマンドプロンプトで実行

### 注意点

- **初回コミット時は時間がかかる場合があります**（依存関係のダウンロード）
- フォーマットが適用された場合、自動的にステージングされます
- フォーマット済みの状態でコミットされるため、コード品質が一定に保たれます

## ⚙️ フォーマット設定の詳細

### Javaフォーマット

[build.gradle](../build.gradle#L174-L191) で設定：

```gradle
java {
    target 'src/*/java/**/*.java'

    eclipse()              // Eclipse JDT Formatter
    endWithNewline()       // ファイル末尾に改行
    removeUnusedImports()  // 未使用インポートを削除
    trimTrailingWhitespace()  // 行末空白を削除
    indentWithSpaces(4)    // 4スペースインデント
}
```

### JSONフォーマット

```gradle
json {
    target 'src/*/resources/**/*.json'

    gson()
        .indentWithSpaces(2)  // 2スペースインデント

    trimTrailingWhitespace()
    endWithNewline()
}
```

### その他ファイル

```gradle
format 'misc', {
    target '*.gradle', '*.md', '.gitignore'

    trimTrailingWhitespace()
    endWithNewline()
}
```

## 🚀 使い方の例

### 開発中の日常的な使い方

```bash
# コードを編集
# ...

# コミット前に確認したい場合（オプション）
./gradlew.bat spotlessCheck

# 通常通りコミット（自動フォーマット有効）
git add .
git commit -m "機能追加"

# pre-commitフックが自動的にフォーマットを適用
# フォーマット済みのコードがコミットされます
```

### 手動でフォーマットを適用したい場合

```bash
# 全ファイルをフォーマット
./gradlew.bat spotlessApply

# ステージング
git add .

# コミット
git commit -m "コードフォーマット適用"
```

### 既存のコードをまとめてフォーマット

```bash
# プロジェクト全体をフォーマット
./gradlew.bat spotlessApply

# 変更を確認
git diff

# 問題なければコミット
git add .
git commit -m "プロジェクト全体をフォーマット"
```

## 🔧 トラブルシューティング

### pre-commitフックが動作しない

**Git Bashの場合:**
```bash
# フックに実行権限を付与
chmod +x .git/hooks/pre-commit
```

**Windowsコマンドプロンプトの場合:**
- `.git/hooks/pre-commit.bat` が自動的に使用されます
- 実行権限の設定は不要です

### フォーマットエラーが出る

```bash
# エラー内容を確認
./gradlew.bat spotlessCheck

# 自動修正を試す
./gradlew.bat spotlessApply

# それでも解決しない場合
./gradlew.bat clean spotlessApply
```

### pre-commitフックをスキップしたい場合

```bash
# コミット時にフックをスキップ（非推奨）
git commit --no-verify -m "メッセージ"
```

**注意:** `--no-verify` の使用は推奨されません。コードの一貫性が保たれなくなります。

## 📊 CI/CDでの使用

継続的インテグレーション（CI）でフォーマットチェックを行う場合：

```bash
# フォーマット違反があればビルド失敗
./gradlew.bat spotlessCheck
```

これにより、フォーマットされていないコードがマージされることを防ぎます。

## ✅ ベストプラクティス

1. **コミット前に必ず自動フォーマットを適用**
   - pre-commitフックが自動的に行います

2. **定期的に`spotlessCheck`を実行**
   - コード品質を維持

3. **チーム全体で同じフォーマット設定を使用**
   - 差分が見やすくなります

4. **大規模なフォーマット変更は別コミットで**
   - レビューしやすくなります

## 🔗 参考リンク

- [Spotless公式ドキュメント](https://github.com/diffplug/spotless)
- [Spotless Gradle Plugin](https://github.com/diffplug/spotless/tree/main/plugin-gradle)
- [Eclipse JDT Formatter](https://help.eclipse.org/latest/index.jsp)

---

**最終更新日:** 2025-11-08
