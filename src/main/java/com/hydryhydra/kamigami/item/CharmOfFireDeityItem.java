package com.hydryhydra.kamigami.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 火の神の御神体 (Charm of Fire Deity)
 *
 * 祠にセットすることで火の神の効果を発動させる御神体アイテム。
 *
 * 効果: - 祠が明るさ15の光源になる - 祠の周囲に炎のパーティクルが出る - 周囲3x3マスにあるアイテムに炎のエフェクトが出る -
 * かまどのレシピで精錬可能なアイテムを変換する（精錬時間経過後）
 *
 * プレイヤーへの効果: - アイテムを持っているとき火炎・マグマ耐性を付与
 */
public class CharmOfFireDeityItem extends Item {
    public CharmOfFireDeityItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 常に僅かに発光させる（エンチャント光沢効果）
        return true;
    }
}
