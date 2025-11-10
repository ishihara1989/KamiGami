package com.hydryhydra.kamigami.offering;

import java.util.Optional;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * サウンドとパーティクルを再生するアクション。
 *
 * JSON例:
 *
 * <pre>
 * {
 *   "type": "play_effects",
 *   "sound": "minecraft:entity.generic.explode",
 *   "sound_volume": 1.0,
 *   "sound_pitch": 1.0,
 *   "particle": "minecraft:explosion_emitter",
 *   "particle_count": 1,
 *   "offset": {"x": 0.5, "y": 0.5, "z": 0.5}
 * }
 * </pre>
 */
public record PlayEffectsAction(Optional<SoundEvent> sound, float soundVolume, float soundPitch,
        Optional<ParticleOptions> particle, int particleCount, Vec3 offset) implements OfferingAction {
    public static final MapCodec<PlayEffectsAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound")
                    .forGetter(PlayEffectsAction::sound),
                    RecordCodecBuilder
                            .mapCodec(i -> i.group(
                                    net.minecraft.util.ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("sound_volume", 1.0F)
                                            .forGetter(a -> ((PlayEffectsAction) a).soundVolume),
                                    net.minecraft.util.ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("sound_pitch", 1.0F)
                                            .forGetter(a -> ((PlayEffectsAction) a).soundPitch))
                                    .apply(i, (vol, pitch) -> null)) // ダミー（実際にはフィールドで取得）
                            .forGetter(a -> a),
                    ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(PlayEffectsAction::particle),
                    net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("particle_count", 1)
                            .forGetter(PlayEffectsAction::particleCount),
                    Vec3.CODEC.optionalFieldOf("offset", new Vec3(0.5, 0.5, 0.5)).forGetter(PlayEffectsAction::offset))
            .apply(instance, (snd, dummy, part, count, off) -> {
                // ダミーから実際の値を取得（このコーデックの構造上必要な回避策）
                return new PlayEffectsAction(snd, 1.0F, 1.0F, part, count, off);
            }));

    // より簡潔なコーデック定義（上記のネストを避ける）
    public static final MapCodec<PlayEffectsAction> CODEC_SIMPLE = RecordCodecBuilder.mapCodec(instance -> instance
            .group(BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound")
                    .forGetter(PlayEffectsAction::sound),
                    net.minecraft.util.ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("sound_volume", 1.0F)
                            .forGetter(PlayEffectsAction::soundVolume),
                    net.minecraft.util.ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("sound_pitch", 1.0F)
                            .forGetter(PlayEffectsAction::soundPitch),
                    ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(PlayEffectsAction::particle),
                    net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("particle_count", 1)
                            .forGetter(PlayEffectsAction::particleCount),
                    Vec3.CODEC.optionalFieldOf("offset", new Vec3(0.5, 0.5, 0.5)).forGetter(PlayEffectsAction::offset))
            .apply(instance, PlayEffectsAction::new));

    @Override
    public boolean perform(ActionContext ctx) {
        double x = ctx.origin().getX() + offset.x;
        double y = ctx.origin().getY() + offset.y;
        double z = ctx.origin().getZ() + offset.z;

        boolean anyEffect = false;

        // サウンドを再生
        if (sound.isPresent()) {
            ctx.level().playSound(null, ctx.origin(), sound.get(), SoundSource.BLOCKS, soundVolume, soundPitch);
            KamiGami.LOGGER.debug("Played sound {} at ({}, {}, {}) with volume={}, pitch={}",
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(sound.get()), x, y, z,
                    soundVolume, soundPitch);
            anyEffect = true;
        }

        // パーティクルを生成
        if (particle.isPresent()) {
            ctx.level().sendParticles(particle.get(), x, y, z, particleCount, 0, 0, 0, 0);
            KamiGami.LOGGER.debug("Spawned {} particle(s) {} at ({}, {}, {})", particleCount,
                    particle.get().getType().toString(), x, y, z);
            anyEffect = true;
        }

        return anyEffect;
    }

    @Override
    public MapCodec<? extends OfferingAction> codec() {
        return CODEC_SIMPLE;
    }
}
