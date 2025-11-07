package com.hydryhydra.kamigami.client.renderer;

import com.hydryhydra.kamigami.block.entity.ShrineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ShrineBlockEntityRenderer implements BlockEntityRenderer<ShrineBlockEntity, ShrineBlockEntityRenderer.ShrineRenderState> {

    private final ItemModelResolver itemModelResolver;

    public static class ShrineRenderState extends BlockEntityRenderState {
        public final ItemStackRenderState itemState = new ItemStackRenderState();
        public boolean hasItem = false;
        public Direction facing = Direction.NORTH;
    }

    public ShrineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public ShrineRenderState createRenderState() {
        return new ShrineRenderState();
    }

    @Override
    public void extractRenderState(ShrineBlockEntity blockEntity, ShrineRenderState renderState, float partialTick,
                                   Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        // Call super to extract base properties
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);

        // Extract custom data
        ItemStack storedItem = blockEntity.getStoredItem();
        renderState.hasItem = !storedItem.isEmpty();
        renderState.facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);

        if (renderState.hasItem) {
            // Update item render state using the model resolver
            this.itemModelResolver.updateForTopItem(
                renderState.itemState,
                storedItem,
                ItemDisplayContext.FIXED,
                blockEntity.getLevel(),
                null,
                (int) blockEntity.getBlockPos().asLong()
            );
        }
    }

    @Override
    public void submit(ShrineRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector,
                      CameraRenderState cameraRenderState) {
        if (!renderState.hasItem) {
            return;
        }

        poseStack.pushPose();

        // Center the item in the shrine
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on facing direction (north face is the opening)
        // The item should face the opening (north side)
        float yRotation = switch (renderState.facing) {
            case NORTH -> 0F;      // Opening facing north
            case SOUTH -> 180F;    // Opening facing south
            case WEST -> 90F;      // Opening facing west
            case EAST -> -90F;     // Opening facing east
            default -> 0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));

        // Move the item forward to be in front of the opening
        // Based on model, the opening is on the north face (Z=2-4)
        // So we position the item at Z=0.25 (forward from center)
        poseStack.translate(0.0, 0.0, -0.25);

        // Tilt the item slightly forward (bottom tilts toward viewer)
        // Rotate around X axis, with positive rotation tilting bottom forward
        poseStack.mulPose(Axis.XP.rotationDegrees(10F));

        // Scale down the item
        poseStack.scale(0.5F, 0.5F, 0.5F);

        // Submit the item for rendering
        renderState.itemState.submit(poseStack, nodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
