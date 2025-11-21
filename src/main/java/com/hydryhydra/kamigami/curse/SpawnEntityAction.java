package com.hydryhydra.kamigami.curse;

import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * エンティティを召喚するアクション。
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "spawn_entity",
 *   "entity": "kamigami:tatari_slime",
 *   "offset": {"x": 0.5, "y": 0.5, "z": 0.5},
 *   "nbt": {"Size": 4}
 * }
 * </pre>
 */
public record SpawnEntityAction(EntityType<?> entityType, Vec3 offset,
        Optional<CompoundTag> nbt) implements CurseAction {
    public static final MapCodec<SpawnEntityAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity")
                    .forGetter(SpawnEntityAction::entityType),
                    Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(SpawnEntityAction::offset),
                    CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(SpawnEntityAction::nbt))
            .apply(instance, SpawnEntityAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        try {
            Entity entity = entityType.create(ctx.level(), EntitySpawnReason.TRIGGERED);
            if (entity == null) {
                KamiGami.LOGGER.warn("Failed to create entity: {}", entityType);
                return false;
            }

            // 位置を設定（オフセット適用）
            double x = ctx.origin().getX() + offset.x;
            double y = ctx.origin().getY() + offset.y;
            double z = ctx.origin().getZ() + offset.z;
            entity.setPos(x, y, z);

            // NBT を適用（Slimeサイズの特別処理）
            nbt.ifPresent(tag -> {
                try {
                    // Slime系エンティティ（TatariSlimeとその派生）のSizeを設定
                    if (tag.contains("Size") && entity instanceof net.minecraft.world.entity.monster.Slime slime) {
                        int size = tag.getIntOr("Size", 1);
                        slime.setSize(size, false); // false = HP をリセットしない
                        KamiGami.LOGGER.info("Applied Size NBT to Slime: {}, Size: {}", entityType.getDescriptionId(),
                                size);
                    }
                } catch (Exception e) {
                    KamiGami.LOGGER.warn("Failed to apply NBT to entity: {}", e.getMessage());
                }
            });

            // Mob の場合は finalizeSpawn を呼び出す（重要！）
            // finalizeSpawn is deprecated but required for proper initialization
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(x, y, z);
                net.minecraft.world.DifficultyInstance difficulty = ctx.level().getCurrentDifficultyAt(pos);
                @SuppressWarnings({"deprecation", "unused"})
                var ignored = mob.finalizeSpawn(ctx.level(), difficulty, EntitySpawnReason.TRIGGERED, null);
                KamiGami.LOGGER.info("Called finalizeSpawn for mob: {}", entityType.getDescriptionId());
            }

            // ワールドに追加
            boolean added = ctx.level().addFreshEntity(entity);
            if (added) {
                KamiGami.LOGGER.info("Spawned entity {} at ({}, {}, {})", entityType.getDescriptionId(), x, y, z);
            } else {
                KamiGami.LOGGER.warn("Failed to add entity to world: {}", entityType);
            }
            return added;
        } catch (Exception e) {
            KamiGami.LOGGER.error("Error spawning entity: {}", entityType, e);
            return false;
        }
    }

    @Override
    public MapCodec<? extends CurseAction> codec() {
        return CODEC;
    }
}
