package com.hydryhydra.kamigami.offering;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 祠へのお供え物や祠の破壊時に実行されるレシピ。
 *
 * レシピは以下の要素で構成される:
 * - trigger: トリガータイプ ("on_break", "on_insert", "on_tick" など)
 * - ingredient: 祠内部のアイテム（Ingredient で一致判定）
 * - requireNoSilkTouch: シルクタッチなしを必須とするか（破壊時のみ）
 * - actions: 実行するアクションのリスト
 * - priority: 複数レシピがマッチした場合の優先度（高い方が優先）
 *
 * JSON例:
 * <pre>
 * {
 *   "type": "kamigami:shrine_offering",
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
public record ShrineOfferingRecipe(TriggerType trigger, Ingredient ingredient, boolean requireNoSilkTouch,
        OfferingAction actions, int priority) {

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

    public static final MapCodec<ShrineOfferingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TriggerType.CODEC.fieldOf("trigger").forGetter(ShrineOfferingRecipe::trigger),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ShrineOfferingRecipe::ingredient),
            Codec.BOOL.optionalFieldOf("require_no_silk_touch", false)
                    .forGetter(ShrineOfferingRecipe::requireNoSilkTouch),
            OfferingActions.ACTION_CODEC.fieldOf("actions").forGetter(ShrineOfferingRecipe::actions),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ShrineOfferingRecipe::priority))
            .apply(instance, ShrineOfferingRecipe::new));

    /**
     * このレシピが指定されたアイテムにマッチするか判定する。
     *
     * @param stack 祠内部のアイテム
     * @return マッチする場合は true
     */
    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }
}
