package com.hydryhydra.kamigami.offering;

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
        Optional<CompoundTag> nbt) implements OfferingAction {
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

            // NBT を適用（オプション）
            // Phase 1 では最低限の対応として、Slime の Size のみサポート
            // 将来的により汎用的な NBT 適用を実装予定
            nbt.ifPresent(tag -> {
                try {
                    // Slime のサイズ設定
                    if (tag.contains("Size") && entity instanceof net.minecraft.world.entity.monster.Slime slime) {
                        tag.getInt("Size").ifPresent(size -> {
                            slime.setSize(size, true);
                            KamiGami.LOGGER.debug("Set slime size to: {}", size);
                        });
                    }
                    // 将来的に Phase 3 で汎用的な NBT 適用を実装予定
                    // 現在は個別のフィールドのみサポート
                } catch (Exception e) {
                    KamiGami.LOGGER.warn("Failed to apply NBT to entity: {}", e.getMessage());
                }
            });

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
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC;
    }
}
