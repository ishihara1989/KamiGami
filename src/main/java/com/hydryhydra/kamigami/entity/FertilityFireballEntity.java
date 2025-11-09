package com.hydryhydra.kamigami.entity;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 豊穣の祟りの火の玉 (Fertility Fireball)
 *
 * ガストの火の玉と似ているが、爆発せずにダメージと空腹効果を与える。
 */
public class FertilityFireballEntity extends LargeFireball {

    public FertilityFireballEntity(EntityType<? extends LargeFireball> entityType, Level level) {
        super(entityType, level);
    }

    public FertilityFireballEntity(Level level, LivingEntity shooter, Vec3 direction) {
        super(level, shooter, direction, 0); // 爆発力0
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            // 爆発はさせない（explosionPowerが0なので自動的に爆発しない）
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide()) {
            Entity entity = result.getEntity();
            Entity owner = this.getOwner();

            // ダメージを与える（2.0 = 1ハート）
            DamageSource damageSource = this.damageSources().fireball(this, owner);
            entity.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), damageSource, 2.0F);

            // 追加効果: 空腹効果と満腹度減少
            if (entity instanceof LivingEntity livingEntity) {
                // 空腹効果を付与（30秒、レベル1）
                livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
                // 満腹度を減らす
                if (livingEntity instanceof Player player) {
                    player.getFoodData().addExhaustion(4.0F);
                }
                KamiGami.LOGGER.info("Fertility Fireball: Hit {} - applied hunger effect",
                        entity.getName().getString());
            }

            this.discard();
        }
    }
}
