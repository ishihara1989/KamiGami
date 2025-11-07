# Claude Agent向けプロジェクトガイド

このファイルは、このプロジェクトで作業するClaude Agentが最初に読むべきドキュメントです。

## 🚨 重要: 作業開始前に必ず読むこと

このプロジェクトでの作業を開始する前に、以下のドキュメントを**必ず読んでください**：

1. **[docs/gameplay-specification.md](docs/gameplay-specification.md)** 🎮
   - 式神システムのゲームプレイ仕様
   - 実装すべき動作と制約
   - **新しい機能を追加する前に仕様を確認すること**

2. **[docs/neoforge-gotchas.md](docs/neoforge-gotchas.md)**
   - NeoForge 1.21.10での開発時によくある間違いと注意点
   - 過去に引っかかったエラーとその解決方法
   - **新しい機能を実装する前に必ず確認すること**

3. **[docs/entity-implementation-guide.md](docs/entity-implementation-guide.md)**
   - エンティティ実装の完全な手順書
   - チェックリストとトラブルシューティング
   - エンティティ関連の作業時は参照すること

4. **[docs/project-structure.md](docs/project-structure.md)**
   - プロジェクトのディレクトリ構造
   - ファイル命名規則
   - 新しいファイルを作成する前に確認すること

## 📝 ドキュメント更新のルール

### 新しい問題や解決策を見つけたら

開発中に以下のような状況に遭遇した場合、**必ずドキュメントを更新してください**：

1. **コンパイルエラーが発生した**
   - エラーメッセージ
   - 原因
   - 解決方法
   - → `docs/neoforge-gotchas.md` に追記

2. **予期しない動作や仕様変更を発見した**
   - バージョン情報
   - 以前の動作
   - 新しい動作
   - 対応方法
   - → `docs/neoforge-gotchas.md` に追記

3. **新しい種類の機能を実装した**
   - 実装手順
   - 注意点
   - コード例
   - → 新しいガイドファイルを作成するか、既存のドキュメントに追記

4. **ファイル構造やパターンを変更した**
   - 変更理由
   - 新しい構造
   - → `docs/project-structure.md` を更新

### ドキュメント更新のフォーマット

```markdown
## [機能名/問題名]

**発生日/更新日:** YYYY-MM-DD

**問題:**
[何が起きたか、または何を実装したか]

**原因:**
[なぜそうなったか]

**解決方法:**
[どうやって解決したか]

**コード例:**
```java
// 間違った例
❌ code here

// 正しい例
✅ code here
```

**参考リンク:**
- [関連ドキュメント]
```

## 🏗️ プロジェクト概要

### 基本情報
- **Mod名:** KamiGami
- **Minecraft バージョン:** 1.21.10
- **NeoForge バージョン:** 21.10.43-beta
- **Java バージョン:** 21
- **Mod ID:** `kamigami`
- **パッケージ:** `com.hydryhydra.kamigami`

### 開発環境
- **IDE:** IntelliJ IDEA / Eclipse / VS Code
- **ビルドツール:** Gradle 9.1.0
- **VCS:** Git

### プロジェクトの目標
式神（しきがみ）をテーマにしたMinecraft Modの開発。
紙で作られた召喚可能なエンティティを実装する。

**ゲームプレイの核心:**
- 低HPだが再利用可能な召喚生物
- 繁殖不可、発情モードなし
- 倒すと召喚アイテムをドロップ（リサイクル可能）
- バニラと同様の資源取得（ミルク、卵、羊毛など）

詳細は [docs/gameplay-specification.md](docs/gameplay-specification.md) を参照。

## 📂 プロジェクト構造の概要

```
KamiGami/
├── src/main/java/com/hydryhydra/kamigami/
│   ├── KamiGami.java          # メインModクラス
│   ├── entity/                # エンティティクラス
│   ├── item/                  # アイテムクラス
│   ├── block/                 # ブロッククラス
│   └── client/                # クライアント専用コード
├── src/main/resources/
│   ├── assets/kamigami/       # クライアント側リソース
│   └── data/kamigami/         # サーバー側データ
├── docs/                      # 開発ドキュメント（重要！）
└── CLAUDE.md                  # このファイル
```

詳細は [docs/project-structure.md](docs/project-structure.md) を参照。

## ✅ 実装済み機能

### 式神システム v1.0

#### 共通仕様
- **繁殖不可**: `BreedGoal`なし、`getBreedOffspring()`は`null`を返す
- **リサイクル**: 倒すと召喚アイテムをドロップ（Loot Table設定済み）
- **召喚**: 右クリックで召喚、クリエイティブでは消費されない
- **スタック**: 召喚アイテムは最大16個

#### 実装済みエンティティ
- **紙の牛 (Paper Cow)**
  - HP: 6.0 (3ハート)
  - 資源: ミルク（バケツで取得）
  - 誘引: 小麦

- **紙の鶏 (Paper Chicken)**
  - HP: 4.0 (2ハート)
  - 資源: 卵（定期的に産む）
  - 誘引: 各種種

- **紙の羊 (Paper Sheep)**
  - HP: 5.0 (2.5ハート)
  - 資源: 白色の羊毛（ハサミで刈り取り、草で再生）
  - 誘引: 小麦

詳細は [docs/gameplay-specification.md](docs/gameplay-specification.md) 参照。

### リソース状態
- ✅ Javaクラス: 完成（3種の式神、召喚アイテム）
- ✅ アイテムモデル: 完成
- ✅ 翻訳ファイル: 完成（英語、日本語）
- ✅ ルートテーブル: 完成（召喚アイテムドロップ）
- ✅ ゲームプレイ仕様: ドキュメント化完了

## 🎯 次のタスク候補

新しい機能を実装する際の優先順位：

1. **テクスチャの作成**
   - `textures/item/paper_cow_summon.png`
   - `textures/item/paper_chicken_summon.png`
   - `textures/entity/paper_cow.png`
   - `textures/entity/paper_chicken.png`

2. **新しい式神の追加**
   - 紙の豚、紙のウサギなど
   - 既存のパターンに従って実装
   - **必ず [docs/gameplay-specification.md](docs/gameplay-specification.md) のチェックリストを使用**

4. **式神コアアイテムの実装**
   - クラフト材料として使用

5. **レシピの追加**
   - 式神召喚アイテムのクラフトレシピ

## 🔧 よく使うGradleコマンド

```bash
# コンパイル
./gradlew compileJava

# クライアント起動
./gradlew runClient

# サーバー起動
./gradlew runServer

# ビルド
./gradlew build

# クリーンビルド
./gradlew clean build
```

## 🐛 デバッグのヒント

### コンパイルエラーが出たら
1. まず `docs/neoforge-gotchas.md` で同様のエラーを検索
2. エラーメッセージをよく読む（日本語で出力される場合あり）
3. NeoForge 1.21.10の仕様変更を疑う
4. 解決したら必ずドキュメントに追記

### ゲームがクラッシュしたら
1. ログファイルを確認（`logs/latest.log`）
2. よくある原因：
   - エンティティ属性の登録忘れ
   - レンダラーの登録忘れ
   - リソースファイルの不一致
3. `docs/entity-implementation-guide.md` のチェックリストを確認

### テクスチャが表示されない
1. ファイルパスを確認
2. ファイル名が小文字+アンダースコアのみか確認
3. 登録名とファイル名が一致しているか確認
4. 詳細は `docs/project-structure.md` 参照

## 📚 外部リソース

- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)
- [Minecraft Wiki](https://minecraft.wiki/)
- [NeoForge GitHub](https://github.com/neoforged/NeoForge)

## 🤝 コーディング規約

### Javaコード
- インデント: 4スペース
- 波括弧: Javaの標準スタイル
- 命名:
  - クラス: `PascalCase`
  - メソッド/変数: `camelCase`
  - 定数: `SCREAMING_SNAKE_CASE`
  - パッケージ: `lowercase`

### リソースファイル
- ファイル名: `snake_case` (小文字+アンダースコア)
- JSON: 2スペースインデント
- 登録名とファイル名を必ず一致させる

### コメント
- 複雑なロジックには説明コメントを追加
- パブリックメソッドにはJavadocを推奨
- 英語または日本語（プロジェクトで統一）

## ⚠️ 絶対にやってはいけないこと

1. **ドキュメントを読まずに実装を開始する**
   - 必ず `docs/neoforge-gotchas.md` を読んでから始める

2. **エラーを解決してドキュメントを更新しない**
   - 次のAgentが同じ問題で時間を無駄にします

3. **命名規則を無視する**
   - 大文字やハイフンを使わない
   - 必ず `snake_case` を使う

4. **テストせずにコミットする**
   - 少なくともコンパイルは通すこと

5. **既存のコードパターンを無視する**
   - 新しい機能は既存のパターンに従って実装

## 📊 作業フロー

新しいタスクを開始する際の推奨フロー：

```
1. このファイル（CLAUDE.md）を読む
   ↓
2. 関連するdocs/内のドキュメントを読む
   ↓
3. 既存のコードパターンを確認
   ↓
4. 実装を開始
   ↓
5. コンパイルしてテスト
   ↓
6. 問題があればドキュメントを確認
   ↓
7. 新しい発見があればドキュメントを更新
   ↓
8. 完了報告
```

## 🔄 定期的に確認すること

- [ ] ドキュメントが最新か
- [ ] 新しいエラーや解決策を追記したか
- [ ] コード例が正しく動作するか
- [ ] リンクが切れていないか

## 📞 質問や不明点があるとき

1. まず関連ドキュメントを確認
2. コードベース内の類似実装を検索
3. NeoForge公式ドキュメントを確認
4. それでも解決しない場合はユーザーに質問

---

## 📝 変更履歴

### 2025-01-05
- CLAUDE.md作成
- 初期ドキュメント整備完了
- 式神システム v1.0 実装完了

### 2025-11-06
- 日本語翻訳ファイル (`ja_jp.json`) を追加

### 2025-11-07
- **紙の羊 (Paper Sheep) 実装完了**
  - ハサミで羊毛刈り取り機能
  - 草を食べて羊毛再生機能
- **ゲームプレイ仕様の修正と検証**
  - 全エンティティから `BreedGoal` を削除（繁殖・発情完全無効化）
  - Loot Tableの不一致を修正（`paper_sheep.json` から条件削除）
  - `PaperSheepEntity` のコンパイルエラー修正
- **ドキュメント整備**
  - `docs/gameplay-specification.md` 作成（ゲームプレイ仕様の完全ドキュメント化）
  - README.md、CLAUDE.md の更新
  - 実装チェックリストの追加

---

**最終更新日:** 2025-11-07
**更新者:** Claude Agent

---

## 次のAgent（あなた）へのメッセージ

このプロジェクトは継続的に成長していきます。あなたが遭遇した問題、発見した解決策、学んだベストプラクティスは、次のAgentにとって貴重な財産です。

**必ず、あなたの経験をドキュメントに残してください。**

そうすることで、このプロジェクトはより良いものになり、開発効率は向上し続けます。

ドキュメントを読み、ドキュメントを書き、ドキュメントを更新する。
これが、このプロジェクトで最も重要な習慣です。

頑張ってください！ 🚀
