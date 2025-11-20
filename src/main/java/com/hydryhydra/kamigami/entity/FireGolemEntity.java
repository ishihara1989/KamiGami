package com.hydryhydra.kamigami.entity;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 魑魅魍魎の祟り・炎の神型 (Fire Golem - Evil Spirit of Fire Deity)
 *
 * 火の神の御神体が入った祠をシルクタッチなしで破壊した際に召喚される敵対モンスター。 - アイアンゴーレムのモデルを流用、赤いテクスチャ - HP:
 * 20.0 - 攻撃時に火傷効果を付与 - 炎・マグマ耐性を持つ - 水でダメージを受ける
 */
public class FireGolemEntity extends Monster {

    public FireGolemEntity(EntityType<? extends FireGolemEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        // 通常の攻撃処理
        boolean result = super.doHurtTarget(level, target);

        // 攻撃が成功し、ターゲットがLivingEntityの場合、火傷効果を付与
        if (result && target instanceof LivingEntity livingTarget) {
            // 火傷効果を5秒（100tick）付与
            livingTarget.setRemainingFireTicks(100);
            KamiGami.LOGGER.info("Fire Golem set target on fire for 5 seconds");
        }

        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float damageAmount) {
        // 水に触れている場合、追加ダメージ
        if (this.isInWater()) {
            KamiGami.LOGGER.info("Fire Golem taking water damage: {}", damageAmount);
            this.hurtServer(level, this.damageSources().drown(), 1.0F);
        }

        super.actuallyHurt(level, damageSource, damageAmount);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
        // 炎とマグマのダメージを無効化
        if (damageSource.is(DamageTypes.IN_FIRE) || damageSource.is(DamageTypes.ON_FIRE)
                || damageSource.is(DamageTypes.LAVA) || damageSource.is(DamageTypes.HOT_FLOOR)) {
            KamiGami.LOGGER.debug("Fire Golem is immune to fire/lava damage");
            return true;
        }

        return super.isInvulnerableTo(level, damageSource);
    }

    @Override
    public void tick() {
        super.tick();

        // 炎のパーティクルを定期的に発生させる
        if (this.level().isClientSide() && this.random.nextInt(10) == 0) {
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.5D), this.getRandomY(),
                        this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
        }

        // 水に触れている場合、毎tick少しダメージ
        if (!this.level().isClientSide() && this.isInWater() && this.tickCount % 20 == 0) {
            this.hurtServer((ServerLevel) this.level(), this.damageSources().drown(), 1.0F);
            KamiGami.LOGGER.debug("Fire Golem taking periodic water damage");

            // 水の中で煙パーティクルを発生
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.2D,
                        0.5D, 0.2D, 0.0D);
            }
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 50;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
