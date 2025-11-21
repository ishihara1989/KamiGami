package com.hydryhydra.kamigami.curse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 確率でアクションを実行する合成アクション。
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "chance",
 *   "probability": 0.4,
 *   "action": {
 *     "type": "replace_block",
 *     "with": "minecraft:clay"
 *   }
 * }
 * </pre>
 */
public record ChanceAction(float probability, CurseAction action) implements CurseAction {
    public static final MapCodec<ChanceAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.FLOAT.fieldOf("probability").forGetter(ChanceAction::probability),
                    CurseActions.ACTION_CODEC.fieldOf("action").forGetter(ChanceAction::action))
            .apply(instance, ChanceAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        if (ctx.random().nextFloat() < probability) {
            return action.perform(ctx);
        }
        return false;
    }

    @Override
    public MapCodec<? extends CurseAction> codec() {
        return CODEC;
    }
}
