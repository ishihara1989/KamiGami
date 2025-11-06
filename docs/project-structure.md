# NeoForge Mod プロジェクト構造ガイド

正しいディレクトリ構造とファイル命名規則のガイドです。

## 目次
1. [基本的なプロジェクト構造](#基本的なプロジェクト構造)
2. [Javaパッケージ構造](#javaパッケージ構造)
3. [リソースディレクトリ構造](#リソースディレクトリ構造)
4. [命名規則](#命名規則)
5. [ファイルパスの例](#ファイルパスの例)

---

## 基本的なプロジェクト構造

```
KamiGami/
├── src/
│   ├── main/
│   │   ├── java/                    # Javaソースコード
│   │   │   └── com/hydryhydra/kamigami/
│   │   └── resources/               # リソースファイル
│   │       ├── assets/              # クライアント側リソース
│   │       │   └── kamigami/
│   │       ├── data/                # サーバー側データ
│   │       │   └── kamigami/
│   │       └── META-INF/
│   └── generated/                   # 自動生成されたリソース
│       └── resources/
├── build/                           # ビルド出力（Gitignore）
├── docs/                            # ドキュメント
├── gradle/                          # Gradleラッパー
├── build.gradle                     # Gradleビルドスクリプト
├── settings.gradle                  # Gradleプロジェクト設定
├── gradlew                          # Gradleラッパー（Unix）
├── gradlew.bat                      # Gradleラッパー（Windows）
└── README.md                        # プロジェクト説明
```

---

## Javaパッケージ構造

### 推奨される構造

```
src/main/java/com/hydryhydra/kamigami/
├── KamiGami.java                    # メインModクラス
├── entity/                          # エンティティ関連
│   ├── ShikigamiEntity.java        # 基底エンティティ
│   ├── PaperCowEntity.java
│   └── PaperChickenEntity.java
├── item/                            # アイテム関連
│   ├── ShikigamiSummonItem.java
│   └── ShikigamiCoreItem.java
├── block/                           # ブロック関連
│   └── CustomBlock.java
├── client/                          # クライアント側のみのコード
│   ├── ClientSetup.java
│   └── renderer/                    # レンダラー
│       ├── PaperCowRenderer.java
│       └── PaperChickenRenderer.java
├── init/                            # 初期化・登録クラス（オプション）
│   ├── ModItems.java
│   ├── ModBlocks.java
│   └── ModEntities.java
├── network/                         # ネットワークパケット
│   └── PacketHandler.java
├── util/                            # ユーティリティクラス
│   └── Helper.java
└── config/                          # 設定関連
    └── ModConfig.java
```

### パッケージの用途

| パッケージ | 用途 | 例 |
|---------|------|-----|
| `entity/` | エンティティクラス | Mob、プロジェクタイル |
| `item/` | アイテムクラス | カスタムアイテム、ツール、武器 |
| `block/` | ブロッククラス | カスタムブロック |
| `block/entity/` | ブロックエンティティ | チェスト、かまどなど |
| `client/` | クライアント専用コード | レンダラー、GUI |
| `client/renderer/` | レンダラー | エンティティ、ブロックエンティティ |
| `client/gui/` | GUI画面 | カスタムメニュー |
| `init/` | 登録クラス | まとめて管理する場合 |
| `network/` | ネットワーク通信 | パケット送受信 |
| `util/` | ユーティリティ | ヘルパーメソッド |
| `config/` | 設定ファイル | Mod設定 |

---

## リソースディレクトリ構造

### assets/ (クライアント側リソース)

```
src/main/resources/assets/kamigami/
├── lang/                            # 翻訳ファイル
│   ├── en_us.json                  # 英語
│   └── ja_jp.json                  # 日本語
├── models/                          # モデル定義
│   ├── item/                       # アイテムモデル
│   │   ├── paper_cow_summon.json
│   │   └── paper_chicken_summon.json
│   └── block/                      # ブロックモデル
│       └── custom_block.json
├── textures/                        # テクスチャ画像
│   ├── item/                       # アイテムテクスチャ
│   │   ├── paper_cow_summon.png
│   │   └── paper_chicken_summon.png
│   ├── entity/                     # エンティティテクスチャ
│   │   ├── paper_cow.png
│   │   └── paper_chicken.png
│   └── block/                      # ブロックテクスチャ
│       └── custom_block.png
├── blockstates/                     # ブロックステート定義
│   └── custom_block.json
└── sounds/                          # サウンドファイル
    └── custom_sound.ogg
```

### data/ (サーバー側データ)

```
src/main/resources/data/kamigami/
├── loot_table/                      # ドロップテーブル
│   ├── entities/                    # エンティティドロップ
│   │   ├── paper_cow.json
│   │   └── paper_chicken.json
│   └── blocks/                      # ブロックドロップ
│       └── custom_block.json
├── recipes/                         # クラフトレシピ
│   ├── paper_cow_summon.json
│   └── paper_chicken_summon.json
├── advancements/                    # 実績
│   └── summon_shikigami.json
└── tags/                            # タグ
    ├── items/                       # アイテムタグ
    │   └── summon_items.json
    └── entity_types/                # エンティティタグ
        └── shikigami.json
```

### META-INF/

```
src/main/resources/META-INF/
├── neoforge.mods.toml              # Mod情報（必須）
└── accesstransformer.cfg           # アクセストランスフォーマー（必要な場合）
```

---

## 命名規則

### ファイル名の規則

| 種類 | 命名規則 | 例 |
|-----|---------|-----|
| **Javaクラス** | PascalCase | `PaperCowEntity.java` |
| **JSONファイル** | snake_case | `paper_cow.json` |
| **画像ファイル** | snake_case | `paper_cow.png` |
| **パッケージ名** | lowercase | `com.hydryhydra.kamigami` |
| **登録名** | snake_case | `"paper_cow"` |

### 重要なルール

1. **登録名とファイル名を一致させる**
   ```java
   // 登録名
   ENTITY_TYPES.register("paper_cow", ...);

   // 対応するファイル
   // テクスチャ: textures/entity/paper_cow.png
   // ルートテーブル: loot_table/entities/paper_cow.json
   // 翻訳: "entity.kamigami.paper_cow"
   ```

2. **名前空間（namespace）を使う**
   ```java
   // ✅ 正しい
   ResourceLocation.fromNamespaceAndPath("kamigami", "textures/entity/paper_cow.png");

   // ❌ 間違い（名前空間なし）
   new ResourceLocation("textures/entity/paper_cow.png");
   ```

3. **小文字とアンダースコアのみ使用**
   ```
   ✅ paper_cow.json
   ✅ shikigami_core.png
   ❌ PaperCow.json
   ❌ paper-cow.json
   ❌ paperCow.json
   ```

---

## ファイルパスの例

### エンティティ「paper_cow」の完全なファイル構成

```
# Javaクラス
src/main/java/com/hydryhydra/kamigami/entity/PaperCowEntity.java
src/main/java/com/hydryhydra/kamigami/client/renderer/PaperCowRenderer.java

# テクスチャ
src/main/resources/assets/kamigami/textures/entity/paper_cow.png

# ルートテーブル
src/main/resources/data/kamigami/loot_table/entities/paper_cow.json

# 翻訳（en_us.json内）
"entity.kamigami.paper_cow": "Paper Cow"

# 登録（KamiGami.java内）
ENTITY_TYPES.register("paper_cow", ...);
```

### アイテム「paper_cow_summon」の完全なファイル構成

```
# Javaクラス
src/main/java/com/hydryhydra/kamigami/item/ShikigamiSummonItem.java

# テクスチャ
src/main/resources/assets/kamigami/textures/item/paper_cow_summon.png

# モデル
src/main/resources/assets/kamigami/models/item/paper_cow_summon.json

# レシピ（オプション）
src/main/resources/data/kamigami/recipes/paper_cow_summon.json

# 翻訳（en_us.json内）
"item.kamigami.paper_cow_summon": "Paper Cow Summon"

# 登録（KamiGami.java内）
ITEMS.register("paper_cow_summon", ...);
```

---

## ResourceLocationの使い方

### 基本的な使い方

```java
// モダンな方法（1.21+）
ResourceLocation location = ResourceLocation.fromNamespaceAndPath("kamigami", "textures/entity/paper_cow.png");

// 短縮形（テクスチャパスの場合）
ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("kamigami", "entity/paper_cow");
// 自動的に "textures/" と ".png" が追加される
```

### パスの指定方法

```java
// ✅ 正しい指定
ResourceLocation.fromNamespaceAndPath("kamigami", "entity/paper_cow");
// 実際のファイル: assets/kamigami/textures/entity/paper_cow.png

// ✅ 完全パスでの指定
ResourceLocation.fromNamespaceAndPath("kamigami", "textures/entity/paper_cow.png");

// ❌ 間違い（先頭にスラッシュ）
ResourceLocation.fromNamespaceAndPath("kamigami", "/entity/paper_cow");

// ❌ 間違い（assets/を含める）
ResourceLocation.fromNamespaceAndPath("kamigami", "assets/kamigami/textures/entity/paper_cow.png");
```

---

## よくある間違い

### 1. パスの区切り文字

```
❌ Windows形式（バックスラッシュ）
"textures\entity\paper_cow.png"

✅ Unix形式（スラッシュ）
"textures/entity/paper_cow.png"
```

### 2. ファイル拡張子

```java
// モデルファイル
❌ .register("paper_cow_summon.json", ...)
✅ .register("paper_cow_summon", ...)

// テクスチャファイル
❌ ResourceLocation.fromNamespaceAndPath(MODID, "entity/paper_cow.png");
✅ ResourceLocation.fromNamespaceAndPath(MODID, "entity/paper_cow");
```

### 3. 大文字小文字

```
❌ PaperCow.json
❌ PAPER_COW.json
✅ paper_cow.json

❌ "PaperCow"
✅ "paper_cow"
```

### 4. ハイフンの使用

```
❌ paper-cow.json
✅ paper_cow.json
```

---

## 推奨されるプロジェクト構成パターン

### パターン1: 全てメインクラスに登録（小規模Mod向け）

```java
// KamiGami.java に全ての登録を記述
public class KamiGami {
    public static final DeferredRegister.Items ITEMS = ...;
    public static final DeferredRegister.Blocks BLOCKS = ...;
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = ...;

    public static final DeferredItem<Item> MY_ITEM = ITEMS.register(...);
    public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.register(...);
    public static final DeferredHolder<EntityType<?>, EntityType<MyEntity>> MY_ENTITY = ENTITY_TYPES.register(...);
}
```

**メリット:**
- シンプルで分かりやすい
- 小規模なModに適している

**デメリット:**
- ファイルが大きくなる
- 多くの要素がある場合、管理が難しい

### パターン2: 種類ごとにクラスを分ける（中〜大規模Mod向け）

```java
// init/ModItems.java
public class ModItems {
    public static final DeferredRegister.Items ITEMS = ...;
    public static final DeferredItem<Item> MY_ITEM = ITEMS.register(...);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

// init/ModEntities.java
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = ...;
    public static final DeferredHolder<EntityType<?>, EntityType<MyEntity>> MY_ENTITY = ENTITY_TYPES.register(...);

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}

// KamiGami.java
public class KamiGami {
    public KamiGami(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
    }
}
```

**メリット:**
- コードが整理される
- 各カテゴリを独立して管理できる
- 大規模なModに適している

**デメリット:**
- ファイル数が増える
- 初心者には複雑に見える

---

## デバッグのヒント

### ファイルが見つからないエラー

```
エラー: Failed to load texture: kamigami:entity/paper_cow
```

**確認すること:**
1. ファイルが存在するか: `assets/kamigami/textures/entity/paper_cow.png`
2. ファイル名が正しいか（小文字、アンダースコア）
3. 拡張子が`.png`か
4. パスが正しいか（`textures/entity/`）

### 登録名の不一致

```java
// エンティティ登録
ENTITY_TYPES.register("paper_cow", ...);

// ファイル名
paper_chicken.json  // ❌ 名前が一致していない！

// 修正
paper_cow.json      // ✅ 正しい
```

### IDE設定

**IntelliJ IDEA:**
- リソースフォルダを正しくマークする
- `src/main/resources` を "Resources Root" に設定

**Eclipse:**
- ビルドパスにリソースフォルダを追加

---

## 参考資料

- [NeoForge Documentation - Project Structure](https://docs.neoforged.net/docs/gettingstarted/)
- [Minecraft Wiki - Resource Pack](https://minecraft.wiki/w/Resource_pack)
- [Minecraft Wiki - Data Pack](https://minecraft.wiki/w/Data_pack)

---

**作成日:** 2025-01-05
**対象バージョン:** Minecraft 1.21.10 / NeoForge 21.10.43-beta
