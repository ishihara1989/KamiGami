package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.entity.TatariSlimeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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
    protected float getShadowRadius(SlimeRenderState state) {
        return state.size * 0.25F;
    }

    @Override
    protected void scale(SlimeRenderState state, PoseStack poseStack) {
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float size = state.size;
        float squishFactor = state.squish / (size * 0.5F + 1.0F);
        float inverse = 1.0F / (squishFactor + 1.0F);
        poseStack.scale(inverse * size, 1.0F / inverse * size, inverse * size);
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    @Override
    public void extractRenderState(TatariSlimeEntity entity, SlimeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.size = entity.getSize();
        state.squish = Mth.lerp(partialTick, entity.oSquish, entity.squish);
    }

    @Override
    public ResourceLocation getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }
}
