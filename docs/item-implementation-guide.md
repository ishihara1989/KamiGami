# NeoForge 1.21.10 アイテム実装ガイド

カスタムアイテムを実装する際の完全な手順書です。

## 目次
1. [実装の全体フロー](#実装の全体フロー)
2. [ステップ1: アイテムクラスの作成](#ステップ1-アイテムクラスの作成)
3. [ステップ2: アイテムの登録](#ステップ2-アイテムの登録)
4. [ステップ3: リソースファイル（重要！）](#ステップ3-リソースファイル重要)
5. [ステップ4: 翻訳ファイル](#ステップ4-翻訳ファイル)
6. [チェックリスト](#チェックリスト)
7. [よくある問題とトラブルシューティング](#よくある問題とトラブルシューティング)

---

## 実装の全体フロー

```
1. アイテムクラス作成（必要な場合）
   ↓
2. メインクラスでアイテム登録
   ↓
3. テクスチャファイル作成
   ↓
4. アイテムモデルファイル作成（models/item/）
   ↓
5. 【重要】Item Model Definitionファイル作成（items/）← 忘れやすい！
   ↓
6. 翻訳ファイル追加
   ↓
7. （オプション）クラフトレシピ追加
```

---

## ステップ1: アイテムクラスの作成

### シンプルなアイテム

バニラの`Item`クラスをそのまま使用できます。カスタムクラスは不要です。

### カスタム動作が必要なアイテム

```java
package com.hydryhydra.kamigami.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MyCustomItem extends Item {

    public MyCustomItem(Properties properties) {
        super(properties);
    }

    // ブロックに対して右クリックしたとき
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!level.isClientSide()) {
            // サーバー側の処理
        }

        return InteractionResult.SUCCESS;
    }

    // 空中で右クリックしたとき
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // サーバー側の処理
        }

        return InteractionResult.SUCCESS;
    }
}
```

### エンティティを召喚するアイテムの例

```java
package com.hydryhydra.kamigami.item;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class EntitySummonItem extends Item {
    private final Supplier<EntityType<? extends Mob>> entityTypeSupplier;

    public EntitySummonItem(Supplier<EntityType<? extends Mob>> entityTypeSupplier, Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos().above();
            EntityType<? extends Mob> entityType = entityTypeSupplier.get();

            Mob entity = entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity != null) {
                entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                level.addFreshEntity(entity);

                // クリエイティブモード以外ではアイテムを消費
                if (!context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
```

---

## ステップ2: アイテムの登録

### メインクラス（KamiGami.java）

```java
package com.hydryhydra.kamigami;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class KamiGami {
    public static final String MODID = "kamigami";

    // アイテムレジストリ
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(MODID);

    // シンプルなアイテムの登録
    public static final DeferredItem<Item> MY_SIMPLE_ITEM = ITEMS.register("my_simple_item",
        () -> new Item(new Item.Properties()));

    // スタック数を指定する場合
    public static final DeferredItem<Item> MY_STACKABLE_ITEM = ITEMS.register("my_stackable_item",
        () -> new Item(new Item.Properties().stacksTo(16)));

    // カスタムクラスを使う場合
    public static final DeferredItem<MyCustomItem> MY_CUSTOM_ITEM = ITEMS.register("my_custom_item",
        () -> new MyCustomItem(new Item.Properties()));

    // エンティティ召喚アイテムの登録
    public static final DeferredItem<EntitySummonItem> SUMMON_ITEM = ITEMS.register("summon_item",
        () -> new EntitySummonItem(() -> MY_ENTITY.get(), new Item.Properties().stacksTo(16)));

    // コンストラクタ
    public KamiGami(IEventBus modEventBus, ModContainer modContainer) {
        // レジストリを登録
        ITEMS.register(modEventBus);
    }
}
```

### 重要なポイント

- **登録名は必ず `snake_case`**（小文字+アンダースコア）
- `DeferredRegister.Items` を使用（型安全）
- `stacksTo()` でスタック数を指定（デフォルトは64）
- エンティティを参照する場合は `Supplier` を使用

---

## ステップ3: リソースファイル（重要！）

### ⚠️ NeoForge 1.21.10での重要な変更

**NeoForge 1.21.10では、アイテムのリソースファイルは3つ必要です！**

1. **テクスチャファイル** - `textures/item/`
2. **アイテムモデルファイル** - `models/item/`
3. **🚨 Item Model Definitionファイル** - `items/` ← **これを忘れると表示されない！**

### 必要なファイル構造

```
src/main/resources/assets/kamigami/
├── items/                          ← 🚨 1.21以降は必須！
│   └── my_item.json               ← Item Model Definition
├── models/
│   └── item/
│       └── my_item.json           ← 従来のモデルファイル
└── textures/
    └── item/
        └── my_item.png            ← テクスチャ画像
```

### 1. テクスチャファイル

**ファイルパス:** `src/main/resources/assets/kamigami/textures/item/my_item.png`

- PNG形式
- 推奨サイズ: 16×16ピクセル（バニラと同じ）
- 32×32や64×64も可能（高解像度テクスチャパックとして）

### 2. アイテムモデルファイル

**ファイルパス:** `src/main/resources/assets/kamigami/models/item/my_item.json`

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "kamigami:item/my_item"
  }
}
```

**フィールド説明:**
- `parent`: 親モデル（通常は `minecraft:item/generated`）
- `layer0`: 第1レイヤーのテクスチャパス（mod ID付き）

### 3. 🚨 Item Model Definitionファイル（最重要！）

**ファイルパス:** `src/main/resources/assets/kamigami/items/my_item.json`

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "kamigami:item/my_item"
  }
}
```

**フィールド説明:**
- `type`: モデルタイプ（通常は `minecraft:model`）
- `model`: `models/item/` 内のモデルファイルへの参照（mod ID付き）

### ⚠️ 超重要: ファイル名の一致

すべてのファイル名は**完全に一致**させる必要があります：

| 場所 | ファイルパス | ファイル名 |
|------|-------------|-----------|
| Java登録 | `ITEMS.register("my_item", ...)` | `my_item` |
| Item Model Definition | `assets/kamigami/items/my_item.json` | `my_item` |
| アイテムモデル | `assets/kamigami/models/item/my_item.json` | `my_item` |
| テクスチャ | `assets/kamigami/textures/item/my_item.png` | `my_item` |

**1つでも名前が違うと、アイテムが表示されません！**

---

## ステップ4: 翻訳ファイル

### 英語翻訳

**ファイルパス:** `src/main/resources/assets/kamigami/lang/en_us.json`

```json
{
  "item.kamigami.my_item": "My Item"
}
```

### 日本語翻訳

**ファイルパス:** `src/main/resources/assets/kamigami/lang/ja_jp.json`

```json
{
  "item.kamigami.my_item": "私のアイテム"
}
```

**翻訳キーのフォーマット:**
```
item.<mod_id>.<item_registration_name>
```

---

## チェックリスト

新しいアイテムを追加する際は、この**チェックリストを必ず確認**してください。

### ✅ Javaコード

- [ ] アイテムクラスを作成した（カスタム動作が必要な場合）
- [ ] メインクラスの`DeferredRegister.Items`でアイテムを登録した
- [ ] 登録名は `snake_case` になっている
- [ ] `ITEMS.register(modEventBus)` を呼んでいる

### ✅ リソースファイル（最重要！）

- [ ] **🚨 Item Model Definitionファイルを作成した** (`items/my_item.json`)
- [ ] アイテムモデルファイルを作成した (`models/item/my_item.json`)
- [ ] テクスチャファイルを作成した (`textures/item/my_item.png`)
- [ ] **すべてのファイル名が完全に一致している**

### ✅ 翻訳ファイル

- [ ] 英語翻訳を追加した (`lang/en_us.json`)
- [ ] 日本語翻訳を追加した (`lang/ja_jp.json`)

### ✅ オプション

- [ ] クラフトレシピを追加した（必要な場合）
- [ ] ルートテーブルを追加した（必要な場合）

---

## よくある問題とトラブルシューティング

### 問題1: アイテムがゲーム内で紫と黒の市松模様になる

**原因:** Item Model Definitionファイル（`items/`ディレクトリ）が存在しない

**解決方法:**
1. `src/main/resources/assets/kamigami/items/my_item.json` を作成
2. 以下の内容を記述:
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "kamigami:item/my_item"
     }
   }
   ```

**ログの確認:**
```
[Render thread/WARN] [net.minecraft.client.resources.model.ModelManager/]:
No model loaded for default item model ID kamigami:my_item of kamigami:my_item
```

このエラーが出ている場合、Item Model Definitionファイルが不足しています。

### 問題2: テクスチャが表示されない

**原因1:** ファイル名が一致していない

**解決方法:**
- Java登録名とすべてのリソースファイル名が一致しているか確認
- すべて `snake_case`（小文字+アンダースコア）になっているか確認

**原因2:** ファイルパスが間違っている

**解決方法:**
- `assets/kamigami/` の下に配置されているか確認
- `items/`, `models/item/`, `textures/item/` のディレクトリ構造が正しいか確認

### 問題3: アイテムが登録されない

**原因:** `ITEMS.register(modEventBus)` を呼び忘れている

**解決方法:**
```java
public KamiGami(IEventBus modEventBus, ModContainer modContainer) {
    ITEMS.register(modEventBus);  // この行を追加
}
```

### 問題4: エンティティ召喚アイテムがクラッシュする

**原因:** `EntityType` が `ResourceKey` なしで登録されている

**解決方法:**
- `docs/neoforge-gotchas.md` の「EntityType.Builder.build()の引数」セクションを参照
- `ResourceKey.create()` を使って正しく登録する

---

## 実装例: 式神召喚アイテム

このプロジェクトの実際の実装例を参考にしてください。

### Javaクラス

- `src/main/java/com/hydryhydra/kamigami/item/ShikigamiSummonItem.java`

### リソースファイル

**Item Model Definition:**
- `src/main/resources/assets/kamigami/items/paper_cow_summon.json`
- `src/main/resources/assets/kamigami/items/paper_chicken_summon.json`
- `src/main/resources/assets/kamigami/items/paper_sheep_summon.json`

**アイテムモデル:**
- `src/main/resources/assets/kamigami/models/item/paper_cow_summon.json`
- `src/main/resources/assets/kamigami/models/item/paper_chicken_summon.json`
- `src/main/resources/assets/kamigami/models/item/paper_sheep_summon.json`

**テクスチャ:**
- `src/main/resources/assets/kamigami/textures/item/paper_cow_summon.png`
- `src/main/resources/assets/kamigami/textures/item/paper_chicken_summon.png`
- `src/main/resources/assets/kamigami/textures/item/paper_sheep_summon.png`

---

## 参考リンク

- [NeoForge Documentation - Items](https://docs.neoforged.net/docs/items/)
- [Minecraft Wiki - Item models (1.21+)](https://minecraft.wiki/w/Tutorials/Models#Item_models)
- [docs/neoforge-gotchas.md](neoforge-gotchas.md) - アイテム関連の注意点

---

## まとめ

### 🚨 絶対に忘れてはいけないこと

1. **Item Model Definitionファイル（`items/`ディレクトリ）は必須！**
2. **すべてのファイル名を完全に一致させる**
3. **登録名は必ず `snake_case`**

この3点を守れば、アイテムは正しく表示されます。

新しいアイテムを追加する前に、このドキュメントの**チェックリスト**を必ず確認してください！

---

**最終更新日:** 2025-11-09
**作成者:** Claude Agent
