package com.hydryhydra.kamigami.offering;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 指定された範囲内の各座標に対してアクションを実行する合成アクション。
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "area",
 *   "shape": {
 *     "min": {"x": -2, "y": -2, "z": -2},
 *     "max": {"x": 2, "y": 0, "z": 2}
 *   },
 *   "per_position": {
 *     "type": "replace_block",
 *     "with": "minecraft:clay"
 *   }
 * }
 * </pre>
 */
public record AreaAction(Box shape, OfferingAction perPosition) implements OfferingAction {

    /**
     * 3D矩形範囲を表すレコード。 座標は起点（祠の位置）からの相対座標。
     */
    public record Box(Vec3 min, Vec3 max) {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(Vec3.CODEC.fieldOf("min").forGetter(Box::min), Vec3.CODEC.fieldOf("max").forGetter(Box::max))
                .apply(instance, Box::new));

        /**
         * この範囲内の全ブロック座標を起点からの相対座標として反復する。
         *
         * @param origin
         *            起点座標（祠の位置）
         * @return 反復可能なブロック座標のストリーム
         */
        public Iterable<BlockPos> iterate(BlockPos origin) {
            int minX = origin.getX() + (int) Math.floor(min.x);
            int minY = origin.getY() + (int) Math.floor(min.y);
            int minZ = origin.getZ() + (int) Math.floor(min.z);
            int maxX = origin.getX() + (int) Math.floor(max.x);
            int maxY = origin.getY() + (int) Math.floor(max.y);
            int maxZ = origin.getZ() + (int) Math.floor(max.z);

            return BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static final MapCodec<AreaAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Box.CODEC.fieldOf("shape").forGetter(AreaAction::shape),
                    OfferingActions.ACTION_CODEC.fieldOf("per_position").forGetter(AreaAction::perPosition))
            .apply(instance, AreaAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        boolean anyExecuted = false;
        int positionCount = 0;

        for (BlockPos pos : shape.iterate(ctx.origin())) {
            positionCount++;
            try {
                // 各座標に対して再現性のある乱数を生成
                RandomSource posRandom = createDeterministicRandom(ctx, pos);

                // 新しいコンテキストを作成（座標と乱数を更新）
                ActionContext posContext = new ActionContext(ctx.level(), pos, ctx.player(), ctx.offeredItem(),
                        posRandom);

                boolean executed = perPosition.perform(posContext);
                anyExecuted |= executed;
            } catch (Exception e) {
                KamiGami.LOGGER.error("Error executing action at position {}", pos, e);
            }
        }

        KamiGami.LOGGER.debug("AreaAction processed {} positions", positionCount);
        return anyExecuted;
    }

    /**
     * 座標ベースの決定論的な乱数生成器を作成する。 マルチプレイでクライアント・サーバー間の同期を保証するため。
     *
     * @param ctx
     *            元のコンテキスト
     * @param pos
     *            現在の座標
     * @return 決定論的な乱数生成器
     */
    private RandomSource createDeterministicRandom(ActionContext ctx, BlockPos pos) {
        // ワールドシード + 座標 + 元の乱数のシード から新しいシードを生成
        long worldSeed = ctx.level().getSeed();
        long posSeed = Mth.getSeed(pos);
        long combinedSeed = worldSeed ^ posSeed ^ 0x9E3779B97F4A7C15L; // ゴールデンレシオハッシュ

        return RandomSource.create(combinedSeed);
    }

    @Override
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC;
    }
}
