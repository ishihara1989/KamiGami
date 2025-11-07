package com.hydryhydra.kamigami.client;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.client.renderer.PaperChickenRenderer;
import com.hydryhydra.kamigami.client.renderer.PaperCowRenderer;
import com.hydryhydra.kamigami.client.renderer.PaperSheepRenderer;
import com.hydryhydra.kamigami.client.renderer.ShrineBlockEntityRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-side setup for entity renderers
 */
@EventBusSubscriber(modid = KamiGami.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register custom renderers for shikigami entities
        event.registerEntityRenderer(KamiGami.PAPER_COW.get(), PaperCowRenderer::new);
        event.registerEntityRenderer(KamiGami.PAPER_CHICKEN.get(), PaperChickenRenderer::new);
        event.registerEntityRenderer(KamiGami.PAPER_SHEEP.get(), PaperSheepRenderer::new);

        // Register block entity renderers
        event.registerBlockEntityRenderer(KamiGami.SHRINE_BLOCK_ENTITY.get(), ShrineBlockEntityRenderer::new);
    }
}
