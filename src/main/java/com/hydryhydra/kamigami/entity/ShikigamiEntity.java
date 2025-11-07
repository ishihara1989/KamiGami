package com.hydryhydra.kamigami.entity;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Shikigami entities (式神) Shikigami are paper summoned
 * creatures with low HP
 */
public abstract class ShikigamiEntity extends Animal {

    public ShikigamiEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Creates default attributes for shikigami entities Low health (4.0 = 2 hearts)
     * and normal movement speed
     */
    public static AttributeSupplier.Builder createShikigamiAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Nullable
    @Override
    public abstract AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob mate);
}
