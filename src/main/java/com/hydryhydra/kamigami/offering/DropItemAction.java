package com.hydryhydra.kamigami.offering;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * アイテムを指定座標にドロップするアクション。
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "drop_item",
 *   "item": "minecraft:bone_meal",
 *   "count": 1
 * }
 * </pre>
 */
public record DropItemAction(Item item, int count) implements OfferingAction {
    public static final MapCodec<DropItemAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(DropItemAction::item),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(DropItemAction::count))
            .apply(instance, DropItemAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        try {
            ItemStack stack = new ItemStack(item, count);
            Containers.dropItemStack(ctx.level(), ctx.origin().getX(), ctx.origin().getY(), ctx.origin().getZ(), stack);
            KamiGami.LOGGER.debug("Dropped {} x{} at {}", item, count, ctx.origin());
            return true;
        } catch (Exception e) {
            KamiGami.LOGGER.error("Error dropping item at {}", ctx.origin(), e);
            return false;
        }
    }

    @Override
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC;
    }
}
