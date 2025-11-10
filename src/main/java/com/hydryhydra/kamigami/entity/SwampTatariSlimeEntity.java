package com.hydryhydra.kamigami.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 沼の祟りスライム (Swamp Tatari Slime)
 *
 * 沼の神の御神体（Charm of Swamp Deity）を祠に奉納した際に召喚される特別な祟りスライム。 通常の祟りスライムとの違い： -
 * サイズ4で固定（大きく強力） - 倒しても分裂しない - 独自のドロップアイテム（土、粘土、リリパッド、きのこ、苔、スライムブロック）
 *
 * テクスチャとモデルは通常の祟りスライム（TatariSlimeEntity）と共有。
 */
public class SwampTatariSlimeEntity extends TatariSlimeEntity {
    private static final int SWAMP_SLIME_SIZE = 4;

    public SwampTatariSlimeEntity(EntityType<? extends SwampTatariSlimeEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // サイズが更新された場合のログ
        if (!this.level().isClientSide()) {
            com.hydryhydra.kamigami.KamiGami.LOGGER.info("SwampTatariSlime onSyncedDataUpdated on SERVER - Size: {}",
                    this.getSize());
        } else {
            com.hydryhydra.kamigami.KamiGami.LOGGER.info("SwampTatariSlime onSyncedDataUpdated on CLIENT - Size: {}",
                    this.getSize());
        }
    }

    @Override
    @SuppressWarnings("deprecation") // finalizeSpawn is deprecated but still needed for spawn initialization
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason spawnReason, SpawnGroupData spawnData) {
        // NBTからサイズが読み込まれている場合はそのまま使用、そうでない場合はサイズ4を設定
        int currentSize = this.getSize();
        if (currentSize == 1) {
            // デフォルトのサイズ1の場合のみ、サイズ4に変更
            this.setSize(SWAMP_SLIME_SIZE, true);
            com.hydryhydra.kamigami.KamiGami.LOGGER.info(
                    "SwampTatariSlime finalizeSpawn - Set size to {}, HP: {}, Attack: {}, spawn reason: {}",
                    SWAMP_SLIME_SIZE, this.getMaxHealth(),
                    this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    spawnReason);
        } else {
            // NBTから読み込まれた場合
            com.hydryhydra.kamigami.KamiGami.LOGGER.info(
                    "SwampTatariSlime finalizeSpawn - Using NBT size: {}, HP: {}, Attack: {}, spawn reason: {}",
                    currentSize, this.getMaxHealth(),
                    this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    spawnReason);
        }

        // 親クラス（Monster）の初期化処理を呼ぶ
        // TatariSlimeEntity.finalizeSpawn()はサイズが1でない場合は変更しないので、上書きされない
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnData);

        // サイズが親クラスで変更されていないことを確認
        com.hydryhydra.kamigami.KamiGami.LOGGER.info("SwampTatariSlime after super.finalizeSpawn - Final size: {}",
                this.getSize());

        return result;
    }

    /**
     * 分裂を無効化するためのフラグ。 沼の祟りスライムは分裂しない。
     */
    @Override
    protected boolean shouldSplit() {
        return false;
    }

    @Override
    public int getSize() {
        int size = super.getSize();
        // デバッグ: クライアント側でサイズが正しく取得できているか確認
        if (this.level().isClientSide() && this.tickCount % 20 == 0) {
            com.hydryhydra.kamigami.KamiGami.LOGGER
                    .info("SwampTatariSlime getSize() on CLIENT - Size: {}, TickCount: {}", size, this.tickCount);
        }
        return size;
    }
}
