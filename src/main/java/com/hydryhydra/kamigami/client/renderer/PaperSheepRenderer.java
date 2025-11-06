package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.client.renderer.layer.PaperSheepFurLayer;
import com.hydryhydra.kamigami.client.renderer.state.PaperSheepRenderState;
import com.hydryhydra.kamigami.entity.PaperSheepEntity;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Renderer for Paper Sheep Shikigami
 * Uses vanilla sheep model with custom white texture
 * Updated for NeoForge 1.21.10 RenderState system
 */
public class PaperSheepRenderer extends MobRenderer<PaperSheepEntity, PaperSheepRenderState, SheepModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/sheep/paper_sheep.png");

    public PaperSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), 0.7F);
        this.addLayer(new PaperSheepFurLayer(this, context.getModelSet()));
    }

    @Override
    public PaperSheepRenderState createRenderState() {
        return new PaperSheepRenderState();
    }

    @Override
    public void extractRenderState(PaperSheepEntity entity, PaperSheepRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Set sheep-specific render state
        state.isSheared = entity.isSheared();
        state.woolColor = DyeColor.WHITE; // Paper sheep are always white
        state.isJebSheep = false; // Paper sheep don't support jeb_ easter egg
        // Note: headEatPositionScale and headEatAngleScale would need methods in PaperSheepEntity
    }

    @Override
    public ResourceLocation getTextureLocation(PaperSheepRenderState state) {
        return TEXTURE;
    }
}