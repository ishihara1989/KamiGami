package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.entity.TatariFertilityEntity;

import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Tatari of Fertility Deity entity (4-block tall tree-like entity
 * with Jack-o-Lantern face)
 */
public class TatariFertilityRenderer extends MobRenderer<TatariFertilityEntity, IronGolemRenderState, IronGolemModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/tatari_fertility.png");

    public TatariFertilityRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolemRenderState state) {
        return TEXTURE;
    }
}
