package com.hydryhydra.kamigami.entity;

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

    public PaperSheepEntity(EntityType<? extends ShikigamiEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, stack -> stack.is(Items.WHEAT), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
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
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Allow shearing with shears
        if (itemStack.is(Items.SHEARS)) {
            if (!this.level().isClientSide() && !this.isBaby()) {
                // Play shearing sound
                this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
                this.gameEvent(GameEvent.SHEAR, player);

                // Damage shears
                itemStack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(itemStack));

                // Drop 1-3 wool (similar to vanilla sheep but simplified)
                int woolAmount = 1 + this.random.nextInt(3);
                for (int i = 0; i < woolAmount; i++) {
                    this.spawnAtLocation((ServerLevel) this.level(), Items.WHITE_WOOL);
                }

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
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
