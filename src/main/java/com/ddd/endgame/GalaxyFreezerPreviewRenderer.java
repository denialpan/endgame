package com.ddd.endgame;

import com.ddd.endgame.block.GalaxyFreezerMultiblock;
import com.ddd.endgame.block.HorizontalFacingEntityBlock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

public final class GalaxyFreezerPreviewRenderer {
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    private static final int DETECTION_RADIUS_BLOCKS = 32;
    private static final int DETECTION_RADIUS_BLOCKS_SQUARED = DETECTION_RADIUS_BLOCKS * DETECTION_RADIUS_BLOCKS;
    private static final long DETECTION_SCAN_INTERVAL_TICKS = 10L;
    private static final float MODEL_ALPHA = 0.42F;
    private static PreviewTarget cachedNearbyTarget;
    private static long nextNearbyScanTick = Long.MIN_VALUE;

    private GalaxyFreezerPreviewRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || !isPreviewItem(player.getMainHandItem()) && !isPreviewItem(player.getOffhandItem())) {
            return;
        }

        PreviewTarget target = findTarget(minecraft, level, player);
        if (target == null) {
            return;
        }

        List<GalaxyFreezerMultiblock.PreviewBlock> previewBlocks = GalaxyFreezerMultiblock.previewBlocks(target.controllerPos(), target.facing());
        if (previewBlocks.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        MultiBufferSource previewBuffer = new TranslucentPreviewBufferSource(bufferSource, MODEL_ALPHA);
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        for (GalaxyFreezerMultiblock.PreviewBlock previewBlock : previewBlocks) {
            if (previewBlock.matches(level.getBlockState(previewBlock.pos()))) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(
                    previewBlock.pos().getX() - camera.x(),
                    previewBlock.pos().getY() - camera.y(),
                    previewBlock.pos().getZ() - camera.z()
            );
            dispatcher.renderSingleBlock(previewBlock.state(), poseStack, previewBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        bufferSource.endBatch(RenderType.translucent());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static boolean isPreviewItem(ItemStack stack) {
        return stack.is(WRENCHES) || stack.is(dddsendgame.GALAXY_FREEZER_ITEM.get());
    }

    private static PreviewTarget findTarget(Minecraft minecraft, Level level, Player player) {
        boolean holdingFreezer = player.getMainHandItem().is(dddsendgame.GALAXY_FREEZER_ITEM.get()) || player.getOffhandItem().is(dddsendgame.GALAXY_FREEZER_ITEM.get());
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return findNearbyFreezer(level, player);
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.is(dddsendgame.GALAXY_FREEZER_BLOCK.get())) {
            return new PreviewTarget(hitPos, hitState.getValue(HorizontalFacingEntityBlock.FACING));
        }

        if (holdingFreezer) {
            return new PreviewTarget(hitPos.relative(hitResult.getDirection()), player.getDirection().getOpposite());
        }
        return findNearbyFreezer(level, player);
    }

    private static PreviewTarget findNearbyFreezer(Level level, Player player) {
        long gameTime = level.getGameTime();
        if (gameTime < nextNearbyScanTick && cachedNearbyTarget != null && isNearbyTargetValid(level, player, cachedNearbyTarget)) {
            return cachedNearbyTarget;
        }

        nextNearbyScanTick = gameTime + DETECTION_SCAN_INTERVAL_TICKS;
        cachedNearbyTarget = scanNearbyFreezer(level, player);
        return cachedNearbyTarget;
    }

    private static boolean isNearbyTargetValid(Level level, Player player, PreviewTarget target) {
        if (target.controllerPos().distToCenterSqr(player.position()) > DETECTION_RADIUS_BLOCKS_SQUARED) {
            return false;
        }
        BlockState state = level.getBlockState(target.controllerPos());
        return state.is(dddsendgame.GALAXY_FREEZER_BLOCK.get());
    }

    private static PreviewTarget scanNearbyFreezer(Level level, Player player) {
        BlockPos center = player.blockPosition();
        PreviewTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -DETECTION_RADIUS_BLOCKS; y <= DETECTION_RADIUS_BLOCKS; y++) {
            for (int z = -DETECTION_RADIUS_BLOCKS; z <= DETECTION_RADIUS_BLOCKS; z++) {
                for (int x = -DETECTION_RADIUS_BLOCKS; x <= DETECTION_RADIUS_BLOCKS; x++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    double distance = mutablePos.distToCenterSqr(player.position());
                    if (distance > DETECTION_RADIUS_BLOCKS_SQUARED || distance >= nearestDistance) {
                        continue;
                    }

                    BlockState state = level.getBlockState(mutablePos);
                    if (state.is(dddsendgame.GALAXY_FREEZER_BLOCK.get())) {
                        nearestDistance = distance;
                        nearest = new PreviewTarget(mutablePos.immutable(), state.getValue(HorizontalFacingEntityBlock.FACING));
                    }
                }
            }
        }
        return nearest;
    }

    private record PreviewTarget(BlockPos controllerPos, Direction facing) {
    }

    private record TranslucentPreviewBufferSource(MultiBufferSource delegate, float alpha) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new AlphaVertexConsumer(this.delegate.getBuffer(RenderType.translucent()), this.alpha);
        }
    }

    private static class AlphaVertexConsumer extends VertexConsumerWrapper {
        private final int alpha;

        private AlphaVertexConsumer(VertexConsumer parent, float alpha) {
            super(parent);
            this.alpha = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return super.setColor(red, green, blue, Math.min(alpha, this.alpha));
        }
    }
}
