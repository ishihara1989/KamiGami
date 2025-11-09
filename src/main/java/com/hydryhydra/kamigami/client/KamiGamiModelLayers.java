package com.hydryhydra.kamigami.client;

import com.hydryhydra.kamigami.KamiGami;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Model layer locations for KamiGami mod
 */
public class KamiGamiModelLayers {
    /**
     * Model layer for Tatari Tree (Tatari Fertility Deity)
     */
    public static final ModelLayerLocation TATARI_TREE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KamiGami.MODID, "tatari_tree"), "main");
}
