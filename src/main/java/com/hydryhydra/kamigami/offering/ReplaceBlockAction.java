package com.hydryhydra.kamigami.offering;

import java.util.List;
import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;

/**
 * ブロックを置換するアクション。
 *
 * 機能:
 * - 単一ブロックの指定 ("with" フィールド)
 * - 重み付きパレット ("palette" フィールド) - ランダムに1つ選択
 * - 確率指定 ("chance" フィールド) - 0.0〜1.0
 * - 条件指定 ("when_air" など) - 空気ブロックのみ置換など
 *
 * JSON例:
 * <pre>
 * {
 *   "type": "replace_block",
 *   "with": "minecraft:clay",
 *   "chance": 1.0
 * }
 * </pre>
 *
 * パレット使用例:
 * <pre>
 * {
 *   "type": "replace_block",
 *   "palette": [
 *     {"state": "minecraft:clay", "weight": 3},
 *     {"state": "minecraft:dirt", "weight": 2},
 *     {"state": "minecraft:moss_block", "weight": 1}
 *   ],
 *   "chance": 0.4
 * }
 * </pre>
 */
public record ReplaceBlockAction(Optional<BlockState> with, Optional<List<PaletteEntry>> palette, float chance,
        boolean whenAir) implements OfferingAction {

    /**
     * パレットエントリ（ブロック状態と重み）
     */
    public record PaletteEntry(BlockState state, int weight) {
        public static final Codec<PaletteEntry> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(BlockState.CODEC.fieldOf("state").forGetter(PaletteEntry::state),
                        Codec.INT.optionalFieldOf("weight", 1).forGetter(PaletteEntry::weight))
                .apply(instance, PaletteEntry::new));
    }

    public static final MapCodec<ReplaceBlockAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.optionalFieldOf("with").forGetter(ReplaceBlockAction::with),
            PaletteEntry.CODEC.listOf().optionalFieldOf("palette").forGetter(ReplaceBlockAction::palette),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(ReplaceBlockAction::chance),
            Codec.BOOL.optionalFieldOf("when_air", false).forGetter(ReplaceBlockAction::whenAir))
            .apply(instance, ReplaceBlockAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        try {
            // 確率チェック
            if (ctx.random().nextFloat() > chance) {
                return false;
            }

            BlockState currentState = ctx.level().getBlockState(ctx.origin());

            // 条件チェック: when_air が true の場合、空気ブロックのみ対象
            if (whenAir && !currentState.isAir()) {
                return false;
            }

            // 置換するブロックを決定
            BlockState targetState = null;
            if (with.isPresent()) {
                targetState = with.get();
            } else if (palette.isPresent()) {
                targetState = pickFromPalette(palette.get(), ctx);
            }

            if (targetState == null) {
                KamiGami.LOGGER.warn("ReplaceBlockAction: No target block specified (neither 'with' nor 'palette')");
                return false;
            }

            // ブロックを置換
            ctx.level().setBlock(ctx.origin(), targetState, 3);
            KamiGami.LOGGER.debug("Replaced block at {} with {}", ctx.origin(), targetState.getBlock());
            return true;
        } catch (Exception e) {
            KamiGami.LOGGER.error("Error replacing block at {}", ctx.origin(), e);
            return false;
        }
    }

    /**
     * パレットから重み付きでランダムに1つ選択する。
     *
     * @param paletteList パレットエントリのリスト
     * @param ctx アクションコンテキスト
     * @return 選択されたブロック状態
     */
    private BlockState pickFromPalette(List<PaletteEntry> paletteList, ActionContext ctx) {
        if (paletteList.isEmpty()) {
            return null;
        }

        // 重みの合計を計算
        int totalWeight = paletteList.stream().mapToInt(PaletteEntry::weight).sum();
        if (totalWeight <= 0) {
            // 重みがすべて0以下の場合は最初のエントリを返す
            return paletteList.get(0).state();
        }

        // ランダムに重みを選択
        int randomWeight = ctx.random().nextInt(totalWeight);
        int currentWeight = 0;

        for (PaletteEntry entry : paletteList) {
            currentWeight += entry.weight();
            if (randomWeight < currentWeight) {
                return entry.state();
            }
        }

        // フォールバック（通常ここには到達しない）
        return paletteList.get(0).state();
    }

    @Override
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC;
    }
}
