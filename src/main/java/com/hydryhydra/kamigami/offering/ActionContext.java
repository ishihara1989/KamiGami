package com.hydryhydra.kamigami.offering;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * アクション実行時のコンテキスト情報を保持するレコード。
 *
 * @param level
 *            サーバーレベル（ワールド）
 * @param origin
 *            起点となる座標（祠の位置など）
 * @param player
 *            アクションをトリガーしたプレイヤー（null の場合あり）
 * @param offeredItem
 *            お供えされたアイテム（祠に入っていたアイテム）
 * @param random
 *            乱数生成器（再現性のあるシード付き）
 */
public record ActionContext(ServerLevel level, BlockPos origin, @Nullable Player player, ItemStack offeredItem,
        RandomSource random) {
}
