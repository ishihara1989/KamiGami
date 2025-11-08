package com.hydryhydra.kamigami.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 沼の神の御神体 (Charm of Swamp Deity)
 *
 * 祠にセットすることで沼の神の効果を発動させる御神体アイテム。
 *
 * 効果: - 祠の周囲3x3マスの土に一定間隔できのこが生える - 御神体が僅かに発光する - 沼っぽいパーティクルが出る
 */
public class CharmOfSwampDeityItem extends Item {
    public CharmOfSwampDeityItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 常に僅かに発光させる（エンチャント光沢効果）
        return true;
    }
}
