package com.hydryhydra.kamigami.curse;

import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 条件付きブロック置換アクション。
 *
 * 機能: - タグベースのブロックマッチング（例: "#minecraft:logs"） - マッチしたブロックを置換または削除 -
 * オプションでアイテムをドロップ（植物→骨粉変換など）
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "conditional_replace",
 *   "match_tag": "minecraft:logs",
 *   "replace_with": "minecraft:air",
 *   "drop_item": "minecraft:bone_meal",
 *   "drop_count": 1
 * }
 * </pre>
 */
public record ConditionalReplaceAction(Optional<TagKey<Block>> matchTag, Optional<BlockState> matchState,
        BlockState replaceWith, Optional<Item> dropItem, int dropCount) implements CurseAction {

    public static final MapCodec<ConditionalReplaceAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(TagKey.codec(Registries.BLOCK).optionalFieldOf("match_tag")
                    .forGetter(ConditionalReplaceAction::matchTag),
                    BlockState.CODEC.optionalFieldOf("match_state").forGetter(ConditionalReplaceAction::matchState),
                    BlockState.CODEC.fieldOf("replace_with").forGetter(ConditionalReplaceAction::replaceWith),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("drop_item")
                            .forGetter(ConditionalReplaceAction::dropItem),
                    Codec.INT.optionalFieldOf("drop_count", 1).forGetter(ConditionalReplaceAction::dropCount))
            .apply(instance, ConditionalReplaceAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        try {
            BlockState currentState = ctx.level().getBlockState(ctx.origin());

            // マッチング判定
            boolean matches = false;
            if (matchTag.isPresent() && currentState.is(matchTag.get())) {
                matches = true;
            }
            if (matchState.isPresent() && currentState.equals(matchState.get())) {
                matches = true;
            }

            if (!matches) {
                return false; // マッチしない場合は何もしない
            }

            // ブロックを置換
            ctx.level().setBlock(ctx.origin(), replaceWith, 3);

            // アイテムをドロップ（オプション）
            if (dropItem.isPresent()) {
                ItemStack stack = new ItemStack(dropItem.get(), dropCount);
                Containers.dropItemStack(ctx.level(), ctx.origin().getX(), ctx.origin().getY(), ctx.origin().getZ(),
                        stack);
                KamiGami.LOGGER.debug("Dropped {} x{} at {}", dropItem.get(), dropCount, ctx.origin());
            }

            KamiGami.LOGGER.debug("Replaced block at {} with {}", ctx.origin(), replaceWith.getBlock());
            return true;
        } catch (Exception e) {
            KamiGami.LOGGER.error("Error in conditional replace at {}", ctx.origin(), e);
            return false;
        }
    }

    @Override
    public MapCodec<? extends CurseAction> codec() {
        return CODEC;
    }

    /**
     * タグベースのブロック削除用のヘルパー。
     *
     * @param tag
     *            削除するブロックのタグ
     * @return ConditionalReplaceAction
     */
    public static ConditionalReplaceAction removeBlocksWithTag(TagKey<Block> tag) {
        return new ConditionalReplaceAction(Optional.of(tag), Optional.empty(), Blocks.AIR.defaultBlockState(),
                Optional.empty(), 0);
    }

    /**
     * 植物を骨粉に変換するヘルパー。
     *
     * @param plantTag
     *            植物のタグ
     * @return ConditionalReplaceAction
     */
    public static ConditionalReplaceAction convertPlantsToBonemeal(TagKey<Block> plantTag) {
        return new ConditionalReplaceAction(Optional.of(plantTag), Optional.empty(), Blocks.AIR.defaultBlockState(),
                Optional.of(net.minecraft.world.item.Items.BONE_MEAL), 1);
    }
}
