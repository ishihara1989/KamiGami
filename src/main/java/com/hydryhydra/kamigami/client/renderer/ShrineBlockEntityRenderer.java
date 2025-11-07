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

public class ShrineBlockEntityRenderer
        implements
            BlockEntityRenderer<ShrineBlockEntity, ShrineBlockEntityRenderer.ShrineRenderState> {

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
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition,
                breakProgress);

        // Extract custom data
        ItemStack storedItem = blockEntity.getStoredItem();
        renderState.hasItem = !storedItem.isEmpty();
        renderState.facing = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);

        if (renderState.hasItem) {
            // Update item render state using the model resolver
            this.itemModelResolver.updateForTopItem(renderState.itemState, storedItem, ItemDisplayContext.FIXED,
                    blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(ShrineRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector,
            CameraRenderState cameraRenderState) {
        if (!renderState.hasItem) {
            return;
        }

        poseStack.pushPose();

        // Anchor to the center of the shrine cavity so the item feels embedded inside
        // the niche
        poseStack.translate(0.5F, 0.45F, 0.5F);

        float yRotation = switch (renderState.facing) {
            case NORTH -> 0F;
            case SOUTH -> 180F;
            case WEST -> 90F;
            case EAST -> -90F;
            default -> 0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));

        // Pull the item slightly toward the opening and lower it so it rests in the
        // void area
        poseStack.translate(0.0F, -0.05F, -0.18F);

        // Gentle forward tilt keeps the face visible without sticking out of the shrine
        poseStack.mulPose(Axis.XP.rotationDegrees(7.5F));

        // Slightly smaller scale keeps tools/blocks from clipping through the frame
        poseStack.scale(0.42F, 0.42F, 0.42F);

        renderState.itemState.submit(poseStack, nodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
