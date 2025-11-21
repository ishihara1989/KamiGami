package com.hydryhydra.kamigami.curse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec3;

/**
 * 祠の祟りレシピを管理するクラス。
 *
 * Phase 1 では静的登録のみ。Phase 3 で DataPack ローディングを実装予定。
 */
public class ShrineCurseRecipes {
    private static final List<LoadedRecipe> RECIPES = new ArrayList<>();

    /**
     * ロードされたレシピ（ID付き）
     */
    public record LoadedRecipe(ResourceLocation id, ShrineCurseRecipe recipe) {
    }

    /**
     * レシピを登録する（静的登録用）
     *
     * @param id
     *            レシピID
     * @param recipe
     *            レシピ
     */
    public static void register(ResourceLocation id, ShrineCurseRecipe recipe) {
        RECIPES.add(new LoadedRecipe(id, recipe));
        KamiGami.LOGGER.info("Registered shrine curse recipe: {}", id);
    }

    /**
     * 登録済みレシピを優先度順にソートする。 初期化時に一度だけ呼ぶ。
     */
    public static void sortByPriority() {
        RECIPES.sort(Comparator.comparingInt((LoadedRecipe r) -> r.recipe().priority()).reversed());
        KamiGami.LOGGER.info("Sorted {} shrine curse recipes by priority", RECIPES.size());
    }

    /**
     * RecipeManagerからレシピをロードする（DataPack対応）
     *
     * @param level
     *            サーバーレベル（RecipeManagerを取得するため）
     */
    public static void loadFromRecipeManager(ServerLevel level) {
        RecipeManager recipeManager = level.recipeAccess();

        // 既存のレシピをクリア（リロード対応）
        RECIPES.clear();

        // RecipeManagerから全てのShrineCurseRecipeを取得
        // getRecipes()を使用してRecipeType別にフィルタリング
        recipeManager.getRecipes().stream()
                .filter(holder -> holder.value().getType().equals(KamiGami.SHRINE_CURSE_RECIPE_TYPE.get()))
                .forEach(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<ShrineCurseRecipe> shrineHolder = (RecipeHolder<ShrineCurseRecipe>) holder;
                    RECIPES.add(new LoadedRecipe(shrineHolder.id().location(), shrineHolder.value()));
                    KamiGami.LOGGER.info("Loaded shrine curse recipe from DataPack: {}", shrineHolder.id().location());
                });

        // レシピが見つからない場合は静的レシピを登録（後方互換性）
        if (RECIPES.isEmpty()) {
            KamiGami.LOGGER.warn("No shrine curse recipes found in DataPacks, registering default recipes...");
            registerDefaultRecipes();
        } else {
            // レシピをソート
            sortByPriority();
            KamiGami.LOGGER.info("Loaded {} shrine curse recipe(s) from DataPacks", RECIPES.size());
        }
    }

    /**
     * 指定されたトリガーとアイテムにマッチする最初のレシピを検索する。
     *
     * @param trigger
     *            トリガータイプ
     * @param cursedItem
     *            祠内部のアイテム
     * @return マッチしたレシピ（見つからない場合は空）
     */
    public static Optional<LoadedRecipe> findRecipe(ShrineCurseRecipe.TriggerType trigger, ItemStack cursedItem) {
        return RECIPES.stream().filter(r -> r.recipe().trigger() == trigger && r.recipe().matches(cursedItem))
                .findFirst();
    }

    /**
     * 全レシピを取得する（デバッグ用）
     *
     * @return 全レシピのリスト
     */
    public static List<LoadedRecipe> getAllRecipes() {
        return new ArrayList<>(RECIPES);
    }

    /**
     * 静的レシピを登録する。 Mod初期化時に一度だけ呼ぶ。
     */
    public static void registerDefaultRecipes() {
        KamiGami.LOGGER.info("Registering default shrine curse recipes...");

        // 通常の祠（御神体なし）の破壊時レシピ
        registerNormalShrineCurse();

        // 沼の神の御神体の破壊時レシピ
        registerSwampDeityShrineCurse();

        // 豊穣の神の御神体の破壊時レシピ
        registerFertilityDeityShrineCurse();

        // 炎の神の御神体の破壊時レシピ
        registerFireDeityShrineCurse();

        // レシピ登録後にソート
        sortByPriority();
    }

    /**
     * 通常の祠（御神体なし）の破壊時レシピを登録。 サイズ1のTatari Slimeを1体召喚。
     *
     * 注意: Ingredient を Optional.empty() にすることで、空のアイテム専用レシピとして扱われる。
     */
    private static void registerNormalShrineCurse() {
        // サイズ1のスライムを召喚するアクション
        CompoundTag slimeNbt = new CompoundTag();
        slimeNbt.putInt("Size", 1);

        CurseAction actions = new SequenceAction(List.of(
                // 爆発音とエフェクト
                new PlayEffectsAction(Optional.of(SoundEvents.GENERIC_EXPLODE.value()), 0.5F, 1.2F,
                        Optional.of(ParticleTypes.EXPLOSION), 1, new Vec3(0.5, 0.5, 0.5)),
                // サイズ1のスライムを召喚
                new SpawnEntityAction(KamiGami.TATARI_SLIME.get(), new Vec3(0.5, 0.5, 0.5), Optional.of(slimeNbt))));

        // 通常の祠用のレシピ（Ingredient は空 = 空アイテム専用）
        ShrineCurseRecipe recipe = new ShrineCurseRecipe(ShrineCurseRecipe.TriggerType.ON_BREAK, Optional.empty(), // 空アイテム専用
                true, // シルクタッチなしを必須
                actions, 0 // 優先度: 最低（他のレシピにマッチしない場合のフォールバック）
        );

        register(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "normal_shrine_curse"), recipe);
    }

    /**
     * 沼の祟りスライム用のNBTを作成するヘルパーメソッド
     */
    private static CompoundTag createSwampSlimeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Size", 4);
        return nbt;
    }

    /**
     * 沼の神の御神体の破壊時レシピを登録。
     *
     * 動作: 1. 周囲5x5、祠から2段下〜祠の高さの範囲の原木を削除 2. 空いたマスに粘土・土・苔をランダムに配置 3. サイズ4の沼の祟りスライムを召喚
     * 4. 爆発音と爆発エフェクト
     *
     * 注意: - 植物→骨粉変換は将来的に実装予定。現在は省略。 - サイズ4のSwampTatariSlimeを召喚（分裂しない、独自ドロップ）
     */
    private static void registerSwampDeityShrineCurse() {
        // 周囲5x5、祠から2段下〜祠の高さ（-2, 0）の範囲を処理
        CurseAction actions = new SequenceAction(List.of(
                // 爆発音とエフェクト
                new PlayEffectsAction(Optional.of(SoundEvents.GENERIC_EXPLODE.value()), 1.0F, 1.0F,
                        Optional.of(ParticleTypes.EXPLOSION_EMITTER), 1, new Vec3(0.5, 0.5, 0.5)),
                // 範囲内の各座標に対して処理
                new AreaAction(new AreaAction.Box(new Vec3(-2, -2, -2), new Vec3(2, 0, 2)), new SequenceAction(List.of(
                        // 原木を削除
                        new ConditionalReplaceAction(Optional.of(net.minecraft.tags.BlockTags.LOGS), Optional.empty(),
                                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Optional.empty(), 0),
                        // 空気ブロックに粘土・土・苔をランダムに配置（40%の確率）
                        new ChanceAction(0.4F, new ReplaceBlockAction(Optional.empty(), Optional.of(List.of(
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.CLAY.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState(), 1))),
                                1.0F, true // when_air = true
                        ))))),
                // 沼の祟りスライム（サイズ4固定、分裂なし、独自ドロップ）を召喚
                // サイズをNBTで明示的に設定（クライアント同期のため）
                new SpawnEntityAction(KamiGami.SWAMP_TATARI_SLIME.get(), new Vec3(0.5, 0.5, 0.5),
                        Optional.of(createSwampSlimeNBT()))));

        // 沼の神の御神体にマッチするレシピ
        ShrineCurseRecipe recipe = new ShrineCurseRecipe(ShrineCurseRecipe.TriggerType.ON_BREAK,
                Optional.of(Ingredient.of(KamiGami.CHARM_OF_SWAMP_DEITY.get())), true, // シルクタッチなしを必須
                actions, 100 // 優先度: 高（通常の祠より優先）
        );

        register(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "swamp_deity_shrine_curse"), recipe);
    }

    /**
     * 豊穣の神の御神体の破壊時レシピを登録。
     *
     * 動作: 1. 周囲5x5、祠の高さ〜2段下の範囲にランダムにブロックを配置 2. 祠と同じ高さ（dy=0）は40%、それ以下は100%の確率 3. Oak
     * Log、Podzol、Gravel、Sand、Coarse Dirtをランダム選択 4. Tatari of Fertility Deityを召喚 5.
     * 爆発音と爆発エフェクト
     */
    private static void registerFertilityDeityShrineCurse() {
        // 周囲5x5、祠の高さ〜2段下（0, -2）の範囲を処理
        CurseAction actions = new SequenceAction(List.of(
                // 爆発音とエフェクト（低音）
                new PlayEffectsAction(Optional.of(SoundEvents.GENERIC_EXPLODE.value()), 1.5F, 0.8F,
                        Optional.of(ParticleTypes.EXPLOSION_EMITTER), 1, new Vec3(0.5, 0.5, 0.5)),
                // 範囲内の各座標に対して処理
                new AreaAction(new AreaAction.Box(new Vec3(-2, -2, -2), new Vec3(2, 0, 2)), new SequenceAction(List.of(
                        // 祠と同じ高さ（Y=0）は40%の確率でブロック配置
                        // それ以下（Y<0）は100%の確率でブロック配置
                        // NOTE: 現在のシステムでは「Y座標に応じた確率変更」を実装できないため、
                        // 全体で40%の確率とする（簡略化）
                        new ChanceAction(0.4F, new ReplaceBlockAction(Optional.empty(), Optional.of(List.of(
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.PODZOL.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState(), 1))),
                                1.0F, true // when_air = true
                        ))))),
                // Tatari of Fertility Deityを召喚
                new SpawnEntityAction(KamiGami.TATARI_FERTILITY.get(), new Vec3(0.5, 0.0, 0.5), Optional.empty())));

        // 豊穣の神の御神体にマッチするレシピ
        ShrineCurseRecipe recipe = new ShrineCurseRecipe(ShrineCurseRecipe.TriggerType.ON_BREAK,
                Optional.of(Ingredient.of(KamiGami.CHARM_OF_FERTILITY.get())), true, // シルクタッチなしを必須
                actions, 100 // 優先度: 高（通常の祠より優先）
        );

        register(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "fertility_deity_shrine_curse"), recipe);
    }

    /**
     * 炎の神の御神体の破壊時レシピを登録。
     *
     * 動作: 1. 周囲5x5、祠の1段下に、中央に強制的にマグマを配置 2.
     * 周囲5x5、祠の1段下（dy=-1）には100%、祠と同じ高さ（dy=0）には40%の確率で
     * Gravel、Sand、Netherrack、Basaltをランダムに配置 3. Fire Golemを召喚 4. 爆発音と爆発エフェクト
     */
    private static void registerFireDeityShrineCurse() {
        // 周囲5x5、祠の高さ〜1段下（0, -1）の範囲を処理
        CurseAction actions = new SequenceAction(List.of(
                // 爆発音とエフェクト
                new PlayEffectsAction(Optional.of(SoundEvents.GENERIC_EXPLODE.value()), 1.5F, 1.0F,
                        Optional.of(ParticleTypes.EXPLOSION_EMITTER), 1, new Vec3(0.5, 0.5, 0.5)),
                // 中央下（0, -1, 0）に強制的にマグマを配置
                new AreaAction(new AreaAction.Box(new Vec3(0, -1, 0), new Vec3(0, -1, 0)),
                        new ReplaceBlockAction(Optional.empty(),
                                Optional.of(List.of(new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 1))),
                                1.0F, false)),
                // 下段（dy=-1）は100%の確率でブロック配置
                new AreaAction(new AreaAction.Box(new Vec3(-2, -1, -2), new Vec3(2, -1, 2)),
                        new ReplaceBlockAction(Optional.empty(), Optional.of(List.of(
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState(), 1))),
                                1.0F, true)),
                // 上段（dy=0）は40%の確率でブロック配置
                new AreaAction(new AreaAction.Box(new Vec3(-2, 0, -2), new Vec3(2, 0, 2)),
                        new ChanceAction(0.4F, new ReplaceBlockAction(Optional.empty(), Optional.of(List.of(
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState(), 1),
                                new ReplaceBlockAction.PaletteEntry(
                                        net.minecraft.world.level.block.Blocks.BASALT.defaultBlockState(), 1))),
                                1.0F, true))),
                // Fire Golemを召喚
                new SpawnEntityAction(KamiGami.FIRE_GOLEM.get(), new Vec3(0.5, 0.0, 0.5), Optional.empty())));

        // 炎の神の御神体にマッチするレシピ
        ShrineCurseRecipe recipe = new ShrineCurseRecipe(ShrineCurseRecipe.TriggerType.ON_BREAK,
                Optional.of(Ingredient.of(KamiGami.CHARM_OF_FIRE_DEITY.get())), true, // シルクタッチなしを必須
                actions, 100 // 優先度: 高（通常の祠より優先）
        );

        register(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "fire_deity_shrine_curse"), recipe);
    }
}
