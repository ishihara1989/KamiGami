package com.hydryhydra.kamigami.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * 魑魅魍魎の祟り (Tatari of Minor Evil Spirits)
 * 黒いスライムのような敵対モンスター。
 * 祠をシルクタッチなしで破壊した際に召喚される。
 *
 * - サイズ2: HP 16、攻撃力4
 * - サイズ1: HP 4、攻撃力2
 * - 倒すと分裂（サイズ2→サイズ1×2）
 * - サイズ1を倒すとスライムボールかインクをドロップ
 */
public class TatariEntity extends Monster implements Enemy {
    private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(TatariEntity.class, EntityDataSerializers.INT);

    public TatariEntity(EntityType<? extends TatariEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new TatariEntity.TatariMoveControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TatariEntity.TatariFloatGoal(this));
        this.goalSelector.addGoal(2, new TatariEntity.TatariAttackGoal(this));
        this.goalSelector.addGoal(3, new TatariEntity.TatariRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new TatariEntity.TatariKeepOnJumpingGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_SIZE, 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    public void setSize(int size, boolean resetHealth) {
        int clampedSize = Mth.clamp(size, 1, 127);
        this.entityData.set(ID_SIZE, clampedSize);
        this.reapplyPosition();
        this.refreshDimensions();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)(clampedSize * clampedSize));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)(0.2F + 0.1F * (float)clampedSize));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((double)clampedSize);
        if (resetHealth) {
            this.setHealth(this.getMaxHealth());
        }

        this.xpReward = clampedSize;
    }

    public int getSize() {
        return this.entityData.get(ID_SIZE);
    }

    @Override
    public EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        float size = 0.51000005F * (float)this.getSize();
        return EntityDimensions.scalable(size, size);
    }

    @Override
    public void tick() {
        this.squish += (this.targetSquish - this.squish) * 0.5F;
        this.oSquish = this.squish;
        super.tick();
        if (this.onGround() && !this.wasOnGround) {
            int size = this.getSize();

            for(int i = 0; i < size * 8; ++i) {
                float f = this.random.nextFloat() * 6.2831855F;
                float f1 = this.random.nextFloat() * 0.5F + 0.5F;
                float f2 = Mth.sin(f) * (float)size * 0.5F * f1;
                float f3 = Mth.cos(f) * (float)size * 0.5F * f1;
                this.level().addParticle(this.getParticleType(), this.getX() + (double)f2, this.getY(), this.getZ() + (double)f3, 0.0D, 0.0D, 0.0D);
            }

            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.targetSquish = -0.5F;
        } else if (!this.onGround() && this.wasOnGround) {
            this.targetSquish = 1.0F;
        }

        this.wasOnGround = this.onGround();
        this.decreaseSquish();
    }

    protected void decreaseSquish() {
        this.targetSquish *= 0.6F;
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (ID_SIZE.equals(key)) {
            this.refreshDimensions();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
            if (this.isInWater() && this.random.nextInt(20) == 0) {
                this.doWaterSplashEffect();
            }
        }

        super.onSyncedDataUpdated(key);
    }

    protected ParticleOptions getParticleType() {
        return ParticleTypes.SQUID_INK;
    }

    protected SoundEvent getSquishSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isTiny() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isTiny() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
    }

    protected SoundEvent getJumpSound() {
        return this.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F * (float)this.getSize();
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }

    protected boolean isDealsDamage() {
        return this.isEffectiveAi();
    }

    protected float getAttackDamage() {
        return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    protected void dealDamage(LivingEntity target) {
        if (this.isAlive()) {
            int size = this.getSize();
            if (this.distanceToSqr(target) < 0.6D * (double)size * 0.6D * (double)size && this.hasLineOfSight(target)) {
                target.hurtOrSimulate(this.damageSources().mobAttack(this), this.getAttackDamage());
                this.playSound(SoundEvents.SLIME_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }
        }
    }

    protected boolean isTiny() {
        return this.getSize() <= 1;
    }

    @Override
    protected float getJumpPower() {
        return 0.4F;
    }

    @Override
    public void jumpFromGround() {
        Vec3 vec3 = this.getDeltaMovement();
        float f = this.getJumpPower();
        this.setDeltaMovement(vec3.x, (double)f, vec3.z);
        this.hasImpulse = true;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason spawnReason, SpawnGroupData spawnData) {
        int size = 2; // デフォルトでサイズ2
        this.setSize(size, true);
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
    }

    float squish;
    float targetSquish;
    float oSquish;
    private boolean wasOnGround;

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        int size = this.getSize();
        if (!this.level().isClientSide() && size > 1 && this.isDeadOrDying()) {
            // 分裂処理
            int newSize = size / 2;
            int splitCount = 2 + this.random.nextInt(3);

            float f = (float)newSize / (float)size;
            for(int i = 0; i < splitCount; ++i) {
                float f1 = ((float)(i % 2) - 0.5F) * f;
                float f2 = ((float)(i / 2) - 0.5F) * f;
                TatariEntity tatari = (TatariEntity)this.getType().create(this.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (tatari != null && this.isPersistenceRequired()) {
                    tatari.setPersistenceRequired();
                }

                if (tatari != null) {
                    tatari.setCustomName(this.getCustomName());
                    tatari.setNoAi(this.isNoAi());
                    tatari.setInvulnerable(this.isInvulnerable());
                    tatari.setSize(newSize, true);
                    tatari.setPos(this.getX() + (double)f1, this.getY() + 0.5D, this.getZ() + (double)f2);
                    tatari.setYRot(this.random.nextFloat() * 360.0F);
                    this.level().addFreshEntity(tatari);
                }
            }
        }

        super.remove(reason);
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        super.push(entity);
        if (entity instanceof IronGolem && this.isDealsDamage()) {
            this.dealDamage((LivingEntity)entity);
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (this.isDealsDamage()) {
            this.dealDamage(player);
        }
    }

    // AI Goals
    static class TatariMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final TatariEntity tatari;
        private boolean isAggressive;

        public TatariMoveControl(TatariEntity tatari) {
            super(tatari);
            this.tatari = tatari;
            this.yRot = 180.0F * tatari.getYRot() / 3.1415927F;
        }

        public void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.isAggressive = aggressive;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = MoveControl.Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = this.tatari.getJumpDelay();
                        if (this.isAggressive) {
                            this.jumpDelay /= 3;
                        }

                        this.tatari.getJumpControl().jump();
                        if (this.tatari.doPlayJumpSound()) {
                            this.tatari.playSound(this.tatari.getJumpSound(), this.tatari.getSoundVolume(), this.tatari.getJumpSoundPitch());
                        }
                    } else {
                        this.tatari.xxa = 0.0F;
                        this.tatari.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }

    static class TatariFloatGoal extends Goal {
        private final TatariEntity tatari;

        public TatariFloatGoal(TatariEntity tatari) {
            this.tatari = tatari;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
            tatari.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (this.tatari.isInWater() || this.tatari.isInLava()) && this.tatari.getMoveControl() instanceof TatariEntity.TatariMoveControl;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.tatari.getRandom().nextFloat() < 0.8F) {
                this.tatari.getJumpControl().jump();
            }

            ((TatariEntity.TatariMoveControl)this.tatari.getMoveControl()).setWantedMovement(1.2D);
        }
    }

    static class TatariAttackGoal extends Goal {
        private final TatariEntity tatari;
        private int growTiredTimer;

        public TatariAttackGoal(TatariEntity tatari) {
            this.tatari = tatari;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.tatari.getTarget();
            if (target == null) {
                return false;
            } else {
                return !this.tatari.canAttack(target) ? false : this.tatari.getMoveControl() instanceof TatariEntity.TatariMoveControl;
            }
        }

        @Override
        public void start() {
            this.growTiredTimer = reducedTickDelay(300);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.tatari.getTarget();
            if (target == null) {
                return false;
            } else if (!this.tatari.canAttack(target)) {
                return false;
            } else {
                return --this.growTiredTimer > 0;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.tatari.getTarget();
            if (target != null) {
                this.tatari.lookAt(target, 10.0F, 10.0F);
            }

            MoveControl movecontrol = this.tatari.getMoveControl();
            if (movecontrol instanceof TatariEntity.TatariMoveControl tatariMoveControl) {
                tatariMoveControl.setDirection(this.tatari.getYRot(), this.tatari.isDealsDamage());
            }
        }
    }

    static class TatariRandomDirectionGoal extends Goal {
        private final TatariEntity tatari;
        private float chosenDegrees;
        private int nextRandomizeTime;

        public TatariRandomDirectionGoal(TatariEntity tatari) {
            this.tatari = tatari;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.tatari.getTarget() == null && (this.tatari.onGround() || this.tatari.isInWater() || this.tatari.isInLava() || this.tatari.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) && this.tatari.getMoveControl() instanceof TatariEntity.TatariMoveControl;
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + this.tatari.getRandom().nextInt(60));
                this.chosenDegrees = (float)this.tatari.getRandom().nextInt(360);
            }

            MoveControl movecontrol = this.tatari.getMoveControl();
            if (movecontrol instanceof TatariEntity.TatariMoveControl tatariMoveControl) {
                tatariMoveControl.setDirection(this.chosenDegrees, false);
            }
        }
    }

    static class TatariKeepOnJumpingGoal extends Goal {
        private final TatariEntity tatari;

        public TatariKeepOnJumpingGoal(TatariEntity tatari) {
            this.tatari = tatari;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !this.tatari.isPassenger();
        }

        @Override
        public void tick() {
            MoveControl movecontrol = this.tatari.getMoveControl();
            if (movecontrol instanceof TatariEntity.TatariMoveControl tatariMoveControl) {
                tatariMoveControl.setWantedMovement(1.0D);
            }
        }
    }

    protected boolean doPlayJumpSound() {
        return true;
    }

    protected float getJumpSoundPitch() {
        float f = this.isTiny() ? 1.4F : 0.8F;
        return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * f;
    }
}
