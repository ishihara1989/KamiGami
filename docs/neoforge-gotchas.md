# NeoForge 1.21.10 開発時の注意点

このドキュメントは、NeoForge 1.21.10でのmod開発時に引っかかりやすいポイントをまとめたものです。

## 目次
1. [エンティティ関連](#エンティティ関連)
2. [レンダラー関連](#レンダラー関連)
3. [アイテム関連](#アイテム関連)
   - [アイテムモデル定義ファイルが必須（重要！）](#アイテムモデル定義ファイルが必須重要)
   - [DeferredHolderをSupplierとして使う](#deferredholderをsupplierとして使う)
   - [InteractionResultの変更](#interactionresultの変更)
4. [イベントバス関連](#イベントバス関連)

---

## エンティティ関連

### EntityType.Builder.build()の引数

**❌ 間違い:**
```java
EntityType.Builder.of(MyEntity::new, MobCategory.CREATURE)
    .sized(0.9F, 1.4F)
    .build("my_entity");  // String引数は受け付けない！

// または
EntityType.Builder.of(MyEntity::new, MobCategory.CREATURE)
    .sized(0.9F, 1.4F)
    .build(null); // nullも間違い！
```

**✅ 正解:**
```java
// MyMod.java (抜粋)
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

// ...

EntityType.Builder.of(MyEntity::new, MobCategory.CREATURE)
    .sized(0.9F, 1.4F)
    .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "my_entity")));
```

**理由:** 1.21以降、`build()`メソッドの引数型が`String`や`null`から`ResourceKey<EntityType<?>>`に変更されました。`ResourceKey.create()`と`ResourceLocation.fromNamespaceAndPath()`を使って`ResourceKey`を生成して渡す必要があります。

---

### EntityType.create()の引数

**❌ 間違い:**
```java
EntityType<MyEntity> entityType = ...;
MyEntity entity = entityType.create(serverLevel);  // 引数が足りない！
```

**✅ 正解:**
```java
EntityType<MyEntity> entityType = ...;
MyEntity entity = entityType.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
```

**利用可能なEntitySpawnReason:**
- `NATURAL` - 自然スポーン
- `SPAWN_ITEM_USE` - スポーンアイテム使用（スポーンエッグなど）
- `SPAWNER` - スポーナー
- `BREEDING` - 繁殖
- `MOB_SUMMONED` - Mobによる召喚
- `COMMAND` - コマンド
- その他多数（`EntitySpawnReason`を参照）

---

### Entityの位置設定

**❌ 間違い:**
```java
entity.moveTo(x, y, z, yaw, pitch);  // このメソッドは存在しない！
```

**✅ 正解:**
```java
entity.setPos(x, y, z);      // 位置を設定
entity.setYRot(yaw);         // Y軸回転を設定
entity.setXRot(pitch);       // X軸回転を設定（必要な場合）
```

---

### Entity属性の登録

エンティティ登録だけでなく、**属性の登録も必要**です：

```java
// コンストラクタで属性登録イベントをリッスン
modEventBus.addListener(this::registerEntityAttributes);

// 属性登録メソッド
private void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(MY_ENTITY.get(), MyEntity.createAttributes().build());
}
```

**よくある間違い:** エンティティタイプだけ登録して属性を登録し忘れる → クラッシュします！

---

### TemptGoalを使用する場合はTEMPT_RANGE属性が必須

**発生日:** 2025-11-06

**問題:**
`TemptGoal`を使用しているエンティティを召喚すると、以下のエラーでクラッシュする：
```
java.lang.IllegalArgumentException: Can't find attribute minecraft:tempt_range
    at net.minecraft.world.entity.ai.goal.TemptGoal.canUse(TemptGoal.java:60)
```

**原因:**
Minecraft 1.21.10では、`TemptGoal`を使用するエンティティは**必ず`Attributes.TEMPT_RANGE`属性を登録する必要があります**。
この属性がないと、AIがティック時に属性を参照しようとしてクラッシュします。

**❌ 間違い:**
```java
public static AttributeSupplier.Builder createAttributes() {
    return createShikigamiAttributes()
            .add(Attributes.MAX_HEALTH, 6.0D);
    // TEMPT_RANGE属性がない！
}

@Override
protected void registerGoals() {
    this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, stack -> stack.is(Items.WHEAT), false));
    // TemptGoalを使っているのに属性が登録されていない
}
```

**✅ 正解:**
```java
public static AttributeSupplier.Builder createAttributes() {
    return createShikigamiAttributes()
            .add(Attributes.MAX_HEALTH, 6.0D)
            .add(Attributes.TEMPT_RANGE, 10.0D);  // この行を追加！
}

@Override
protected void registerGoals() {
    this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, stack -> stack.is(Items.WHEAT), false));
    // これでクラッシュしない
}
```

**TEMPT_RANGEの値について:**
- バニラの動物は通常`10.0D`を使用
- この値は、プレイヤーがアイテムを持っている時にエンティティが反応する距離（ブロック単位）
- 値を小さくすると近くでしか反応しなくなる
- 値を大きくすると遠くからでも反応する

**重要:** `TemptGoal`を使用する場合は、**必ず`TEMPT_RANGE`属性を登録すること**。これを忘れるとエンティティがスポーンした瞬間にクラッシュします。

---

## レンダラー関連

### MobRendererの型引数

**発生日:** 2025-11-06

**❌ 間違い:**
```java
// 1.20以前の書き方
public class MyRenderer extends MobRenderer<MyEntity, MyModel> {
    // ...
}
```

**✅ 正解:**
```java
// 1.21以降は3つの型引数が必要
public class MyRenderer extends MobRenderer<MyEntity, MyRenderState, MyModel> {

    @Override
    public MyRenderState createRenderState() {
        return new MyRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(MyRenderState state) {
        return TEXTURE;
    }
}
```

**重要な変更点:**
- `RenderState`型が追加された（Entity、RenderState、Modelの3つ）
- `createRenderState()`メソッドの実装が必須
- `getTextureLocation()`の引数がEntityからRenderStateに変更

**バニラモデルを使う場合の例:**
```java
public class PaperCowRenderer extends MobRenderer<PaperCowEntity, CowRenderState, CowModel> {
    @Override
    public CowRenderState createRenderState() {
        return new CowRenderState();  // バニラのRenderStateを使用
    }
}
```

---

### RenderStateクラスの作成

**発生日:** 2025-11-06

**問題:**
カスタムエンティティのレンダラーを作る際に、RenderStateクラスが必要だが、どう実装すべきか不明確。

**RenderStateの階層構造:**
```
EntityRenderState (基底)
  ↓
LivingEntityRenderState (生物系エンティティ用)
  ↓
SheepRenderState, CowRenderState, など (バニラの具体的なエンティティ用)
```

**✅ バニラのRenderStateを使う場合（推奨）:**
```java
package com.example.client.renderer.state;

import net.minecraft.client.renderer.entity.state.SheepRenderState;

/**
 * Paper Sheepのレンダーステート
 * バニラのSheepRenderStateを継承
 */
public class PaperSheepRenderState extends SheepRenderState {
    // バニラのSheepRenderStateには以下が含まれている:
    // - boolean isSheared
    // - DyeColor woolColor
    // - boolean isJebSheep
    // - float headEatPositionScale
    // - float headEatAngleScale
}
```

**✅ カスタムRenderStateを作る場合:**
```java
package com.example.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MyCustomRenderState extends LivingEntityRenderState {
    // エンティティ固有のレンダリング情報を追加
    public boolean isSpecialMode;
    public float customScale;
    // など
}
```

**❌ 間違った実装:**
```java
// EntityRenderStateに型パラメータを付けてはいけない
public class MyRenderState extends EntityRenderState<MyEntity> {  // ❌
    // ...
}

// コンストラクタでエンティティを受け取る必要はない
public MyRenderState(MyEntity entity) {  // ❌
    super(entity);
}
```

**重要:** RenderStateクラスにはコンストラクタは不要です。フィールドのみを定義します。

---

### extractRenderState()でエンティティデータをRenderStateにコピー

**発生日:** 2025-11-06

RenderStateシステムでは、エンティティのデータをRenderStateオブジェクトにコピーする必要があります。

**✅ 正解:**
```java
@Override
public void extractRenderState(PaperSheepEntity entity, PaperSheepRenderState state, float partialTick) {
    super.extractRenderState(entity, state, partialTick);

    // エンティティからRenderStateにデータをコピー
    state.isSheared = entity.isSheared();
    state.woolColor = DyeColor.WHITE;
    state.isJebSheep = false;
}
```

**重要なポイント:**
- `super.extractRenderState()`を最初に呼ぶ
- エンティティの状態を読み取ってRenderStateのフィールドに設定
- RenderStateは「スナップショット」として機能する

---

### RenderLayerの変更（render → submit）

**発生日:** 2025-11-06

**問題:**
1.20以前の`RenderLayer`は`render()`メソッドを使っていたが、1.21.10では`submit()`メソッドに変更された。

**❌ 間違い (1.20):**
```java
public class MyLayer extends RenderLayer<MyEntity, MyModel<MyEntity>> {
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       MyEntity entity, float limbSwing, ...) {
        // レンダリング処理
    }
}
```

**✅ 正解 (1.21.10):**
```java
public class MyLayer extends RenderLayer<MyRenderState, MyModel> {
    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight,
                       MyRenderState renderState, float yRot, float xRot) {
        // レンダリング処理
    }
}
```

**主な変更点:**
1. **型パラメータが変更**: `<Entity, Model>` → `<RenderState, Model>`
2. **メソッド名が変更**: `render()` → `submit()`
3. **第2引数が変更**: `MultiBufferSource` → `SubmitNodeCollector`
4. **第4引数が変更**: `Entity entity` → `RenderState renderState`
5. **その他の引数が簡略化**: `limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch` → `yRot, xRot`

**インポートの変更:**
```java
// 1.21.10で追加
import net.minecraft.client.renderer.SubmitNodeCollector;
```

**完全な例（羊の毛レイヤー）:**
```java
package com.example.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;

public class PaperSheepFurLayer extends RenderLayer<PaperSheepRenderState, SheepModel> {
    private static final ResourceLocation FUR_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mymod", "textures/entity/sheep/fur.png");

    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public PaperSheepFurLayer(RenderLayerParent<PaperSheepRenderState, SheepModel> parent,
                              EntityModelSet modelSet) {
        super(parent);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight,
                       PaperSheepRenderState renderState, float yRot, float xRot) {
        if (!renderState.isSheared) {
            EntityModel<SheepRenderState> model = renderState.isBaby ? this.babyModel : this.adultModel;
            int woolColor = renderState.getWoolColor();

            coloredCutoutModelCopyLayerRender(
                model,
                FUR_TEXTURE,
                poseStack,
                nodeCollector,
                packedLight,
                renderState,
                woolColor,
                0
            );
        }
    }
}
```

**重要:**
- `SubmitNodeCollector`は`net.minecraft.client.renderer`パッケージにある
- `coloredCutoutModelCopyLayerRender()`メソッドのシグネチャも変更されている

---

### レンダラー登録

**❌ 間違い:**
```java
@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
// busパラメータは存在しない！
```

**✅ 正解:**
```java
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MY_ENTITY.get(), MyRenderer::new);
    }
}
```

**注意点:**
- `bus`パラメータは削除されました
- デフォルトでMODバスに登録されます
- `@SubscribeEvent`アノテーションで適切なイベントをリッスンします

---

### CompoundTagのgetBoolean()がOptional型を返す

**発生日:** 2025-11-06

**問題:**
`CompoundTag.getBoolean()`メソッドの戻り値が`boolean`から`Optional<Boolean>`に変更された。

**❌ 間違い:**
```java
protected void loadData(CompoundTag tag) {
    this.setSheared(tag.getBoolean("Sheared"));  // エラー: Optional<Boolean>をbooleanに変換できない
}
```

**✅ 正解:**
```java
protected void loadData(CompoundTag tag) {
    this.setSheared(tag.getBoolean("Sheared").orElse(false));
}
```

**重要なポイント:**
- `getBoolean()`は`Optional<Boolean>`を返す
- `.orElse(デフォルト値)`を使ってOptionalから値を取り出す
- キーが存在しない場合はデフォルト値が使われる

---

## アイテム関連

### アイテムモデル定義ファイルが必須（重要！）

**発生日:** 2025-11-06

**問題:**
アイテムモデルファイル（`models/item/*.json`）とテクスチャファイルが存在するのに、ゲーム内でテクスチャが表示されない。

ログに以下のエラーが出力される：
```
[Render thread/WARN] [net.minecraft.client.resources.model.ModelManager/]: No model loaded for default item model ID kamigami:my_item of kamigami:my_item
```

**原因:**
**NeoForge 1.21.10では、新しい「Item Model Definition」システムが導入されました**。
`models/item/`にモデルファイルがあっても、**追加で`items/`ディレクトリにItem Model Definitionファイルを作成する必要があります**。

これはMinecraft 1.21での大きな変更点で、アイテムモデルの読み込み方法が根本的に変わりました。

**ディレクトリ構造:**
```
src/main/resources/assets/kamigami/
├── items/                          ← 新しく必要！（1.21以降）
│   ├── my_item.json               ← Item Model Definition
│   ├── another_item.json
│   └── ...
├── models/
│   └── item/
│       ├── my_item.json           ← 従来のモデルファイル
│       ├── another_item.json
│       └── ...
└── textures/
    └── item/
        ├── my_item.png
        ├── another_item.png
        └── ...
```

**❌ 間違い（1.20以前の方法）:**
```
assets/kamigami/
├── models/item/my_item.json  ← これだけでは不十分！
└── textures/item/my_item.png
```

**✅ 正解（1.21以降）:**

1. **Item Model Definitionファイルを作成:** `assets/kamigami/items/my_item.json`
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "kamigami:item/my_item"
  }
}
```

2. **従来のモデルファイル:** `assets/kamigami/models/item/my_item.json`
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "kamigami:item/my_item"
  }
}
```

3. **テクスチャファイル:** `assets/kamigami/textures/item/my_item.png`

**重要なポイント:**
- **`items/`ディレクトリは必須**。これがないとモデルが読み込まれない
- Item Model Definitionファイルの名前は**アイテムの登録名と完全に一致**させる
- `model`フィールドで従来の`models/item/`にあるモデルファイルを参照する
- ファイル名はすべて`snake_case`（小文字+アンダースコア）

**実装例（召喚アイテムの場合）:**

`assets/kamigami/items/paper_cow_summon.json`:
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "kamigami:item/paper_cow_summon"
  }
}
```

`assets/kamigami/models/item/paper_cow_summon.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "kamigami:item/paper_cow_summon"
  }
}
```

**デバッグ方法:**
1. ゲームを起動して`runs/client/logs/latest.log`を確認
2. "No model loaded for default item model ID"というエラーを探す
3. エラーがある場合は`items/`ディレクトリとファイル名を確認

**参考:**
- [Minecraft Wiki - Item models (1.21+)](https://minecraft.wiki/w/Tutorials/Models#Item_models)
- NeoForge 1.21では、このシステムにより条件付きモデルや動的モデルがサポートされるようになりました

---

### DeferredHolderをSupplierとして使う

**❌ 間違い:**
```java
public class MyItem extends Item {
    private final DeferredHolder<EntityType<?>, EntityType<MyEntity>> entityType;

    public MyItem(DeferredHolder<EntityType<?>, EntityType<MyEntity>> entityType) {
        this.entityType = entityType;
        // DeferredHolderはSupplier<EntityType<...>>ではない！
    }
}
```

**✅ 正解:**
```java
public class MyItem extends Item {
    private final Supplier<EntityType<? extends Mob>> entityTypeSupplier;

    public MyItem(Supplier<EntityType<? extends Mob>> entityTypeSupplier) {
        this.entityTypeSupplier = entityTypeSupplier;
    }
}

// 登録時
ITEMS.register("my_item",
    () -> new MyItem(() -> MY_ENTITY.get()));  // lambdaでラップ
```

**理由:** `DeferredHolder`は`Supplier`を実装していますが、型引数が一致しないため明示的にラップが必要です。

---

### InteractionResultの変更

**❌ 間違い:**
```java
return InteractionResult.sidedSuccess(level.isClientSide);  // メソッドが存在しない
```

**✅ 正解:**
```java
// サーバー側とクライアント側で異なる結果を返す場合
return !level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;

// または単純に
return InteractionResult.SUCCESS;
return InteractionResult.CONSUME;
```

---

## イベントバス関連

### EntityAttributeCreationEventの登録

**正しい登録方法:**
```java
public MyMod(IEventBus modEventBus, ModContainer modContainer) {
    // エンティティタイプを登録
    ENTITY_TYPES.register(modEventBus);

    // 属性イベントをリッスン（重要！）
    modEventBus.addListener(this::registerEntityAttributes);
}

private void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(MY_ENTITY.get(), MyEntity.createAttributes().build());
}
```

**よくある間違い:**
- `NeoForge.EVENT_BUS`に登録してしまう → 呼ばれません！
- MODイベントバス（`modEventBus`）に登録する必要があります

---

## その他の注意点

### Level.isClientSideの変更

**❌ 間違い:**
```java
if (level.isClientSide) {  // フィールドアクセスは不可
    // ...
}
```

**✅ 正解:**
```java
if (level.isClientSide()) {  // メソッド呼び出し
    // ...
}
```

---

### 非推奨メソッド

以下のメソッドは非推奨です（削除予定）：

```java
// ❌ 非推奨
BLOCKS.registerSimpleBlock("name", properties);
ITEMS.registerSimpleItem("name", properties);

// ✅ 代替方法（推奨）
BLOCKS.register("name", () -> new Block(properties));
ITEMS.register("name", () -> new Item(properties));
```

---

## レシピ関連

### レシピディレクトリ名の変更（重要！）

**発生日:** 2025-11-07

**問題:**
Minecraft 1.21以降、レシピファイルのディレクトリ名が変更されました。

**❌ 間違い（1.20以前）:**
```
src/main/resources/data/kamigami/recipes/my_recipe.json
```

**✅ 正解（1.21以降）:**
```
src/main/resources/data/kamigami/recipe/my_recipe.json
```

**重要なポイント:**
- ディレクトリ名は `recipes`（複数形）から `recipe`（単数形）に変更
- この変更に気づかないと、レシピが一切読み込まれない
- ログに「Loaded 1461 recipes」のようにバニラのレシピ数しか表示されない
- エラーメッセージは出ない（サイレント失敗）

**確認方法:**
1. ゲーム起動後、ログファイル `runs/client/logs/latest.log` を確認
2. "Loaded XXXX recipes" の数がバニラ（1461）より多いか確認
3. JEI経由でレシピが表示されるか確認

---

### レシピのIngredient（材料）の記法（NeoForge 1.21.2+）

**発生日:** 2025-11-07

**重要な仕様変更:**
NeoForge 1.21.2以降では、レシピの材料（ingredient）の記法が変更されました。
**バニラアイテムは文字列、カスタムIngredientはオブジェクト形式**を使います。

#### バニラアイテムを材料にする場合（推奨）

**✅ 正解（NeoForge 1.21.2+）:**
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["BB", "BB"],
  "key": {
    "B": "minecraft:bamboo"
  },
  "result": {
    "id": "minecraft:bamboo_planks",
    "count": 1
  }
}
```

**❌ 間違い（オブジェクト形式はカスタムIngredient専用）:**
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["BB", "BB"],
  "key": {
    "B": {
      "item": "minecraft:bamboo"
    }
  },
  "result": {
    "id": "minecraft:bamboo_planks",
    "count": 1
  }
}
```

このオブジェクト形式を使うと以下のエラーが出ます：
```
Couldn't parse data file 'kamigami:bamboo_plank' from 'kamigami:recipe/bamboo_plank.json':
DataResult.Error['Map entry 'B' : Failed to parse either.
First: Input does not contain a key [type]: MapLike[{"item":"minecraft:bamboo"}]
Second: ... Input does not contain a key [neoforge:ingredient_type]: MapLike[{"item":"minecraft:bamboo"}]
```

#### 材料の種類別の正しい記法

**1. バニラアイテム（文字列形式）:**
```json
{
  "key": {
    "S": "minecraft:stick"
  }
}
```

**2. アイテムタグ（文字列形式、`#`で始まる）:**
```json
{
  "key": {
    "W": "#minecraft:planks"
  }
}
```

**3. Shapelessレシピの材料リスト:**
```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    "minecraft:paper",
    "minecraft:stick",
    "#minecraft:planks"
  ],
  "result": {
    "id": "kamigami:example_item",
    "count": 1
  }
}
```

**4. カスタムIngredient（オブジェクト形式、`neoforge:ingredient_type`が必要）:**
```json
{
  "key": {
    "C": {
      "neoforge:ingredient_type": "kamigami:custom_ingredient",
      "item": "kamigami:special_item",
      "custom_data": {...}
    }
  }
}
```

#### 重要なポイント

- **バニラのアイテムやタグは必ず文字列で指定**
  - アイテム: `"minecraft:stick"`
  - タグ: `"#minecraft:planks"` (先頭に`#`を付ける)
- **オブジェクト形式はカスタムIngredient専用**
  - `neoforge:ingredient_type` フィールドが必須
  - 独自の材料マッチングロジックを実装する場合のみ使用
- **NeoForge 1.21.0-1.21.1では旧形式（オブジェクト）も動作するが、1.21.2+では文字列形式に統一**

#### バージョン別の対応

| NeoForge バージョン | バニラアイテム | タグ | カスタムIngredient |
|-------------------|-------------|------|-------------------|
| 1.20以前 | オブジェクト `{"item":"..."}` | オブジェクト `{"tag":"..."}` | 非対応 |
| 1.21.0-1.21.1 | オブジェクトor文字列 | オブジェクトor文字列 | オブジェクト（`type`フィールド） |
| 1.21.2+ | **文字列** `"namespace:item"` | **文字列** `"#namespace:tag"` | オブジェクト（`neoforge:ingredient_type`） |

**参考:**
- [NeoForged Documentation - Ingredients (1.21.4)](https://docs.neoforged.net/docs/1.21.4/resources/server/recipes/ingredients)

---

## デバッグのヒント

### ログファイルの場所

**開発環境でのログとクラッシュレポート:**

- **最新ログ:** `runs/client/logs/latest.log` または `runs/server/logs/latest.log`
  - 常に最新の実行ログがここに出力される
  - 問題解決時はまずこのファイルを確認

- **クラッシュレポート:** `runs/client/crash-reports/` または `runs/server/crash-reports/`
  - ゲームクラッシュ時に詳細なレポートが生成される
  - ファイル名にタイムスタンプが含まれる

- **古いログ:** `runs/client/logs/` または `runs/server/logs/`
  - 過去のログは圧縮されて保存される（`.log.gz`）

**開発用Modの追加:**

開発クライアント/サーバーにJEIなどのModを追加する場合：
- **配置場所:** `runs/client/mods/` または `runs/server/mods/`
- Modの `.jar` ファイルを直接このディレクトリに配置
- クライアント再起動で自動的に読み込まれる
- ログの "Mod List:" セクションで読み込まれたか確認できる

**注意:** `run/mods/` ではなく `runs/client/mods/` が正しいディレクトリです。

---

### コンパイルエラーの読み方

NeoForgeのコンパイルエラーは日本語で出力されることがあります：

```
エラー: 不適合な型: StringをResourceKey<EntityType<?>>に変換できません
```

この場合、型の不一致が原因です。ドキュメントでメソッドシグネチャを確認しましょう。

### よくあるクラッシュ原因

1. **Entity属性の登録忘れ** → `EntityAttributeCreationEvent`で登録
2. **クライアント側でのレンダラー未登録** → `EntityRenderersEvent.RegisterRenderers`で登録
3. **リソース名の不一致** → 登録名とファイル名を一致させる
4. **レシピディレクトリ名が間違っている** → 1.21以降は `recipe`（単数形）を使用
5. **BlockのResourceKey設定忘れ** → 1.21以降は `BlockBehaviour.Properties#setId()` が必須

---

## ブロック関連

### BlockBehaviour.PropertiesにはsetId()が必須（NeoForge 1.21+）

**発生日:** 2025-11-07

**問題:**
カスタムブロックを登録して起動すると、以下のエラーでクラッシュする：
```
java.lang.IllegalStateException: Trying to access unbound value: ResourceKey[minecraft:block / modid:my_block]
```

このエラーは、`BlockItem`や`BlockEntityType`の登録時に`DeferredBlock.get()`を呼び出した際に発生する。

**原因:**
**NeoForge 1.21以降、カスタムブロックの`BlockBehaviour.Properties`には必ず`setId()`でResourceKeyを設定する必要があります。**

これを忘れると、ブロックがレジストリに正しくバインドされず、`DeferredHolder`が「unbound（未バインド）」状態のままになります。その結果、他の登録（BlockItemやBlockEntityType）でこのブロックを参照しようとすると、上記のエラーが発生します。

**❌ 間違い（1.20以前の書き方）:**
```java
public static final DeferredBlock<MyBlock> MY_BLOCK = BLOCKS.register("my_block",
    () -> new MyBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)));
// setId()がないため、ブロックがレジストリにバインドされない！
```

**✅ 正解（NeoForge 1.21以降）:**
```java
public static final DeferredBlock<MyBlock> MY_BLOCK = BLOCKS.register("my_block",
    () -> new MyBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .setId(ResourceKey.create(Registries.BLOCK,
                   ResourceLocation.fromNamespaceAndPath(MODID, "my_block")))));
```

**必要なインポート:**
```java
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
```

**より簡潔な方法（推奨）:**

NeoForgeの`DeferredRegister.Blocks`には`registerBlock()`という便利なヘルパーメソッドがあり、これを使うと自動的に`setId()`が呼ばれます：

```java
// registerBlock()を使用する場合（推奨）
public static final DeferredBlock<MyBlock> MY_BLOCK = BLOCKS.registerBlock(
    "my_block",
    MyBlock::new,  // コンストラクタ参照
    BlockBehaviour.Properties.of()
        .mapColor(MapColor.WOOD)
        .strength(2.0F, 3.0F)
        .sound(SoundType.WOOD)
);
```

この方法を使うと、ResourceKeyの設定を手動で行う必要がなくなります。

---

### BlockItemの登録方法（NeoForge 1.21+）

**発生日:** 2025-11-07

**問題:**
ブロックアイテムを登録する際の正しい方法が分からない。

**✅ 正しい登録方法:**

NeoForge 1.21以降、`ITEMS.registerItem()`を使用すると、自動的に`Item.Properties#setId()`が呼ばれます。また、ブロックアイテムの場合は`useBlockDescriptionPrefix()`を使用することが推奨されます：

```java
public static final DeferredItem<BlockItem> MY_BLOCK_ITEM = ITEMS.registerItem("my_block",
    properties -> new BlockItem(MY_BLOCK.get(), properties.useBlockDescriptionPrefix()));
```

**重要なポイント:**
- `ITEMS.registerItem()`は自動的にアイテムIDを設定します
- `useBlockDescriptionPrefix()`はブロックの説明文をアイテムにも適用します
- ブロックの`.get()`はラムダ内で呼ばれるため、循環参照の問題は発生しません

**より簡潔な方法（ブロックとアイテムを同時登録）:**

NeoForgeの`registerSimpleBlockItem()`を使うと、さらに簡潔に書けます：

```java
// ブロック登録
public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.registerSimpleBlock(
    "my_block",
    BlockBehaviour.Properties.of()
        .mapColor(MapColor.WOOD)
        .strength(2.0F, 3.0F)
);

// ブロックアイテム登録（自動的にBlockItemが作成される）
public static final DeferredItem<BlockItem> MY_BLOCK_ITEM =
    ITEMS.registerSimpleBlockItem(MY_BLOCK);
```

---

### BlockEntityTypeの登録

**発生日:** 2025-11-07

**問題:**
`BlockEntityType`を登録する際の正しい方法は？

**✅ 正解:**

NeoForge 1.21.10では、`BlockEntityType`のコンストラクタを直接使用します：

```java
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MyBlockEntity>> MY_BLOCK_ENTITY =
    BLOCK_ENTITY_TYPES.register("my_block",
        () -> new BlockEntityType<>(MyBlockEntity::new, MY_BLOCK.get()));
```

**重要なポイント:**
- `BlockEntityType.Builder`は削除されました（1.21以降）
- コンストラクタの第1引数：BlockEntityのコンストラクタ参照
- 第2引数以降：このBlockEntityTypeを使用できるブロック（可変長引数）
- ブロックの`.get()`はラムダ内で呼ばれるため、ブロックが先に登録されていれば問題ありません

**BlockEntityのコンストラクタ:**
```java
public class MyBlockEntity extends BlockEntity {
    public MyBlockEntity(BlockPos pos, BlockState blockState) {
        super(MY_BLOCK_ENTITY.get(), pos, blockState);
    }
    // ...
}
```

**参考:**
- [NeoForge Documentation - Block Entities](https://docs.neoforged.net/docs/blockentities/)

---

## BlockEntity関連

### BlockEntityのクライアント同期にはgetUpdateTag()が必須（NeoForge 1.21.10）

**発生日:** 2025-11-07

**問題:**
BlockEntityにアイテムを保存し、`setChanged()`と`level.sendBlockUpdated()`を呼び出しても、クライアント側でデータが同期されない。レンダラーでアイテムを描画しようとしても、常に空（`ItemStack.EMPTY`）として認識される。

ログを確認すると：
```
[Server thread] setStoredItem called - New: minecraft:iron_ingot
[Server thread] Sending block update to client for position BlockPos{x=-9, y=74, z=15}
[Render thread] loadAdditional at BlockPos{x=-9, y=74, z=15} - Loaded: EMPTY
```

サーバー側ではアイテムが設定されているのに、クライアント側では`EMPTY`として読み込まれている。

**原因:**
**NeoForge 1.21.10では、BlockEntityのクライアント同期に`getUpdateTag()`メソッドのオーバーライドが必須です。**

`getUpdatePacket()`だけを実装しても、初期チャンク読み込み時やブロック更新時のデータ同期は行われません。`getUpdateTag()`は、クライアントに送信するNBTデータを返すメソッドで、これがないとカスタムデータが一切同期されません。

**❌ 間違い（`getUpdateTag()`がない）:**
```java
public class ShrineBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;

    public void setStoredItem(ItemStack stack) {
        this.storedItem = stack;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.storedItem.isEmpty()) {
            output.store("StoredItem", ItemStack.CODEC, this.storedItem);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedItem = input.read("StoredItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    // getUpdateTag()がない！→ クライアントにデータが送信されない
}
```

**✅ 正解（`getUpdateTag()`を実装）:**
```java
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public class ShrineBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;

    public void setStoredItem(ItemStack stack) {
        this.storedItem = stack;
        this.setChanged();
        // Sync to client
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.storedItem.isEmpty()) {
            output.store("StoredItem", ItemStack.CODEC, this.storedItem);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedItem = input.read("StoredItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    // For client synchronization
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        // Encode ItemStack using CODEC
        if (!this.storedItem.isEmpty()) {
            DataResult<Tag> result = ItemStack.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE),
                this.storedItem
            );
            result.ifSuccess(nbtTag -> tag.put("StoredItem", nbtTag));
        }
        return tag;
    }
}
```

**重要なポイント:**

1. **`getUpdateTag()`は必須**
   - このメソッドがないと、`sendBlockUpdated()`を呼んでもクライアントにカスタムデータが送信されない
   - クライアント側の`loadAdditional()`は自動的に呼ばれる

2. **ItemStackの正しいエンコード方法**
   - `ItemStack.CODEC.encodeStart()`を使ってNBTタグに変換
   - `registries.createSerializationContext(NbtOps.INSTANCE)`でシリアライゼーションコンテキストを作成
   - `DataResult.ifSuccess()`でエンコード結果を安全に処理

3. **同期の流れ**
   ```
   サーバー側:
   1. setStoredItem() でデータを変更
   2. level.sendBlockUpdated() でクライアントに通知
   3. getUpdateTag() が呼ばれ、NBTタグを返す

   クライアント側:
   4. NBTタグを受信
   5. loadAdditional() が自動的に呼ばれる
   6. storedItem フィールドが更新される
   7. レンダラーが正しいデータを取得できる
   ```

4. **デバッグ方法**
   - `getUpdateTag()`と`loadAdditional()`にログを追加
   - サーバー側で`getUpdateTag()`が呼ばれているか確認
   - クライアント側で`loadAdditional()`でデータが読み込まれているか確認

**必要なインポート:**
```java
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
```

**参考:**
- [NeoForge Documentation - Block Entities: Synchronization](https://docs.neoforged.net/docs/blockentities/bes)
- この問題は特にBlockEntityRendererでカスタムデータを使用する場合に顕在化します

---

## ルートテーブル（Loot Tables）関連

### カスタムエンティティのルートテーブルでtype_specificは使えない

**発生日:** 2025-11-08

**問題:**
カスタムスライム型エンティティを実装し、バニラのスライムと同様にサイズによってドロップを変える仕様にした。ルートテーブルで `type_specific` を使ってサイズをチェックしたが、サイズ1のスライムを倒しても何もドロップしない。

```json
{
  "conditions": [
    {
      "condition": "minecraft:entity_properties",
      "entity": "this",
      "predicate": {
        "type_specific": {
          "type": "minecraft:slime",
          "size": {
            "max": 1
          }
        }
      }
    }
  ]
}
```

**原因:**
`type_specific` の `type` フィールドは**バニラのエンティティタイプのみ**をサポートしています。カスタムエンティティ（例：`kamigami:tatari_slime`）は、たとえバニラのSlimeクラスを継承していても、`"type": "minecraft:slime"` の条件にはマッチしません。

この条件は文字通り「このエンティティのタイプが `minecraft:slime` であるか」をチェックするため、カスタムエンティティでは**常に失敗**します。

**❌ 間違い（カスタムエンティティでは動作しない）:**
```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:slime_ball"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:entity_properties",
          "entity": "this",
          "predicate": {
            "type_specific": {
              "type": "minecraft:slime",  // ❌ カスタムエンティティには使えない
              "size": {
                "max": 1
              }
            }
          }
        }
      ]
    }
  ]
}
```

**✅ 正解（NBTベースでチェック）:**
```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:slime_ball"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:entity_properties",
          "entity": "this",
          "predicate": {
            "nbt": "{Size:1}"  // ✅ NBTタグで直接チェック
          }
        }
      ]
    }
  ]
}
```

**重要なポイント:**

1. **NBTベースの条件を使用**
   - カスタムエンティティでは、`type_specific` の代わりに `nbt` プロパティを使用
   - エンティティがNBTに保存しているデータを直接チェックできる

2. **エンティティ側でNBTを正しく保存する**
   ```java
   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
       super.addAdditionalSaveData(output);
       output.putInt("Size", this.getSize());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
       super.readAdditionalSaveData(input);
       int size = input.getIntOr("Size", 1);
       this.setSize(size, false);
   }
   ```

3. **デバッグ方法**
   - エンティティの `remove()` メソッドにログを追加して、死亡時の条件を確認
   - ルートテーブルの条件が正しく評価されているか確認
   ```java
   @Override
   public void remove(Entity.RemovalReason reason) {
       int size = this.getSize();
       LOGGER.info("Entity removed - Size: {}, isDead: {}, isClientSide: {}",
           size, this.isDeadOrDying(), this.level().isClientSide());
       super.remove(reason);
   }
   ```

4. **複数のアイテムをドロップする場合**
   - 複数の `pools` を使用して、それぞれ独立したアイテムをドロップ
   ```json
   {
     "type": "minecraft:entity",
     "pools": [
       {
         "rolls": 1,
         "entries": [{ "type": "minecraft:item", "name": "minecraft:slime_ball" }],
         "conditions": [{ "condition": "minecraft:entity_properties", "entity": "this", "predicate": { "nbt": "{Size:1}" } }]
       },
       {
         "rolls": 1,
         "entries": [{ "type": "minecraft:item", "name": "minecraft:ink_sac" }],
         "conditions": [{ "condition": "minecraft:entity_properties", "entity": "this", "predicate": { "nbt": "{Size:1}" } }]
       }
     ]
   }
   ```
   - 上記の例では、サイズ1のエンティティを倒すと、スライムボール1個とイカスミ1個の両方がドロップします

**参考:**
- [Minecraft Wiki - Loot Tables](https://minecraft.wiki/w/Loot_table)
- [Minecraft Wiki - Predicates](https://minecraft.wiki/w/Predicate)
- `type_specific` は以下のバニラエンティティタイプのみサポート：
  - `minecraft:slime`
  - `minecraft:fishing_hook`
  - `minecraft:player`
  - `minecraft:cat`
  - `minecraft:raider`
  - `minecraft:axolotl`
  - `minecraft:boat`

---

## バージョン情報

- **Minecraft:** 1.21.10
- **NeoForge:** 21.10.43-beta
- **作成日:** 2025-01-05
- **最終更新:** 2025-11-08
  - **ルートテーブル関連の重要な変更を追加:**
    - **カスタムエンティティのルートテーブルで`type_specific`は使えない**
      - バニラのエンティティタイプのみサポート
      - カスタムエンティティではNBTベースの条件を使用
      - 複数アイテムのドロップ方法
  - **BlockEntity関連の重要な変更を追加:**
    - **BlockEntityのクライアント同期には`getUpdateTag()`が必須**
      - `getUpdatePacket()`だけでは不十分
      - ItemStackのCODECを使った正しいエンコード方法
      - 同期の流れとデバッグ方法
    - `BlockBehaviour.Properties#setId()` が必須になった（NeoForge 1.21+）
    - "Trying to access unbound value" エラーの原因と解決方法
    - `BlockItem` と `BlockEntityType` の正しい登録方法
    - `registerBlock()` と `registerSimpleBlockItem()` ヘルパーメソッドの紹介
  - **レシピ関連の重要な変更を追加:**
    - レシピディレクトリ名の変更（recipes → recipe）
    - **レシピのIngredient記法の変更（NeoForge 1.21.2+）**
      - バニラアイテム/タグ: 文字列形式に統一
      - カスタムIngredient: オブジェクト形式（`neoforge:ingredient_type`必須）
      - バージョン別対応表を追加
  - ログファイルとクラッシュレポートの場所を追加
  - 開発用Modの追加方法を追加
  - RenderState システムの詳細な説明を追加（2025-11-06）
  - RenderLayer の変更（render → submit）を追加（2025-11-06）
  - CompoundTag.getBoolean() の Optional 対応を追加（2025-11-06）

---

## 参考リンク

- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)
- [Minecraft Wiki](https://minecraft.wiki/)
