package com.hydryhydra.kamigami.offering;

import com.mojang.serialization.MapCodec;

/**
 * 祠へのお供え物や祠の破壊時に実行されるアクションの基底インターフェース。
 *
 * 全てのアクションは Codec でシリアライズ可能であり、JSON から読み込める。
 * アクションは合成可能（SequenceAction など）で、複雑な効果を表現できる。
 */
public interface OfferingAction {
    /**
     * アクションを実行する。
     *
     * @param ctx 実行コンテキスト（ワールド、座標、プレイヤー、アイテム、乱数など）
     * @return アクションが何らかの効果を発揮した場合は true
     */
    boolean perform(ActionContext ctx);

    /**
     * このアクションの Codec を返す。
     * JSON シリアライゼーションに使用される。
     *
     * @return アクションの MapCodec
     */
    MapCodec<? extends OfferingAction> codec();
}
