# Spotless Cache Troubleshooting Guide

このドキュメントは、Spotlessの「JVM-local cache is stale」エラーの予防と対処方法をまとめたものです。

## 問題の概要

### エラーメッセージ
```
Execution failed for task ':spotlessJava'.
> Error while evaluating property 'lineEndingsPolicy' of task ':spotlessJava'.
   > Spotless JVM-local cache is stale. Regenerate the cache with
       rmdir /q /s .gradle/configuration-cache
```

### 原因
Gradleの構成キャッシュ（Configuration Cache）とSpotlessのキャッシュが不整合を起こすことで発生します。
主な原因:
1. SpotlessやGradleのバージョンを変更した後、古い構成キャッシュが残っている
2. `build.gradle`や`gradle.properties`を変更した後、キャッシュが更新されていない
3. Spotless 6.x系の構成キャッシュ対応が不完全だった

## 実装済みの予防策

### 1. Spotless 8.0.0へのアップグレード ✅

**実施日:** 2025-11-11

Spotless 7.0.0以降で構成キャッシュの完全対応が実装されました。
このプロジェクトでは8.0.0（2025年9月24日リリース）を使用しています。

**変更内容:**
- `build.gradle`の5行目: `id 'com.diffplug.spotless' version '8.0.0'`

**参考:**
- [Spotless Issue #987](https://github.com/diffplug/spotless/issues/987) - Configuration cache対応のトラッキング
- [Gradle Configuration Cache Documentation](https://docs.gradle.org/current/userguide/configuration_cache.html)

### 2. 構成キャッシュクリーンアップスクリプト ✅

エラーが発生した場合や、予防的にキャッシュをクリアするためのスクリプトを用意しました。

#### Windows環境（コマンドプロンプト）
```batch
clean-config-cache.bat
```

#### Git Bash / Linux / Mac環境
```bash
./clean-config-cache.sh
```

#### 手動クリーンアップ（Windows）
```batch
rmdir /q /s .gradle\configuration-cache
```

#### 手動クリーンアップ（Bash）
```bash
rm -rf .gradle/configuration-cache
```

### 3. Git Pre-commitフックの自動対応 ✅

`.git/hooks/pre-commit`に以下の機能を実装しました:

1. **設定ファイル変更時の自動クリーンアップ**
   - `build.gradle`, `gradle.properties`, `gradle/`配下のファイルが変更された場合、自動的に構成キャッシュをクリア

2. **Spotless失敗時の自動リトライ**
   - Spotlessがエラーで失敗した場合、構成キャッシュをクリアして自動的に1回リトライ

**コード例:**
```bash
# Check if build.gradle or gradle.properties has been modified
if git diff --cached --name-only | grep -qE '^(build\.gradle|gradle\.properties|gradle/|spotless\.gradle)$'; then
    echo "Build configuration files modified. Cleaning Gradle configuration cache..."
    rm -rf .gradle/configuration-cache
fi

# Run spotless apply to format all files
./gradlew.bat spotlessApply --quiet

# If spotless fails with cache error, clean cache and retry once
if [ $? -ne 0 ]; then
    echo "Spotless failed. Checking for cache issues..."
    rm -rf .gradle/configuration-cache
    ./gradlew.bat spotlessApply --quiet
fi
```

## 手動対処方法

### エラーが発生した場合の対処手順

1. **構成キャッシュをクリア**
   ```bash
   # Git Bash環境
   ./clean-config-cache.sh

   # または手動で
   rm -rf .gradle/configuration-cache
   ```

2. **Spotlessを再実行**
   ```bash
   ./gradlew spotlessApply
   ```

3. **それでも解決しない場合**
   ```bash
   # すべてのGradleキャッシュをクリア（最終手段）
   rm -rf .gradle

   # ビルドを再実行
   ./gradlew clean build
   ```

## 根本的な予防策

### A. バージョンアップ時のチェックリスト

Spotless、Gradle、またはプラグインをアップグレードする際は:

- [ ] 構成キャッシュをクリアする
- [ ] `./gradlew clean`を実行する
- [ ] `./gradlew spotlessCheck`でテストする
- [ ] このドキュメントのバージョン情報を更新する

### B. CI/CD環境での対策

CI環境でこのエラーを予防するには:

```yaml
# GitHub Actions の例
- name: Clear Gradle configuration cache if config changed
  run: |
    if git diff --name-only HEAD~1 | grep -qE '^(build\.gradle|gradle\.properties|gradle/)'; then
      rm -rf .gradle/configuration-cache
    fi

- name: Run Spotless
  run: ./gradlew spotlessCheck
```

### C. 定期的なキャッシュクリーンアップ

ローカル開発環境で定期的に（1週間に1回程度）実行することを推奨:

```bash
# すべてのキャッシュをクリア
rm -rf .gradle/caches
rm -rf .gradle/configuration-cache

# ビルドを再実行
./gradlew clean build
```

## トラブルシューティング

### Q: エラーが頻発する場合

**A:** 以下を確認してください:

1. Spotlessのバージョンが8.0.0以降か確認
   ```bash
   grep "com.diffplug.spotless" build.gradle
   ```

2. 構成キャッシュが有効になっているか確認
   ```bash
   grep "org.gradle.configuration-cache" gradle.properties
   ```

3. 無効化してみる（一時的な対処）
   ```properties
   # gradle.properties
   org.gradle.configuration-cache=false
   ```

### Q: 特定のファイルだけフォーマットエラーが出る

**A:** ファイル固有の問題の可能性があります:

```bash
# 特定のファイルを除外する（build.gradle）
spotless {
    java {
        target 'src/*/java/**/*.java'
        targetExclude 'src/main/java/com/example/ProblematicFile.java'
    }
}
```

### Q: 構成キャッシュを完全に無効化したい

**A:** `gradle.properties`を編集:

```properties
# gradle.properties
org.gradle.configuration-cache=false
```

**注意:** 無効化するとビルドが遅くなる可能性があります。

## 参考リンク

### Gradle公式ドキュメント
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Configuration Cache - Not yet implemented](https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:not_yet_implemented)

### Spotless
- [GitHub: diffplug/spotless](https://github.com/diffplug/spotless)
- [Issue #987: Configuration cache support](https://github.com/diffplug/spotless/issues/987)
- [Spotless Gradle Plugin](https://plugins.gradle.org/plugin/com.diffplug.spotless)

### 関連ドキュメント
- [docs/code-formatting.md](code-formatting.md) - コードフォーマット全般のガイド
- [docs/neoforge-gotchas.md](neoforge-gotchas.md) - NeoForge開発の注意点

## 変更履歴

### 2025-11-11
- **Spotless 8.0.0へアップグレード**
  - 6.25.0から8.0.0へ更新（構成キャッシュの完全対応）
- **構成キャッシュクリーンアップスクリプトを追加**
  - `clean-config-cache.bat`（Windows）
  - `clean-config-cache.sh`（Bash）
- **Git pre-commitフックに自動対応機能を追加**
  - 設定ファイル変更時の自動クリーンアップ
  - Spotless失敗時の自動リトライ
- **このドキュメント作成**

---

**最終更新日:** 2025-11-11
**更新者:** Claude Agent
