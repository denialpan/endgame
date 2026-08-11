package com.ddd.endgame;

import com.ddd.endgame.item.RandomBlockPlacerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RandomBlockPlacerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final RandomBlockPlacerItemRenderer INSTANCE = new RandomBlockPlacerItemRenderer();

    private RandomBlockPlacerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Block block = RandomBlockPlacerItem.selectedBlock(stack);
        BlockState state = block.defaultBlockState();
        if (state.isAir()) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
    }
}
