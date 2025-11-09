package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.client.KamiGamiModelLayers;
import com.hydryhydra.kamigami.client.model.TatariTreeModel;
import com.hydryhydra.kamigami.entity.TatariFertilityEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Tatari of Fertility Deity entity (6-block tall tree-like entity
 * with Jack-o-Lantern face and branches with leaves)
 */
public class TatariFertilityRenderer
        extends
            MobRenderer<TatariFertilityEntity, TatariTreeRenderState, TatariTreeModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/tatari_tree/tatari_tree.png");

    public TatariFertilityRenderer(EntityRendererProvider.Context context) {
        super(context, new TatariTreeModel(context.bakeLayer(KamiGamiModelLayers.TATARI_TREE)), 1.0F);
    }

    @Override
    public TatariTreeRenderState createRenderState() {
        return new TatariTreeRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(TatariTreeRenderState state) {
        return TEXTURE;
    }
}
