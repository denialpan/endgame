package com.ddd.endgame;

import com.ddd.endgame.block.GalaxyFreezerMultiblock;
import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import com.ddd.endgame.block.HorizontalFacingEntityBlock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.lwjgl.opengl.GL11;

public final class GalaxyFreezerPreviewRenderer {
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    private static final int DETECTION_RADIUS_BLOCKS = 32;
    private static final int DETECTION_RADIUS_BLOCKS_SQUARED = DETECTION_RADIUS_BLOCKS * DETECTION_RADIUS_BLOCKS;
    private static final float COMPLETE_MODEL_ALPHA = 0.18F;
    private static final float MISSING_MODEL_ALPHA = 0.42F;
    private static final MultiBufferSource.BufferSource IMMEDIATE = MultiBufferSource.immediate(new ByteBufferBuilder(256));
    private static final Map<BlockPos, PreviewTarget> QUEUED_TARGETS = new LinkedHashMap<>();

    private GalaxyFreezerPreviewRenderer() {
    }

    public static void enqueue(GalaxyFreezerBlockEntity freezer) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || freezer.getLevel() == null || !isHoldingPreviewItem(player)) {
            return;
        }

        BlockState state = freezer.getBlockState();
        if (!state.is(dddsendgame.GALAXY_FREEZER_BLOCK.get())) {
            return;
        }

        BlockPos pos = freezer.getBlockPos();
        QUEUED_TARGETS.put(pos.immutable(), new PreviewTarget(pos.immutable(), state.getValue(HorizontalFacingEntityBlock.FACING)));
    }

    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || !isHoldingPreviewItem(player)) {
            QUEUED_TARGETS.clear();
            return;
        }

        Map<BlockPos, PreviewTarget> targets = new LinkedHashMap<>(QUEUED_TARGETS);
        QUEUED_TARGETS.clear();
        PreviewTarget placementTarget = findPlacementTarget(minecraft, player);
        if (placementTarget != null && targets.isEmpty()) {
            targets.put(placementTarget.controllerPos(), placementTarget);
        }

        if (targets.isEmpty()) {
            return;
        }

        if (targets.values().stream().noneMatch(target -> hasVisiblePreviewBlock(level, player, target))) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        poseStack.pushPose();
        poseStack.mulPose(event.getModelViewMatrix());
        for (PreviewTarget target : targets.values()) {
            List<GalaxyFreezerMultiblock.PreviewBlock> previewBlocks = GalaxyFreezerMultiblock.previewBlocks(target.controllerPos(), target.facing());
            for (GalaxyFreezerMultiblock.PreviewBlock previewBlock : previewBlocks) {
                if (!isPreviewBlockInRange(player, previewBlock.pos())) {
                    continue;
                }

                boolean complete = previewBlock.matches(level.getBlockState(previewBlock.pos()));
                MultiBufferSource previewBuffer = new TranslucentPreviewBufferSource(
                        IMMEDIATE,
                        complete ? COMPLETE_MODEL_ALPHA : MISSING_MODEL_ALPHA
                );

                poseStack.pushPose();
                poseStack.translate(
                        previewBlock.pos().getX() - camera.x(),
                        previewBlock.pos().getY() - camera.y(),
                        previewBlock.pos().getZ() - camera.z()
                );
                dispatcher.renderSingleBlock(previewBlock.state(), poseStack, previewBuffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        IMMEDIATE.endBatch(RenderType.translucent());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static boolean isHoldingPreviewItem(Player player) {
        return isPreviewItem(player.getMainHandItem()) || isPreviewItem(player.getOffhandItem());
    }

    private static boolean isPreviewItem(ItemStack stack) {
        return stack.is(WRENCHES) || stack.is(dddsendgame.GALAXY_FREEZER_ITEM.get());
    }

    private static PreviewTarget findPlacementTarget(Minecraft minecraft, Player player) {
        boolean holdingFreezer = isHoldingFreezer(player);
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        if (holdingFreezer) {
            return new PreviewTarget(hitResult.getBlockPos().relative(hitResult.getDirection()), player.getDirection().getOpposite());
        }
        return null;
    }

    private static boolean isHoldingFreezer(Player player) {
        return player.getMainHandItem().is(dddsendgame.GALAXY_FREEZER_ITEM.get()) || player.getOffhandItem().is(dddsendgame.GALAXY_FREEZER_ITEM.get());
    }

    private static boolean hasVisiblePreviewBlock(Level level, Player player, PreviewTarget target) {
        return GalaxyFreezerMultiblock.previewBlocks(target.controllerPos(), target.facing()).stream()
                .anyMatch(previewBlock -> isPreviewBlockInRange(player, previewBlock.pos()));
    }

    private static boolean isPreviewBlockInRange(Player player, BlockPos pos) {
        return player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= DETECTION_RADIUS_BLOCKS_SQUARED;
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
