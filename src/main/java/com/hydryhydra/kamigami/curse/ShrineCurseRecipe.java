package com.hydryhydra.kamigami.curse;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 祠の破壊時に実行される祟りレシピ。
 *
 * レシピは以下の要素で構成される: - trigger: トリガータイプ ("on_break", "on_insert", "on_tick" など) -
 * ingredient: 祠内部のアイテム（Optional<Ingredient> で一致判定、空の場合は空アイテムにのみマッチ） -
 * requireNoSilkTouch: シルクタッチなしを必須とするか（破壊時のみ） - actions: 実行するアクションのリスト -
 * priority: 複数レシピがマッチした場合の優先度（高い方が優先）
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "kamigami:shrine_curse",
 *   "trigger": "on_break",
 *   "ingredient": {"item": "kamigami:charm_of_swamp_deity"},
 *   "require_no_silk_touch": true,
 *   "priority": 100,
 *   "actions": [
 *     {
 *       "type": "sequence",
 *       "steps": [...]
 *     }
 *   ]
 * }
 * </pre>
 */
public record ShrineCurseRecipe(TriggerType trigger, Optional<Ingredient> ingredient, boolean requireNoSilkTouch,
        CurseAction actions, int priority) implements Recipe<RecipeInput> {

    /**
     * トリガータイプ
     */
    public enum TriggerType {
        /** 祠が破壊された時 */
        ON_BREAK,
        /** アイテムが挿入された時 */
        ON_INSERT,
        /** 定期的にTick */
        ON_TICK;

        public static final Codec<TriggerType> CODEC = Codec.STRING.xmap(s -> {
            return switch (s.toLowerCase()) {
                case "on_break" -> ON_BREAK;
                case "on_insert" -> ON_INSERT;
                case "on_tick" -> ON_TICK;
                default -> throw new IllegalArgumentException("Unknown trigger type: " + s);
            };
        }, trigger -> trigger.name().toLowerCase());
    }

    public static final MapCodec<ShrineCurseRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(TriggerType.CODEC.fieldOf("trigger").forGetter(ShrineCurseRecipe::trigger),
                    Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(ShrineCurseRecipe::ingredient),
                    Codec.BOOL.optionalFieldOf("require_no_silk_touch", false)
                            .forGetter(ShrineCurseRecipe::requireNoSilkTouch),
                    CurseActions.ACTION_CODEC.fieldOf("actions").forGetter(ShrineCurseRecipe::actions),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(ShrineCurseRecipe::priority))
            .apply(instance, ShrineCurseRecipe::new));

    /**
     * このレシピが指定されたアイテムにマッチするか判定する。
     *
     * @param stack
     *            祠内部のアイテム
     * @return マッチする場合は true
     */
    public boolean matches(ItemStack stack) {
        if (ingredient.isEmpty()) {
            // ingredient が空の場合 = 空アイテム専用レシピ
            return stack.isEmpty();
        } else {
            // ingredient が指定されている場合 = 指定アイテムのみマッチ
            return !stack.isEmpty() && ingredient.get().test(stack);
        }
    }

    // Recipe interface implementation (required for DataPack loading)

    @Override
    public boolean matches(RecipeInput input, Level level) {
        // This recipe type doesn't use the standard recipe matching system
        // Matching is done via matches(ItemStack) method
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        // This recipe type doesn't produce items
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ShrineCurseRecipe> getSerializer() {
        return KamiGami.SHRINE_CURSE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<ShrineCurseRecipe> getType() {
        return KamiGami.SHRINE_CURSE_RECIPE_TYPE.get();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // Not shown in recipe book (using CRAFTING_MISC as placeholder)
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        // Not a crafting recipe
        return PlacementInfo.NOT_PLACEABLE;
    }
}
