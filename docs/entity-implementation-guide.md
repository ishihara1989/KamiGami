# NeoForge 1.21.10 エンティティ実装ガイド

カスタムエンティティを実装する際の完全な手順書です。

## 目次
1. [実装の全体フロー](#実装の全体フロー)
2. [ステップ1: エンティティクラスの作成](#ステップ1-エンティティクラスの作成)
3. [ステップ2: エンティティの登録](#ステップ2-エンティティの登録)
4. [ステップ3: クライアント側のレンダラー](#ステップ3-クライアント側のレンダラー)
5. [ステップ4: リソースファイル](#ステップ4-リソースファイル)
6. [チェックリスト](#チェックリスト)

---

## 実装の全体フロー

```
1. エンティティクラス作成
   ↓
2. メインクラスでエンティティタイプ登録
   ↓
3. エンティティ属性の登録
   ↓
4. クライアント側レンダラー作成
   ↓
5. レンダラー登録
   ↓
6. テクスチャ・翻訳ファイル追加
   ↓
7. （オプション）ルートテーブル追加
```

---

## ステップ1: エンティティクラスの作成

### 基本的なMobエンティティ

```java
package com.example.mymod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class MyCustomEntity extends Animal {

    public MyCustomEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    // AIゴールの登録（行動パターン）
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    // 属性の設定（HP、移動速度など）
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    // 繁殖の実装（必須）
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        // 繁殖させない場合
        return null;

        // 繁殖させる場合
        // return ModEntities.MY_CUSTOM_ENTITY.get().create(level, EntitySpawnReason.BREEDING);
    }

    // 餌の設定（Animalの場合必須）
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    // サウンドの設定（オプション）
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;  // バニラサウンドを使用
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.COW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }
}
```

### よく使うAIゴール

```java
// 基本動作
new FloatGoal(this);                          // 泳ぐ
new PanicGoal(this, 2.0D);                    // ダメージを受けたら逃げる
new WaterAvoidingRandomStrollGoal(this, 1.0D); // ランダムに歩き回る
new RandomLookAroundGoal(this);               // ランダムに見回す

// プレイヤーとの相互作用
new LookAtPlayerGoal(this, Player.class, 6.0F);  // プレイヤーを見る
new FollowParentGoal(this, 1.25D);               // 親について行く（子供のみ）

// 誘惑
new TemptGoal(this, 1.25D,
    stack -> stack.is(Items.WHEAT), false);      // 小麦で誘惑される

// 繁殖
new BreedGoal(this, 1.0D);                       // 繁殖行動
```

---

## ステップ2: エンティティの登録

### メインクラスでの登録

```java
// MyMod.java
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class MyMod {
    public static final String MODID = "mymod";

    // 1. DeferredRegisterの作成
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    // 2. エンティティタイプの登録
    public static final DeferredHolder<EntityType<?>, EntityType<MyCustomEntity>> MY_CUSTOM_ENTITY =
        ENTITY_TYPES.register("my_custom_entity",
            () -> EntityType.Builder.of(MyCustomEntity::new, MobCategory.CREATURE)
                .sized(0.9F, 1.4F)           // 当たり判定のサイズ（幅, 高さ）
                .clientTrackingRange(10)      // クライアント追跡範囲
                .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "my_custom_entity"))));

    // 3. コンストラクタで登録
    public MyMod(IEventBus modEventBus, ModContainer modContainer) {
        // エンティティタイプを登録
        ENTITY_TYPES.register(modEventBus);

        // 属性登録イベントをリッスン（重要！忘れるとクラッシュ）
        modEventBus.addListener(this::registerEntityAttributes);
    }

    // 4. 属性の登録
    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(MY_CUSTOM_ENTITY.get(), MyCustomEntity.createAttributes().build());
    }
}
```

### サイズの目安

```java
// 一般的なMobのサイズ
.sized(0.9F, 1.4F)   // 牛サイズ
.sized(0.4F, 0.7F)   // 鶏サイズ
.sized(0.6F, 1.8F)   // プレイヤーサイズ
.sized(0.5F, 0.5F)   // 小型（スライム小など）
.sized(2.0F, 3.0F)   // 大型（エンダーマンなど）
```

### MobCategoryの選択

```java
MobCategory.CREATURE     // 平和的な動物（牛、豚など）
MobCategory.MONSTER      // 敵対的なモンスター（ゾンビなど）
MobCategory.AMBIENT      // 環境Mob（コウモリなど）
MobCategory.WATER_CREATURE  // 水生生物（イカなど）
MobCategory.MISC         // その他
```

---

## ステップ3: クライアント側のレンダラー

### 3-1. レンダラークラスの作成

#### バニラモデルを使う場合（簡単）

```java
package com.example.mymod.client.renderer;

import com.example.mymod.MyMod;
import com.example.mymod.entity.MyCustomEntity;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.resources.ResourceLocation;

public class MyCustomEntityRenderer extends MobRenderer<MyCustomEntity, CowRenderState, CowModel> {
    // テクスチャの場所
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(MyMod.MODID,
            "textures/entity/my_custom_entity.png");

    public MyCustomEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.7F);
        // 第3引数は影のサイズ
    }

    @Override
    public CowRenderState createRenderState() {
        return new CowRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(CowRenderState state) {
        return TEXTURE;
    }
}
```

#### カスタムモデルを使う場合（上級）

```java
// まずRenderStateを定義
public class MyCustomRenderState extends LivingEntityRenderState {
    // カスタムレンダリング情報を追加
}

// 次にレンダラーを実装
public class MyCustomEntityRenderer extends MobRenderer<MyCustomEntity, MyCustomRenderState, MyCustomModel> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(MyMod.MODID,
            "textures/entity/my_custom_entity.png");

    public MyCustomEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new MyCustomModel(context.bakeLayer(MY_MODEL_LAYER)), 0.7F);
    }

    @Override
    public MyCustomRenderState createRenderState() {
        return new MyCustomRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(MyCustomRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(MyCustomEntity entity, MyCustomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // エンティティからレンダリング状態を抽出
    }
}
```

### 3-2. レンダラーの登録

```java
package com.example.mymod.client;

import com.example.mymod.MyMod;
import com.example.mymod.client.renderer.MyCustomEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MyMod.MY_CUSTOM_ENTITY.get(), MyCustomEntityRenderer::new);
    }
}
```

**重要な注意点:**
- `@EventBusSubscriber`に`bus`パラメータは不要
- `value = Dist.CLIENT`を必ず指定（クライアント側のみで実行）
- メソッドは`static`にする

---

## ステップ4: リソースファイル

### 4-1. エンティティテクスチャ

**パス:**
```
src/main/resources/assets/mymod/textures/entity/my_custom_entity.png
```

**サイズ:**
- 64x32 ピクセル（標準的なバニラMob）
- 64x64 ピクセル（複雑なモデル）
- カスタムサイズ（カスタムモデルの場合）

### 4-2. 翻訳ファイル

**パス:**
```
src/main/resources/assets/mymod/lang/en_us.json
```

**内容:**
```json
{
  "entity.mymod.my_custom_entity": "My Custom Entity"
}
```

### 4-3. ルートテーブル（オプション）

**パス:**
```
src/main/resources/data/mymod/loot_table/entities/my_custom_entity.json
```

**基本的なドロップ:**
```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:leather"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:killed_by_player"
        }
      ]
    }
  ]
}
```

**経験値ドロップ:**
```json
{
  "type": "minecraft:entity",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:leather"
        }
      ]
    }
  ],
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": {
        "min": 0,
        "max": 2
      }
    },
    {
      "function": "minecraft:looting_enchant",
      "count": {
        "min": 0,
        "max": 1
      }
    }
  ]
}
```

---

## チェックリスト

実装が完了したら、以下を確認してください：

### コード
- [ ] エンティティクラスを作成した
- [ ] `registerGoals()`を実装した
- [ ] `createAttributes()`を実装した
- [ ] `getBreedOffspring()`を実装した（Animalの場合）
- [ ] `isFood()`を実装した（Animalの場合）
- [ ] メインクラスで`ENTITY_TYPES`を作成した
- [ ] エンティティタイプを登録した
- [ ] `ENTITY_TYPES.register(modEventBus)`を呼んだ
- [ ] `EntityAttributeCreationEvent`をリッスンした
- [ ] 属性を登録した
- [ ] レンダラークラスを作成した（`client`パッケージ）
- [ ] `ClientSetup`クラスを作成した
- [ ] `@EventBusSubscriber`アノテーションを付けた
- [ ] レンダラーを登録した

### リソース
- [ ] エンティティテクスチャを追加した
- [ ] 翻訳ファイルを更新した
- [ ] ルートテーブルを追加した（オプション）

### テスト
- [ ] コンパイルが通る
- [ ] ゲームが起動する
- [ ] エンティティがスポーンする
- [ ] テクスチャが表示される
- [ ] AIが正しく動作する
- [ ] ドロップアイテムが正しい

---

## トラブルシューティング

### エンティティがスポーンしない
1. エンティティタイプが正しく登録されているか確認
2. 属性が登録されているか確認（`EntityAttributeCreationEvent`）
3. コマンドで試す: `/summon mymod:my_custom_entity ~ ~ ~`

### テクスチャが表示されない（ピンク＆黒）
1. テクスチャファイルのパスを確認
2. `ResourceLocation`の名前空間とパスを確認
3. ファイル名が正しいか確認（小文字のみ、`_`使用可）

### クラッシュする
1. 属性登録を忘れていないか確認
2. コンソールのエラーメッセージを確認
3. レンダラーがクライアント側のみで登録されているか確認

### AIが動かない
1. `registerGoals()`を実装したか確認
2. ゴールの優先度（第1引数）が適切か確認
3. エンティティの`MobCategory`が適切か確認

---

## 参考資料

- [NeoForge Documentation - Entities](https://docs.neoforged.net/docs/entities/)
- [Minecraft Wiki - Entity](https://minecraft.wiki/w/Entity)
- バニラのエンティティコード（`net.minecraft.world.entity`パッケージ）

---

**作成日:** 2025-01-05
**対象バージョン:** Minecraft 1.21.10 / NeoForge 21.10.43-beta
