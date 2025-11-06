# NeoForge 1.21.10 開発時の注意点

このドキュメントは、NeoForge 1.21.10でのmod開発時に引っかかりやすいポイントをまとめたものです。

## 目次
1. [エンティティ関連](#エンティティ関連)
2. [レンダラー関連](#レンダラー関連)
3. [アイテム関連](#アイテム関連)
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
- `RenderState`型が追加された
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

## アイテム関連

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

## デバッグのヒント

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

---

## バージョン情報

- **Minecraft:** 1.21.10
- **NeoForge:** 21.10.43-beta
- **作成日:** 2025-01-05

---

## 参考リンク

- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)
- [Minecraft Wiki](https://minecraft.wiki/)
