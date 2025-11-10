package com.hydryhydra.kamigami.offering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;

/**
 * 祠のお供え物レシピを管理するクラス。
 *
 * Phase 1 では静的登録のみ。Phase 3 で DataPack ローディングを実装予定。
 */
public class ShrineOfferingRecipes {
    private static final List<LoadedRecipe> RECIPES = new ArrayList<>();

    /**
     * ロードされたレシピ（ID付き）
     */
    public record LoadedRecipe(ResourceLocation id, ShrineOfferingRecipe recipe) {
    }

    /**
     * レシピを登録する（静的登録用）
     *
     * @param id
     *            レシピID
     * @param recipe
     *            レシピ
     */
    public static void register(ResourceLocation id, ShrineOfferingRecipe recipe) {
        RECIPES.add(new LoadedRecipe(id, recipe));
        KamiGami.LOGGER.info("Registered shrine offering recipe: {}", id);
    }

    /**
     * 登録済みレシピを優先度順にソートする。 初期化時に一度だけ呼ぶ。
     */
    public static void sortByPriority() {
        RECIPES.sort(Comparator.comparingInt((LoadedRecipe r) -> r.recipe().priority()).reversed());
        KamiGami.LOGGER.info("Sorted {} shrine offering recipes by priority", RECIPES.size());
    }

    /**
     * 指定されたトリガーとアイテムにマッチする最初のレシピを検索する。
     *
     * @param trigger
     *            トリガータイプ
     * @param offeredItem
     *            祠内部のアイテム
     * @return マッチしたレシピ（見つからない場合は空）
     */
    public static Optional<LoadedRecipe> findRecipe(ShrineOfferingRecipe.TriggerType trigger, ItemStack offeredItem) {
        return RECIPES.stream().filter(r -> r.recipe().trigger() == trigger && r.recipe().matches(offeredItem))
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
        KamiGami.LOGGER.info("Registering default shrine offering recipes...");

        // 通常の祠（御神体なし）の破壊時レシピ
        // 空のアイテムにマッチ（Ingredient.EMPTY は存在しないため、何とも一致しない条件を使う）
        // ※この条件は ShrineBlock 側で「アイテムが空の場合」として明示的にチェックする
        registerNormalShrineCurse();

        // レシピ登録後にソート
        sortByPriority();
    }

    /**
     * 通常の祠（御神体なし）の破壊時レシピを登録。 サイズ1のTatari Slimeを1体召喚。
     *
     * 注意: このレシピは ShrineBlock 側で空のアイテムの場合に直接実行される。 Ingredient
     * には使われないダミー値（BARRIER）を設定している。
     */
    private static void registerNormalShrineCurse() {
        // サイズ1のスライムを召喚するアクション
        CompoundTag slimeNbt = new CompoundTag();
        slimeNbt.putInt("Size", 1);

        OfferingAction actions = new SequenceAction(List.of(
                // 爆発音とエフェクト
                new PlayEffectsAction(Optional.of(SoundEvents.GENERIC_EXPLODE.value()), 0.5F, 1.2F,
                        Optional.of(ParticleTypes.EXPLOSION), 1, new Vec3(0.5, 0.5, 0.5)),
                // サイズ1のスライムを召喚
                new SpawnEntityAction(KamiGami.TATARI_SLIME.get(), new Vec3(0.5, 0.5, 0.5), Optional.of(slimeNbt))));

        // 通常の祠用のレシピ（Ingredient はダミー値）
        // NOTE: Ingredient.of() は空を許可しないため、実際には使われない BARRIER を指定
        // ShrineBlock 側で空のアイテムの場合は findRecipe() を呼ばずに直接このレシピを実行する
        ShrineOfferingRecipe recipe = new ShrineOfferingRecipe(ShrineOfferingRecipe.TriggerType.ON_BREAK,
                Ingredient.of(net.minecraft.world.item.Items.BARRIER), // ダミー値（使用されない）
                true, // シルクタッチなしを必須
                actions, 0 // 優先度: 最低（他のレシピにマッチしない場合のフォールバック）
        );

        register(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "normal_shrine_curse"), recipe);
    }

    /**
     * 通常の祠（御神体なし）用のレシピを取得する。 ShrineBlock から直接呼び出すためのヘルパーメソッド。
     *
     * @return 通常の祠用のレシピ（存在しない場合は空）
     */
    public static Optional<LoadedRecipe> getNormalShrineCurseRecipe() {
        return RECIPES.stream().filter(
                r -> r.id().equals(ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "normal_shrine_curse")))
                .findFirst();
    }
}
