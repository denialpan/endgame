package com.ddd.endgame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class RandomBlockPlacerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final RandomBlockPlacerItemRenderer INSTANCE = new RandomBlockPlacerItemRenderer();
    private static final long CYCLE_MILLIS = 500L;
    private static List<BlockState> cachedRenderableBlocks;

    private RandomBlockPlacerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = currentBlockState();
        if (state != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private static BlockState currentBlockState() {
        List<BlockState> states = renderableBlocks();
        if (states.isEmpty()) {
            return null;
        }

        long cycle = Math.floorDiv(System.currentTimeMillis(), CYCLE_MILLIS);
        return states.get(randomIndex(cycle, states.size()));
    }

    private static int randomIndex(long cycle, int size) {
        long mixed = cycle ^ 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed = mixed ^ (mixed >>> 31);
        return Math.floorMod(mixed, size);
    }

    private static List<BlockState> renderableBlocks() {
        if (cachedRenderableBlocks != null) {
            return cachedRenderableBlocks;
        }

        List<BlockState> states = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            BlockState state = block.defaultBlockState();
            if (!state.isAir()
                    && block.asItem() != Items.AIR
                    && state.getRenderShape() == RenderShape.MODEL
                    && block.isEnabled(Minecraft.getInstance().level == null
                    ? net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS
                    : Minecraft.getInstance().level.enabledFeatures())) {
                states.add(state);
            }
        });
        cachedRenderableBlocks = List.copyOf(states);
        return cachedRenderableBlocks;
    }
}
