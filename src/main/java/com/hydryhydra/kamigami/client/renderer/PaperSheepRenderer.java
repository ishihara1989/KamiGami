package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.entity.PaperSheepEntity;

import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Paper Sheep Shikigami
 * Uses vanilla sheep model with custom white texture
 */
public class PaperSheepRenderer extends MobRenderer<PaperSheepEntity, SheepRenderState, SheepModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/paper_sheep.png");

    public PaperSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), 0.7F);
    }

    @Override
    public SheepRenderState createRenderState() {
        return new SheepRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(SheepRenderState state) {
        return TEXTURE;
    }
}
