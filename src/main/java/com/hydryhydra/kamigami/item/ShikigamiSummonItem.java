package com.hydryhydra.kamigami.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Base class for Shikigami summoning items Right-click to summon a shikigami
 * entity
 */
public class ShikigamiSummonItem extends Item {
    private final Supplier<EntityType<? extends Mob>> entityTypeSupplier;

    public ShikigamiSummonItem(Properties properties, Supplier<EntityType<? extends Mob>> entityTypeSupplier) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = context.getItemInHand();
        Vec3 clickLocation = context.getClickLocation();

        // Create and spawn the entity
        EntityType<? extends Mob> entityType = entityTypeSupplier.get();
        Mob entity = entityType.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);

        if (entity != null) {
            entity.setPos(clickLocation.x, clickLocation.y, clickLocation.z);
            entity.setYRot(context.getRotation());
            serverLevel.addFreshEntity(entity);

            // Consume the item
            Player player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                itemStack.shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
