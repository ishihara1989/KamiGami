package com.hydryhydra.kamigami.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * Paper Sheep Shikigami (紙の羊)
 * A summoned paper sheep that behaves like a vanilla sheep but with low HP
 * Can be sheared for wool but does not drop wool on death
 */
public class PaperSheepEntity extends ShikigamiEntity {

    private static final EntityDataAccessor<Boolean> DATA_SHEARED_ID = SynchedEntityData
            .defineId(PaperSheepEntity.class, EntityDataSerializers.BOOLEAN);

    public PaperSheepEntity(EntityType<? extends ShikigamiEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_SHEARED_ID, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        // No BreedGoal - Shikigami cannot breed
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25D, stack -> stack.is(Items.WHEAT), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(4, new EatBlockGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createShikigamiAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 5.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE, 10.0D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        // Shikigami cannot breed
        return null;
    }

    @Override
    public void ate() {
        super.ate();
        this.setSheared(false);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Allow shearing with shears
        if (itemStack.is(Items.SHEARS)) {
            if (!this.level().isClientSide() && this.isShearable()) {
                this.shear(SoundSource.PLAYERS);
                this.gameEvent(GameEvent.SHEAR, player);
                itemStack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(itemStack));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    public void shear(SoundSource p_29823_) {
        this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, p_29823_, 1.0F, 1.0F);
        this.setSheared(true);
        int i = 1 + this.random.nextInt(3);

        for (int j = 0; j < i; ++j) {
            this.spawnAtLocation((ServerLevel) this.level(), Items.WHITE_WOOL);
        }
    }

    public boolean isShearable() {
        return this.isAlive() && !this.isSheared() && !this.isBaby();
    }

    public boolean isSheared() {
        return this.entityData.get(DATA_SHEARED_ID);
    }

    public void setSheared(boolean sheared) {
        this.entityData.set(DATA_SHEARED_ID, sheared);
    }

    // Note: NBT serialization methods may have changed in 1.21.10
    // Keeping data persistence logic for potential future use
    protected void saveShearData(CompoundTag pCompound) {
        pCompound.putBoolean("Sheared", this.isSheared());
    }

    protected void loadShearData(CompoundTag pCompound) {
        if (pCompound.contains("Sheared")) {
            this.setSheared(pCompound.getBoolean("Sheared").orElse(false));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
