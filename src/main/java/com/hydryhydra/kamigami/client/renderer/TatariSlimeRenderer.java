package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.entity.TatariSlimeEntity;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Tatari Slime entity (black slime-like hostile mob)
 */
public class TatariSlimeRenderer extends MobRenderer<TatariSlimeEntity, SlimeRenderState, SlimeModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/tatari_slime.png");

    public TatariSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    @Override
    public void extractRenderState(TatariSlimeEntity entity, SlimeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.size = entity.getSize();
        state.squish = entity.squish;
    }

    @Override
    public ResourceLocation getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }
}
