package com.hydryhydra.kamigami.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Paper Chicken Shikigami (紙の鶏)
 * A summoned paper chicken that behaves like a vanilla chicken but with low HP
 */
public class PaperChickenEntity extends ShikigamiEntity {
    public int eggTime = this.random.nextInt(6000) + 6000;

    public PaperChickenEntity(EntityType<? extends ShikigamiEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        // No BreedGoal - Shikigami cannot breed
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.0D, stack -> stack.is(Items.WHEAT_SEEDS) ||
                stack.is(Items.MELON_SEEDS) ||
                stack.is(Items.PUMPKIN_SEEDS) ||
                stack.is(Items.BEETROOT_SEEDS) ||
                stack.is(Items.TORCHFLOWER_SEEDS) ||
                stack.is(Items.PITCHER_POD), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createShikigamiAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 4.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE, 10.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Handle egg laying
        if (!this.level().isClientSide() && this.isAlive() && !this.isBaby() && --this.eggTime <= 0) {
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation((ServerLevel) this.level(), Items.EGG);
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.eggTime = this.random.nextInt(6000) + 6000;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        // Shikigami cannot breed
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) ||
                stack.is(Items.MELON_SEEDS) ||
                stack.is(Items.PUMPKIN_SEEDS) ||
                stack.is(Items.BEETROOT_SEEDS) ||
                stack.is(Items.TORCHFLOWER_SEEDS) ||
                stack.is(Items.PITCHER_POD);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }
}
