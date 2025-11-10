package com.hydryhydra.kamigami.offering;

import java.util.HashMap;
import java.util.Map;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.ResourceLocation;

/**
 * 全てのアクションタイプを登録・管理するレジストリ。
 *
 * 新しいアクションタイプを追加する場合は、ここに register を追加する。
 */
public class OfferingActions {
    // アクションタイプのマップ（名前 -> Codec）
    private static final Map<ResourceLocation, MapCodec<? extends OfferingAction>> ACTION_TYPES = new HashMap<>();

    // アクションの多相 Codec（JSON で "type" フィールドを使って判別）
    public static final Codec<OfferingAction> ACTION_CODEC = ResourceLocation.CODEC.dispatch("type",
            action -> getTypeId(action.codec()), OfferingActions::getCodec);

    // ========================================
    // アクションタイプの登録
    // ========================================

    // 合成アクション
    public static final MapCodec<SequenceAction> SEQUENCE = register("sequence", SequenceAction.CODEC);

    // Phase 1: 基本アクション
    public static final MapCodec<SpawnEntityAction> SPAWN_ENTITY = register("spawn_entity", SpawnEntityAction.CODEC);
    public static final MapCodec<PlayEffectsAction> PLAY_EFFECTS = register("play_effects",
            PlayEffectsAction.CODEC_SIMPLE);

    // Phase 2: ブロック操作アクション
    public static final MapCodec<ReplaceBlockAction> REPLACE_BLOCK = register("replace_block",
            ReplaceBlockAction.CODEC);
    public static final MapCodec<AreaAction> AREA = register("area", AreaAction.CODEC);
    public static final MapCodec<ChanceAction> CHANCE = register("chance", ChanceAction.CODEC);
    public static final MapCodec<DropItemAction> DROP_ITEM = register("drop_item", DropItemAction.CODEC);

    // ========================================
    // ヘルパーメソッド
    // ========================================

    /**
     * アクションタイプを登録する。
     *
     * @param name
     *            アクション名（例: "spawn_entity"）
     * @param codec
     *            アクションの Codec
     * @return 登録された Codec
     */
    private static <T extends OfferingAction> MapCodec<T> register(String name, MapCodec<T> codec) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, name);
        if (ACTION_TYPES.containsKey(id)) {
            throw new IllegalStateException("Duplicate action type registration: " + id);
        }
        ACTION_TYPES.put(id, codec);
        return codec;
    }

    /**
     * Codec から対応する ResourceLocation を取得する。
     *
     * @param codec
     *            アクションの Codec
     * @return 対応する ResourceLocation
     */
    private static ResourceLocation getTypeId(MapCodec<? extends OfferingAction> codec) {
        for (Map.Entry<ResourceLocation, MapCodec<? extends OfferingAction>> entry : ACTION_TYPES.entrySet()) {
            if (entry.getValue() == codec) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("Unknown action codec: " + codec);
    }

    /**
     * ResourceLocation から対応する Codec を取得する。
     *
     * @param id
     *            アクションタイプの ID
     * @return 対応する Codec
     */
    private static MapCodec<? extends OfferingAction> getCodec(ResourceLocation id) {
        MapCodec<? extends OfferingAction> codec = ACTION_TYPES.get(id);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown action type: " + id);
        }
        return codec;
    }
}
