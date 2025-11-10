package com.hydryhydra.kamigami.offering;

import java.util.List;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 複数のアクションを順番に実行する合成アクション。
 *
 * JSON例:
 * <pre>
 * {
 *   "type": "sequence",
 *   "steps": [
 *     {"type": "play_effects", ...},
 *     {"type": "spawn_entity", ...}
 *   ]
 * }
 * </pre>
 */
public record SequenceAction(List<OfferingAction> steps) implements OfferingAction {
    public static final MapCodec<SequenceAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(OfferingActions.ACTION_CODEC.listOf().fieldOf("steps").forGetter(SequenceAction::steps))
            .apply(instance, SequenceAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        boolean anyExecuted = false;
        for (OfferingAction action : steps) {
            try {
                boolean executed = action.perform(ctx);
                anyExecuted |= executed;
            } catch (Exception e) {
                KamiGami.LOGGER.error("Error executing action in sequence: {}", action.getClass().getSimpleName(), e);
            }
        }
        return anyExecuted;
    }

    @Override
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC;
    }
}
