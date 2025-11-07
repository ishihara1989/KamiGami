package com.hydryhydra.kamigami.client.renderer.layer;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.client.renderer.state.PaperSheepRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;

/**
 * Render layer for Paper Sheep wool/fur Updated for NeoForge 1.21.10
 * RenderState system Based on vanilla SheepWoolLayer
 */
public class PaperSheepFurLayer extends RenderLayer<PaperSheepRenderState, SheepModel> {
    private static final ResourceLocation FUR_TEXTURE = ResourceLocation.fromNamespaceAndPath(KamiGami.MODID,
            "textures/entity/sheep/paper_sheep_fur.png");

    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public PaperSheepFurLayer(RenderLayerParent<PaperSheepRenderState, SheepModel> parent, EntityModelSet modelSet) {
        super(parent);
        // Use vanilla sheep fur models for adult and baby
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight,
            PaperSheepRenderState renderState, float yRot, float xRot) {
        // Only render fur if not sheared
        if (!renderState.isSheared) {
            // Use baby or adult model based on age
            EntityModel<SheepRenderState> model = renderState.isBaby ? this.babyModel : this.adultModel;

            // Paper sheep wool is always white
            int woolColor = renderState.getWoolColor();

            // Render the wool layer with white color
            coloredCutoutModelCopyLayerRender(model, FUR_TEXTURE, poseStack, nodeCollector, packedLight, renderState,
                    woolColor, 0);
        }
    }
}
