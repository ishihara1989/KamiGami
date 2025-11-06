package com.hydryhydra.kamigami.client.renderer.state;

import net.minecraft.client.renderer.entity.state.SheepRenderState;

/**
 * Render state for Paper Sheep
 * Uses vanilla SheepRenderState which extends LivingEntityRenderState
 * and includes shear status and wool color information
 */
public class PaperSheepRenderState extends SheepRenderState {
    // Uses vanilla SheepRenderState which already includes:
    // - boolean isSheared
    // - DyeColor woolColor
    // - boolean isJebSheep
    // - float headEatPositionScale
    // - float headEatAngleScale
}
