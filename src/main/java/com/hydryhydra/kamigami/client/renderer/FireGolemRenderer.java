package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.client.util.TextureProcessor;
import com.hydryhydra.kamigami.entity.FireGolemEntity;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Fire Golem entity. Uses the vanilla Iron Golem model with a
 * dynamically generated red/fire-themed texture. The texture is created by
 * processing the vanilla iron golem texture with a red hue shift.
 */
public class FireGolemRenderer extends MobRenderer<FireGolemEntity, IronGolemRenderState, IronGolemModel> {
    private final ResourceLocation textureLocation;

    public FireGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);

        // Create red-tinted version of iron golem texture dynamically
        // Process vanilla iron golem texture: shift hue to red/orange tones
        this.textureLocation = TextureProcessor.createFireGolemTexture("fire_golem");
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolemRenderState state) {
        return textureLocation;
    }
}
