package com.ddd.endgame;

import com.ddd.endgame.item.RandomBlockPlacerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RandomBlockPlacerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final RandomBlockPlacerItemRenderer INSTANCE = new RandomBlockPlacerItemRenderer();

    private RandomBlockPlacerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Item item = RandomBlockPlacerItem.selectedItem(stack);
        if (item instanceof RandomBlockPlacerItem) {
            return;
        }
        if (!(item instanceof BlockItem blockItem)) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            minecraft.getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffer, minecraft.level, 0);
            poseStack.popPose();
            return;
        }

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        if (state.isAir()) {
            return;
        }
        poseStack.pushPose();
        applyBlockDisplayTransform(displayContext, poseStack);
        minecraft.getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyBlockDisplayTransform(ItemDisplayContext displayContext, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyInverseTransform(generatedTransform(displayContext), poseStack);
        applyTransform(blockTransform(displayContext), poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static Transform generatedTransform(ItemDisplayContext displayContext) {
        return switch (displayContext) {
            case GROUND -> new Transform(0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.5F);
            case FIXED -> new Transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            case THIRD_PERSON_RIGHT_HAND -> new Transform(0.0F, -90.0F, 55.0F, 0.0F, 4.0F, 0.5F, 0.85F);
            case THIRD_PERSON_LEFT_HAND -> new Transform(0.0F, 90.0F, -55.0F, 0.0F, 4.0F, 0.5F, 0.85F);
            case FIRST_PERSON_RIGHT_HAND -> new Transform(0.0F, -90.0F, 25.0F, 1.13F, 3.2F, 1.13F, 0.68F);
            case FIRST_PERSON_LEFT_HAND -> new Transform(0.0F, 90.0F, -25.0F, 1.13F, 3.2F, 1.13F, 0.68F);
            default -> Transform.IDENTITY;
        };
    }

    private static Transform blockTransform(ItemDisplayContext displayContext) {
        return switch (displayContext) {
            case GUI -> new Transform(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
            case GROUND -> new Transform(0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.25F);
            case FIXED -> new Transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> new Transform(75.0F, 45.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F);
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> new Transform(0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
            default -> Transform.IDENTITY;
        };
    }

    private static void applyTransform(Transform transform, PoseStack poseStack) {
        poseStack.translate(transform.translation.x(), transform.translation.y(), transform.translation.z());
        poseStack.mulPose(transform.rotation());
        poseStack.scale(transform.scale, transform.scale, transform.scale);
    }

    private static void applyInverseTransform(Transform transform, PoseStack poseStack) {
        if (transform == Transform.IDENTITY) {
            return;
        }
        float inverseScale = 1.0F / transform.scale;
        poseStack.scale(inverseScale, inverseScale, inverseScale);
        poseStack.mulPose(transform.rotation().conjugate());
        poseStack.translate(-transform.translation.x(), -transform.translation.y(), -transform.translation.z());
    }

    private record Transform(float xRot, float yRot, float zRot, Vector3f translation, float scale) {
        private static final Transform IDENTITY = new Transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);

        private Transform(float xRot, float yRot, float zRot, float x, float y, float z, float scale) {
            this(xRot, yRot, zRot, new Vector3f(x / 16.0F, y / 16.0F, z / 16.0F), scale);
        }

        private Quaternionf rotation() {
            return new Quaternionf().rotationXYZ(
                    this.xRot * (float) (Math.PI / 180.0),
                    this.yRot * (float) (Math.PI / 180.0),
                    this.zRot * (float) (Math.PI / 180.0)
            );
        }
    }
}
