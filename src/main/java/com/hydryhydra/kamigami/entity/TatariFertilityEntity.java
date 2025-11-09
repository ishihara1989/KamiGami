package com.hydryhydra.kamigami.entity;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 魑魅魍魎の祟り・豊穣の神型 (Tatari Fertility Deity - Evil Spirit of Fertility God)
 *
 * 豊穣の御神体が入った祠をシルクタッチなしで破壊した際に召喚される敵対モンスター。 - 高さ4ブロックの木の幹にジャック・オ・ランタンの顔 -
 * 2本の水平な枝に葉がある - ガストのような遠距離攻撃（爆発なし、ダメージ＋空腹効果付与）
 */
public class TatariFertilityEntity extends Monster {

    public TatariFertilityEntity(EntityType<? extends TatariFertilityEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new TatariFertilityAttackGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this)); // 優先度を8に変更（7との競合を回避）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D) // 40.0から20.0に半減
                .add(Attributes.FOLLOW_RANGE, 100.0D).add(Attributes.MOVEMENT_SPEED, 0.0D) // 動かない（木なので）
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // ノックバック無効
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 50;
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float damageAmount) {
        // 斧での攻撃の場合、ダメージを3倍にする
        if (damageSource.getEntity() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.is(ItemTags.AXES)) {
                KamiGami.LOGGER.info("Tatari Fertility: Hit by axe, doubling damage from {} to {}", damageAmount,
                        damageAmount * 3.0F);
                damageAmount *= 3.0F;
            }
        }
        super.actuallyHurt(level, damageSource, damageAmount);
    }

    /**
     * 飛翔体を発射する
     */
    private void performRangedAttack(LivingEntity target) {
        if (this.level().isClientSide()) {
            return;
        }

        KamiGami.LOGGER.info("Tatari Fertility: Firing projectile at target {}", target.getName().getString());

        // カスタムの火の玉を発射（爆発しない、空腹効果付与）
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.5D) - this.getY(0.5D);
        double d2 = target.getZ() - this.getZ();

        FertilityFireballEntity fireball = new FertilityFireballEntity(this.level(), this, new Vec3(d0, d1, d2));
        fireball.setPos(this.getX(), this.getY(0.5D) + 2.0D, this.getZ()); // 顔の位置から発射

        this.level().addFreshEntity(fireball);
        this.playSound(SoundEvents.GHAST_SHOOT, 10.0F,
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    /**
     * 遠距離攻撃用AI Goal
     */
    static class TatariFertilityAttackGoal extends Goal {
        private final TatariFertilityEntity entity;
        private int attackTime = -1;

        public TatariFertilityAttackGoal(TatariFertilityEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            return this.entity.getTarget() != null;
        }

        @Override
        public void start() {
            // 攻撃開始時の初期化
        }

        @Override
        public void stop() {
            // 攻撃停止時のクリーンアップ
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.entity.getTarget();
            if (target == null) {
                return;
            }

            double distanceSqr = this.entity.distanceToSqr(target);
            boolean canSee = this.entity.getSensing().hasLineOfSight(target);

            // ターゲットを見る
            this.entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (--this.attackTime == 0) {
                if (!canSee) {
                    return;
                }

                float f = (float) Math.sqrt(distanceSqr) / 64.0F;
                this.entity.performRangedAttack(target);
                this.attackTime = Mth.floor(f * (float) (40 - 20) + (float) 20);
            } else if (this.attackTime < 0) {
                this.attackTime = Mth.floor(Mth.lerp(Math.sqrt(distanceSqr) / 64.0D, (double) 20, (double) 40));
            }
        }
    }
}
