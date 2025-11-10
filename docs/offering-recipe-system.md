# 祠のお供え物レシピシステム

## 概要

祠（Shrine）の破壊時やアイテム挿入時に実行される処理を、データ駆動型のレシピシステムとして実装しました。このシステムは、NeoForge 1.21.10の`Codec`を使用した柔軟で拡張可能なアクションシステムに基づいています。

## アーキテクチャ

### コアコンポーネント

#### 1. `OfferingAction` インターフェース
すべてのアクションが実装する基本インターフェース。

```java
public interface OfferingAction {
    boolean perform(ActionContext ctx);
    MapCodec<? extends OfferingAction> codec();
}
```

#### 2. `ActionContext` レコード
アクション実行時のコンテキスト情報を保持。

```java
public record ActionContext(
    ServerLevel level,
    BlockPos origin,
    @Nullable Player player,
    ItemStack offeredItem,
    RandomSource random
) {}
```

#### 3. `ShrineOfferingRecipe` レコード
レシピの定義。

```java
public record ShrineOfferingRecipe(
    TriggerType trigger,           // ON_BREAK, ON_INSERT, ON_TICK
    Ingredient ingredient,          // マッチするアイテム
    boolean requireNoSilkTouch,     // シルクタッチなしを必須とするか
    OfferingAction actions,         // 実行するアクション
    int priority                    // 優先度（高い方が優先）
) {}
```

### レシピ管理

#### `ShrineOfferingRecipes` クラス
レシピの登録・検索を管理。

- `register(ResourceLocation, ShrineOfferingRecipe)` - レシピ登録
- `findRecipe(TriggerType, ItemStack)` - レシピ検索
- `getNormalShrineCurseRecipe()` - 通常の祠用のレシピ取得（空のアイテム対応）
- `sortByPriority()` - 優先度順にソート

## 実装されたアクション

### 1. `SequenceAction` - 複数アクションの順次実行
```java
new SequenceAction(List.of(
    action1,
    action2,
    action3
))
```

### 2. `SpawnEntityAction` - エンティティ召喚
```java
CompoundTag nbt = new CompoundTag();
nbt.putInt("Size", 4);

new SpawnEntityAction(
    KamiGami.TATARI_SLIME.get(),
    new Vec3(0.5, 0.5, 0.5),  // オフセット
    Optional.of(nbt)           // NBTデータ（Slime Sizeサポート）
)
```

**注意**: Phase 1では`Slime`の`Size`フィールドのみNBTサポート。将来的に汎用NBT適用を実装予定。

### 3. `PlayEffectsAction` - サウンド・パーティクル
```java
new PlayEffectsAction(
    Optional.of(SoundEvents.GENERIC_EXPLODE.value()),
    1.0F,  // 音量
    1.0F,  // ピッチ
    Optional.of(ParticleTypes.EXPLOSION_EMITTER),
    1,     // パーティクル数
    new Vec3(0.5, 0.5, 0.5)  // オフセット
)
```

### 4. `ReplaceBlockAction` - ブロック置換
```java
// 単一ブロック置換
new ReplaceBlockAction(
    Optional.of(Blocks.CLAY.defaultBlockState()),
    Optional.empty(),  // パレットなし
    1.0F,              // 確率100%
    true               // when_air=true（空気ブロックのみ）
)

// パレットからランダム選択
new ReplaceBlockAction(
    Optional.empty(),
    Optional.of(List.of(
        new ReplaceBlockAction.PaletteEntry(Blocks.CLAY.defaultBlockState(), 3),
        new ReplaceBlockAction.PaletteEntry(Blocks.DIRT.defaultBlockState(), 2),
        new ReplaceBlockAction.PaletteEntry(Blocks.MOSS_BLOCK.defaultBlockState(), 1)
    )),
    1.0F,
    true
)
```

### 5. `AreaAction` - 範囲内反復処理
```java
new AreaAction(
    new AreaAction.Box(
        new Vec3(-2, -2, -2),  // 最小座標（相対）
        new Vec3(2, 0, 2)      // 最大座標（相対）
    ),
    perPositionAction  // 各座標で実行するアクション
)
```

**特徴**: 座標ごとに決定論的な乱数生成器を作成し、マルチプレイでの同期を保証。

```java
long worldSeed = ctx.level().getSeed();
long posSeed = Mth.getSeed(pos);
long combinedSeed = worldSeed ^ posSeed ^ 0x9E3779B97F4A7C15L;
RandomSource posRandom = RandomSource.create(combinedSeed);
```

### 6. `ChanceAction` - 確率実行
```java
new ChanceAction(
    0.4F,     // 40%の確率
    action    // 実行するアクション
)
```

### 7. `DropItemAction` - アイテムドロップ
```java
new DropItemAction(
    Items.BONE_MEAL,
    1  // ドロップ数
)
```

### 8. `ConditionalReplaceAction` - 条件付きブロック置換
```java
// タグベースのマッチング
new ConditionalReplaceAction(
    Optional.of(BlockTags.LOGS),           // マッチするタグ
    Optional.empty(),                      // 特定のBlockStateマッチング（オプション）
    Blocks.AIR.defaultBlockState(),        // 置換先
    Optional.empty(),                      // ドロップアイテム（オプション）
    0                                      // ドロップ数
)

// 植物を骨粉に変換（将来実装予定）
new ConditionalReplaceAction(
    Optional.of(BlockTags.FLOWERS),
    Optional.empty(),
    Blocks.AIR.defaultBlockState(),
    Optional.of(Items.BONE_MEAL),
    1
)
```

## 実装済みレシピ

### 1. 通常の祠（御神体なし）
**ID**: `kamigami:normal_shrine_curse`
**優先度**: 0（最低）

```java
new SequenceAction(List.of(
    new PlayEffectsAction(/* 小さな爆発音・パーティクル */),
    new SpawnEntityAction(/* サイズ1のTatari Slime */)
))
```

**特殊処理**: 空のアイテムにマッチさせるため、`ShrineBlock`側で`getNormalShrineCurseRecipe()`を直接呼び出す。

### 2. 沼の神の御神体
**ID**: `kamigami:swamp_deity_shrine_curse`
**優先度**: 100（高）

```java
new SequenceAction(List.of(
    new PlayEffectsAction(/* 大きな爆発音・パーティクル */),
    new AreaAction(
        new Box(new Vec3(-2, -2, -2), new Vec3(2, 0, 2)),
        new SequenceAction(List.of(
            new ConditionalReplaceAction(/* 原木を削除 */),
            new ChanceAction(0.4F,
                new ReplaceBlockAction(/* 粘土・土・苔をランダム配置 */)
            )
        ))
    ),
    new SpawnEntityAction(/* サイズ4のTatari Slime */)
))
```

**簡略化**: 植物→骨粉変換は将来実装予定。

### 3. 豊穣の神の御神体
**ID**: `kamigami:fertility_deity_shrine_curse`
**優先度**: 100（高）

```java
new SequenceAction(List.of(
    new PlayEffectsAction(/* 低音の爆発音・パーティクル */),
    new AreaAction(
        new Box(new Vec3(-2, -2, -2), new Vec3(2, 0, 2)),
        new ChanceAction(0.4F,
            new ReplaceBlockAction(/* Oak Log、Podzol等をランダム配置 */)
        )
    ),
    new SpawnEntityAction(/* Tatari of Fertility Deity */)
))
```

**簡略化**: Y座標に応じた確率変更（dy=0で40%、dy<0で100%）は、全体で40%として簡略化。

## ShrineBlockとの統合

### `playerWillDestroy()` メソッドの変更
```java
if (willCurse && level instanceof ServerLevel serverLevel) {
    // すべての祠タイプでレシピシステムを使用
    executeRecipeOrFallback(serverLevel, pos, player, storedItem);
}
```

### `executeRecipeOrFallback()` メソッド
```java
private void executeRecipeOrFallback(ServerLevel level, BlockPos pos,
                                    Player player, ItemStack storedItem) {
    // 空のアイテムの場合は直接レシピ取得
    var recipeOpt = storedItem.isEmpty()
        ? ShrineOfferingRecipes.getNormalShrineCurseRecipe()
        : ShrineOfferingRecipes.findRecipe(TriggerType.ON_BREAK, storedItem);

    if (recipeOpt.isPresent()) {
        var recipe = recipeOpt.get().recipe();
        RandomSource random = level.getRandom();
        ActionContext ctx = new ActionContext(level, pos, player, storedItem, random);
        recipe.actions().perform(ctx);
    } else {
        // フォールバック（現在は削除済み）
    }
}
```

## NeoForge 1.21.10の新機能活用

### 1. Codecベースのシリアライゼーション
`MapCodec`と`RecordCodecBuilder`を使用した型安全なシリアライゼーション。

```java
public static final MapCodec<SpawnEntityAction> CODEC =
    RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                .fieldOf("entity").forGetter(SpawnEntityAction::entityType),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO)
                .forGetter(SpawnEntityAction::offset),
            CompoundTag.CODEC.optionalFieldOf("nbt")
                .forGetter(SpawnEntityAction::nbt)
        )
        .apply(instance, SpawnEntityAction::new));
```

### 2. 多相ディスパッチ
`ResourceLocation.CODEC.dispatch()`を使用した型ベースのディスパッチ。

```java
public static final Codec<OfferingAction> ACTION_CODEC =
    ResourceLocation.CODEC.dispatch("type",
        action -> getTypeId(action.codec()),
        OfferingActions::getCodec);
```

### 3. タグベースのブロックマッチング
`TagKey<Block>`を使用した柔軟なブロックマッチング。

```java
TagKey.codec(Registries.BLOCK).optionalFieldOf("match_tag")
```

## 将来の拡張予定

### Phase 3: DataPackローディング
現在は静的登録のみ。将来的にJSONファイルからのレシピロード機能を実装予定。

**JSON例**:
```json
{
  "type": "kamigami:shrine_offering",
  "trigger": "on_break",
  "ingredient": {"item": "kamigami:charm_of_swamp_deity"},
  "require_no_silk_touch": true,
  "priority": 100,
  "actions": {
    "type": "kamigami:sequence",
    "steps": [
      {
        "type": "kamigami:play_effects",
        "sound": "minecraft:entity.generic.explode",
        "sound_volume": 1.0,
        "sound_pitch": 1.0,
        "particle": "minecraft:explosion_emitter",
        "particle_count": 1
      },
      {
        "type": "kamigami:spawn_entity",
        "entity": "kamigami:tatari_slime",
        "offset": {"x": 0.5, "y": 0.5, "z": 0.5},
        "nbt": {"Size": 4}
      }
    ]
  }
}
```

### 新しいアクションタイプ
- **条件分岐アクション**: Y座標や時間に基づく条件分岐
- **ループアクション**: 指定回数の繰り返し実行
- **遅延アクション**: Tick遅延後の実行
- **汎用NBTサポート**: Slime以外のエンティティへのNBT適用
- **高度な植物判定**: `isPlant()`メソッドに相当する柔軟な条件

### ON_INSERTトリガー
アイテム挿入時の処理（お供え物システム）。

### ON_TICKトリガー
定期的な処理（継続的な効果）。

## トラブルシューティング

### Ingredient.of()が空を許可しない
**問題**: `Ingredient.of()`に空の配列を渡すと`UnsupportedOperationException`が発生。

**解決策**: ダミー値（`Items.BARRIER`）を使用し、`ShrineBlock`側で空のアイテムを明示的にチェック。

```java
// レシピ定義側
Ingredient.of(Items.BARRIER)  // ダミー値

// ShrineBlock側
var recipeOpt = storedItem.isEmpty()
    ? ShrineOfferingRecipes.getNormalShrineCurseRecipe()
    : ShrineOfferingRecipes.findRecipe(TriggerType.ON_BREAK, storedItem);
```

### Mth.getSeed()の非推奨警告
**問題**: `Mth.getSeed(Vec3i)`がNeoForge 1.21.10で非推奨。

**現状**: 警告のみで動作に問題なし。将来的に代替APIへの移行を検討。

### 決定論的乱数の重要性
**問題**: マルチプレイでクライアント・サーバー間の同期が必要。

**解決策**: `AreaAction`で座標ベースの決定論的シードを生成。

```java
long combinedSeed = worldSeed ^ posSeed ^ 0x9E3779B97F4A7C15L;
```

## 参考資料

- [NeoForge Documentation](https://docs.neoforged.net/)
- [Mojang Serialization (Codec)](https://github.com/Mojang/DataFixerUpper)
- [docs/neoforge-gotchas.md](neoforge-gotchas.md) - NeoForge 1.21.10の注意点

---

**最終更新日**: 2025-11-10
**実装者**: Claude Agent
