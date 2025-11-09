package com.hydryhydra.kamigami.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 豊穣の御神体 (Charm of Fertility)
 *
 * 祠にセットすることで豊穣の効果を発動させる御神体アイテム。
 *
 * 効果: - 祠の周囲3x3マスに一定間隔で骨粉効果 - 土ブロックを草ブロックに変換 - 御神体が僅かに発光する - 骨粉っぽいパーティクルが出る
 */
public class CharmOfFertilityItem extends Item {
    public CharmOfFertilityItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 常に僅かに発光させる（エンチャント光沢効果）
        return true;
    }
}
