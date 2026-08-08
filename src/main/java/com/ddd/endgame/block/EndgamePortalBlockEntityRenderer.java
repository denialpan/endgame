package com.ddd.endgame.block;

import com.ddd.endgame.Config;
import com.ddd.endgame.compat.IrisCompat;
import com.ddd.endgame.dddsendgame;
import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EndgamePortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final float BLOCK_MIN = 0.0F;
    private static final float BLOCK_MAX = 1.0F;
    private static final float DEPTH_MIN = 0.002F;
    private static final float DEPTH_MAX = 0.998F;
    private static final float SKYBOX_SIZE = 96.0F;
    private static final float FIXED_SKYBOX_FOV = 70.0F;
    private static final float ITEM_WINDOW_SCALE = 0.985F;
    private static final int BLOCK_STENCIL_REF = 1;
    private static final int ITEM_STENCIL_REF = 2;
    private static final long STATIC_CACHE_PRUNE_INTERVAL_TICKS = 200L;
    private static final long STATIC_CACHE_MAX_IDLE_TICKS = 400L;
    private static final float STATIC_POSE_TRANSLATION_EPSILON = 0.001F;
    private static final List<WindowMask> WINDOW_MASKS = new ArrayList<>();
    private static final List<WindowMask> ITEM_WINDOW_MASKS = new ArrayList<>();
    private static final Set<Long> WINDOW_MASK_KEYS = new HashSet<>();
    private static final Set<MatrixKey> ITEM_WINDOW_MASK_KEYS = new HashSet<>();
    private static final Map<Long, StaticWorldWindowMask> STATIC_WORLD_WINDOW_MASKS = new HashMap<>();
    private static Level staticCacheLevel;
    private static long lastStaticCachePruneTick;
    private static int lastStencilClearRenderTick = Integer.MIN_VALUE;
    private static float lastStencilClearPartialTick = Float.NaN;
    private static int lastDistantAnimationRenderTick = Integer.MIN_VALUE;
    private static double lastDistantAnimationSeconds;
    private static int blockEntityWindowSkippedThisFrame;
    private static int lastBlockEntityWindowsQueued;
    private static int lastBlockEntityWindowsVisible;
    private static int lastBlockEntityWindowsCulled;
    private static int lastBlockEntityWindowsSkipped;
    private static boolean stencilEnabled;

    public EndgamePortalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return true;
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureStencil(minecraft);
        if (!stencilEnabled) {
            return;
        }

        BlockPos blockPos = blockEntity.getBlockPos();
        if (!WINDOW_MASK_KEYS.add(blockPos.asLong())) {
            return;
        }

        if (blockEntityWindowLimitReached()) {
            blockEntityWindowSkippedThisFrame++;
            return;
        }

        WindowMask mask = createBlockEntityWindowMask(blockEntity, poseStack.last().pose(), minecraft);
        WINDOW_MASKS.add(mask);
        if (!isDistant(mask.cameraRelativeBounds())) {
            renderWindowDepthMask(mask.pose());
        }
    }

    public static void renderSkyboxLayer(RenderLevelStageEvent event) {
        renderSkyboxLayer(event, WINDOW_MASKS, WINDOW_MASK_KEYS, BLOCK_STENCIL_REF, true, BLOCK_MIN, BLOCK_MAX);
    }

    public static void renderPhotonSkyboxLayer(RenderLevelStageEvent event) {
        renderSkyboxLayer(event, WINDOW_MASKS, WINDOW_MASK_KEYS, BLOCK_STENCIL_REF, true, DEPTH_MIN, DEPTH_MAX);
    }

    public static void discardSkyboxLayer() {
        WINDOW_MASKS.clear();
        WINDOW_MASK_KEYS.clear();
        updateBlockEntityWindowCounts(true, 0, 0);
    }

    public static void renderItemSkyboxLayer(RenderLevelStageEvent event) {
        renderSkyboxLayer(event, ITEM_WINDOW_MASKS, ITEM_WINDOW_MASK_KEYS, ITEM_STENCIL_REF, false, BLOCK_MIN, BLOCK_MAX);
    }

    private static void renderSkyboxLayer(
            RenderLevelStageEvent event,
            List<? extends WindowMask> queuedMasks,
            Set<?> queuedKeys,
            int stencilRef,
            boolean trackBlockEntityCount,
            float maskMin,
            float maskMax
    ) {
        if (!stencilEnabled) {
            ensureStencil(Minecraft.getInstance());
        }
        if (!stencilEnabled || queuedMasks.isEmpty()) {
            queuedKeys.clear();
            updateBlockEntityWindowCounts(trackBlockEntityCount, 0, 0);
            return;
        }

        List<? extends WindowMask> masks = new ArrayList<>(queuedMasks);
        queuedMasks.clear();
        queuedKeys.clear();

        List<? extends WindowMask> visibleMasks = masks.stream()
                .filter(mask -> shouldRenderMask(mask, event, !trackBlockEntityCount))
                .toList();
        updateBlockEntityWindowCounts(trackBlockEntityCount, masks.size(), visibleMasks.size());
        if (visibleMasks.isEmpty()) {
            return;
        }

        if (visibleMasks.stream().anyMatch(mask -> mask.tintGreenBlue() < 0.999F)) {
            renderTintedSkyboxLayer(event, visibleMasks, stencilRef, maskMin, maskMax);
            return;
        }

        prepareStencilForFrame(event);
        renderSkyboxMaskPass(event, visibleMasks, stencilRef, maskMin, maskMax, 1.0F);
        restoreAfterSkyboxLayer();
    }

    private static void renderTintedSkyboxLayer(
            RenderLevelStageEvent event,
            List<? extends WindowMask> visibleMasks,
            int stencilRef,
            float maskMin,
            float maskMax
    ) {
        int groupRef = stencilRef;
        Map<Integer, List<WindowMask>> groupedMasks = new HashMap<>();
        for (WindowMask mask : visibleMasks) {
            int tintKey = Math.round(mask.tintGreenBlue() * 255.0F);
            groupedMasks.computeIfAbsent(tintKey, ignored -> new ArrayList<>()).add(mask);
        }

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        for (Map.Entry<Integer, List<WindowMask>> entry : groupedMasks.entrySet()) {
            RenderSystem.stencilMask(0xFF);
            RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
            renderSkyboxMaskPass(event, entry.getValue(), groupRef, maskMin, maskMax, entry.getKey() / 255.0F);
        }

        restoreAfterSkyboxLayer();
    }

    private static void renderSkyboxMaskPass(
            RenderLevelStageEvent event,
            List<? extends WindowMask> visibleMasks,
            int stencilRef,
            float maskMin,
            float maskMax,
            float greenBlue
    ) {
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, stencilRef, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        PoseStack poseStack = event.getPoseStack();
        BufferBuilder maskBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (WindowMask mask : visibleMasks) {
            mask.append(maskBuilder, maskMin, maskMax);
        }
        BufferUploader.drawWithShader(maskBuilder.buildOrThrow());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, stencilRef, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        poseStack.pushPose();
        applyConfiguredSkyboxRotation(poseStack, skyboxAnimationSeconds(event, visibleMasks));
        withFixedSkyboxProjection(() -> renderSkyboxCube(poseStack.last().pose(), SKYBOX_SIZE, greenBlue));
        poseStack.popPose();
    }

    public static void registerWindowMask(Matrix4f pose) {
        registerWindowMask(pose, 1.0F);
    }

    public static void registerWindowMask(Matrix4f pose, float greenBlue) {
        if (!Config.DROPPED_ITEM_WINDOWS.getAsBoolean() || IrisCompat.isRenderingShadowPass()) {
            return;
        }

        Matrix4f itemPose = new Matrix4f(pose);
        itemPose.translate(0.5F, 0.5F, 0.5F);
        itemPose.scale(ITEM_WINDOW_SCALE);
        itemPose.translate(-0.5F, -0.5F, -0.5F);
        if (tooFar(transformedUnitCubeBounds(itemPose))) {
            return;
        }
        if (!ITEM_WINDOW_MASK_KEYS.add(MatrixKey.from(itemPose))) {
            return;
        }
        ITEM_WINDOW_MASKS.add(new DynamicWindowMask(itemPose, transformedUnitCubeBounds(itemPose), greenBlue));
    }

    public static void registerPixelWindowMask(Matrix4f pose, boolean[][] pixels, int pixelCount, float frontZ, float backZ) {
        registerPixelWindowMask(pose, pixels, pixelCount, frontZ, backZ, 1.0F);
    }

    public static void registerPixelWindowMask(Matrix4f pose, boolean[][] pixels, int pixelCount, float frontZ, float backZ, float greenBlue) {
        if (!Config.DROPPED_ITEM_WINDOWS.getAsBoolean() || IrisCompat.isRenderingShadowPass()) {
            return;
        }

        Matrix4f itemPose = new Matrix4f(pose);
        if (tooFar(transformedUnitCubeBounds(itemPose))) {
            return;
        }
        if (!ITEM_WINDOW_MASK_KEYS.add(MatrixKey.from(itemPose))) {
            return;
        }
        ITEM_WINDOW_MASKS.add(new PixelWindowMask(itemPose, transformedUnitCubeBounds(itemPose), pixels, pixelCount, frontZ, backZ, greenBlue));
    }

    public static int lastBlockEntityWindowsQueued() {
        return lastBlockEntityWindowsQueued;
    }

    public static int lastBlockEntityWindowsVisible() {
        return lastBlockEntityWindowsVisible;
    }

    public static int lastBlockEntityWindowsCulled() {
        return lastBlockEntityWindowsCulled;
    }

    public static int lastBlockEntityWindowsSkipped() {
        return lastBlockEntityWindowsSkipped;
    }

    private static void ensureStencil(Minecraft minecraft) {
        if (!stencilEnabled) {
            minecraft.getMainRenderTarget().enableStencil();
            stencilEnabled = minecraft.getMainRenderTarget().isStencilEnabled();
        }
    }

    private static boolean blockEntityWindowLimitReached() {
        int maxWindows = Config.MAX_BLOCK_ENTITY_WINDOWS.get();
        return maxWindows > 0 && WINDOW_MASKS.size() >= maxWindows;
    }

    private static WindowMask createBlockEntityWindowMask(BlockEntity blockEntity, Matrix4f fallbackPose, Minecraft minecraft) {
        float greenBlue = blockEntityTintGreenBlue(blockEntity);
        if (blockEntity.getLevel() != minecraft.level || minecraft.level == null) {
            Matrix4f pose = new Matrix4f(fallbackPose);
            return new DynamicWindowMask(pose, transformedUnitCubeBounds(pose), greenBlue);
        }

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        BlockPos blockPos = blockEntity.getBlockPos();
        if (!isStaticWorldPose(fallbackPose, blockPos, cameraPosition)) {
            Matrix4f pose = new Matrix4f(fallbackPose);
            return new DynamicWindowMask(pose, transformedUnitCubeBounds(pose), greenBlue);
        }

        ensureStaticCacheLevel(minecraft.level);
        long key = blockPos.asLong();
        StaticWorldWindowMask mask = STATIC_WORLD_WINDOW_MASKS.computeIfAbsent(key, ignored -> new StaticWorldWindowMask(blockPos));
        mask.prepare(cameraPosition, minecraft.level.getGameTime(), greenBlue);
        pruneStaticWorldWindowMasks(minecraft.level.getGameTime());
        return mask;
    }

    private static float blockEntityTintGreenBlue(BlockEntity blockEntity) {
        if (blockEntity instanceof EndgameDecorativeBlockEntity decorativeBlockEntity
                && decorativeBlockEntity.getBlockState().is(dddsendgame.GALAXY_BLOCK.get())) {
            return decorativeBlockEntity.galaxyTintGreenBlue();
        }
        return 1.0F;
    }

    private static boolean isStaticWorldPose(Matrix4f pose, BlockPos blockPos, Vec3 cameraPosition) {
        float expectedX = (float)(blockPos.getX() - cameraPosition.x);
        float expectedY = (float)(blockPos.getY() - cameraPosition.y);
        float expectedZ = (float)(blockPos.getZ() - cameraPosition.z);
        return nearlyEqual(pose.m30(), expectedX)
                && nearlyEqual(pose.m31(), expectedY)
                && nearlyEqual(pose.m32(), expectedZ)
                && nearlyEqual(pose.m00(), 1.0F)
                && nearlyEqual(pose.m11(), 1.0F)
                && nearlyEqual(pose.m22(), 1.0F)
                && nearlyEqual(pose.m01(), 0.0F)
                && nearlyEqual(pose.m02(), 0.0F)
                && nearlyEqual(pose.m10(), 0.0F)
                && nearlyEqual(pose.m12(), 0.0F)
                && nearlyEqual(pose.m20(), 0.0F)
                && nearlyEqual(pose.m21(), 0.0F);
    }

    private static boolean nearlyEqual(float actual, float expected) {
        return Math.abs(actual - expected) <= STATIC_POSE_TRANSLATION_EPSILON;
    }

    private static void ensureStaticCacheLevel(Level level) {
        if (staticCacheLevel == level) {
            return;
        }

        STATIC_WORLD_WINDOW_MASKS.clear();
        staticCacheLevel = level;
        lastStaticCachePruneTick = 0L;
    }

    private static void pruneStaticWorldWindowMasks(long gameTime) {
        if (gameTime - lastStaticCachePruneTick < STATIC_CACHE_PRUNE_INTERVAL_TICKS) {
            return;
        }

        STATIC_WORLD_WINDOW_MASKS.values().removeIf(mask -> gameTime - mask.lastSeenGameTime > STATIC_CACHE_MAX_IDLE_TICKS);
        lastStaticCachePruneTick = gameTime;
    }

    private static void updateBlockEntityWindowCounts(boolean trackBlockEntityCount, int queuedCount, int visibleCount) {
        if (!trackBlockEntityCount) {
            return;
        }

        lastBlockEntityWindowsQueued = queuedCount;
        lastBlockEntityWindowsVisible = visibleCount;
        lastBlockEntityWindowsCulled = queuedCount - visibleCount;
        lastBlockEntityWindowsSkipped = blockEntityWindowSkippedThisFrame;
        blockEntityWindowSkippedThisFrame = 0;
    }

    private static void prepareStencilForFrame(RenderLevelStageEvent event) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        int renderTick = event.getRenderTick();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (lastStencilClearRenderTick == renderTick && Float.compare(lastStencilClearPartialTick, partialTick) == 0) {
            return;
        }

        RenderSystem.stencilMask(0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        lastStencilClearRenderTick = renderTick;
        lastStencilClearPartialTick = partialTick;
    }

    public static void applyConfiguredSkyboxRotation(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getTimer() == null) {
            applyConfiguredSkyboxRotation(poseStack, 0.0D);
            return;
        }

        applyConfiguredSkyboxRotation(poseStack, currentAnimationSeconds(minecraft, minecraft.getTimer().getGameTimeDeltaPartialTick(false)));
    }

    public static void withFixedSkyboxProjection(Runnable renderAction) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, minecraft.getWindow().getWidth());
        int height = Math.max(1, minecraft.getWindow().getHeight());
        Matrix4f fixedProjection = new Matrix4f().perspective(
                (float)Math.toRadians(FIXED_SKYBOX_FOV),
                (float)width / (float)height,
                0.05F,
                minecraft.gameRenderer.getDepthFar()
        );

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(fixedProjection, VertexSorting.DISTANCE_TO_ORIGIN);
        try {
            renderAction.run();
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    public static void renderGlobalSkybox() {
        renderGlobalSkybox(1.0F);
    }

    public static void renderGlobalSkybox(float greenBlue) {
        PoseStack skyboxPose = new PoseStack();
        applyConfiguredSkyboxRotation(skyboxPose);
        withFixedSkyboxProjection(() -> renderSkyboxCube(skyboxPose.last().pose(), SKYBOX_SIZE, greenBlue));
    }

    private static void applyConfiguredSkyboxRotation(PoseStack poseStack, double seconds) {
        double pitchSpeed = Config.PITCH_ROTATION_SPEED.get();
        double yawSpeed = Config.YAW_ROTATION_SPEED.get();
        double rollSpeed = Config.ROLL_ROTATION_SPEED.get();

        if (pitchSpeed != 0.0D) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotationDegrees(seconds, pitchSpeed)));
        }
        if (yawSpeed != 0.0D) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(seconds, yawSpeed)));
        }
        if (rollSpeed != 0.0D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees(seconds, rollSpeed)));
        }
    }

    private static float rotationDegrees(double seconds, double degreesPerSecond) {
        return (float) ((seconds * degreesPerSecond) % 360.0D);
    }

    private static double skyboxAnimationSeconds(RenderLevelStageEvent event, List<? extends WindowMask> visibleMasks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0D;
        }

        double nearestDistance = nearestMaskDistance(visibleMasks);
        double disableRotationDistance = Config.DISABLE_ROTATION_DISTANCE.get();
        if (disableRotationDistance > 0.0D && nearestDistance >= disableRotationDistance) {
            return 0.0D;
        }

        double seconds = currentAnimationSeconds(minecraft, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        double distantAnimationDistance = Config.DISTANT_ANIMATION_DISTANCE.get();
        int frameInterval = Config.DISTANT_ANIMATION_FRAME_INTERVAL.get();
        if (distantAnimationDistance <= 0.0D || nearestDistance < distantAnimationDistance || frameInterval <= 1) {
            return seconds;
        }

        int renderTick = event.getRenderTick();
        if (lastDistantAnimationRenderTick == Integer.MIN_VALUE || renderTick - lastDistantAnimationRenderTick >= frameInterval) {
            lastDistantAnimationRenderTick = renderTick;
            lastDistantAnimationSeconds = seconds;
        }

        return lastDistantAnimationSeconds;
    }

    private static double currentAnimationSeconds(Minecraft minecraft, float partialTick) {
        if (minecraft.level == null) {
            return 0.0D;
        }

        return (minecraft.level.getGameTime() + partialTick) / 20.0D;
    }

    private static double nearestMaskDistance(List<? extends WindowMask> masks) {
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;
        for (WindowMask mask : masks) {
            nearestDistanceSqr = Math.min(nearestDistanceSqr, distanceToBoundsSqr(mask.cameraRelativeBounds()));
        }
        return Math.sqrt(nearestDistanceSqr);
    }

    private static double distanceToBoundsSqr(AABB bounds) {
        double dx = axisDistanceToBounds(0.0D, bounds.minX, bounds.maxX);
        double dy = axisDistanceToBounds(0.0D, bounds.minY, bounds.maxY);
        double dz = axisDistanceToBounds(0.0D, bounds.minZ, bounds.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double axisDistanceToBounds(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }

    private static void renderWindowDepthMask(Matrix4f pose) {
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        appendWindowMask(builder, pose, DEPTH_MIN, DEPTH_MAX);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void appendWindowMask(BufferBuilder builder, Matrix4f pose, float min, float max) {
        appendMaskFace(builder, pose, min, max, min, max, max, max, max, max);
        appendMaskFace(builder, pose, min, max, max, min, min, min, min, min);
        appendMaskFace(builder, pose, max, max, max, min, min, max, max, min);
        appendMaskFace(builder, pose, min, min, min, max, min, max, max, min);
        appendMaskFace(builder, pose, min, max, min, min, min, min, max, max);
        appendMaskFace(builder, pose, min, max, max, max, max, max, min, min);
    }

    private static boolean shouldRenderMask(WindowMask mask, RenderLevelStageEvent event, boolean distanceCull) {
        AABB cameraRelativeBounds = mask.cameraRelativeBounds();
        if (distanceCull && tooFar(cameraRelativeBounds)) {
            return false;
        }

        Frustum frustum = event.getFrustum();
        if (frustum == null) {
            return true;
        }

        return frustum.isVisible(mask.worldBounds(event.getCamera().getPosition()));
    }

    private static boolean isDistant(AABB cameraRelativeBounds) {
        double distantAnimationDistance = Config.DISTANT_ANIMATION_DISTANCE.get();
        return distantAnimationDistance > 0.0D && distanceToBoundsSqr(cameraRelativeBounds) >= distantAnimationDistance * distantAnimationDistance;
    }

    private static boolean tooFar(AABB cameraRelativeBounds) {
        double maxDistance = Config.RENDER_DISTANCE.get();
        if (maxDistance <= 0.0D) {
            return false;
        }

        double centerX = (cameraRelativeBounds.minX + cameraRelativeBounds.maxX) * 0.5D;
        double centerY = (cameraRelativeBounds.minY + cameraRelativeBounds.maxY) * 0.5D;
        double centerZ = (cameraRelativeBounds.minZ + cameraRelativeBounds.maxZ) * 0.5D;
        double radiusX = (cameraRelativeBounds.maxX - cameraRelativeBounds.minX) * 0.5D;
        double radiusY = (cameraRelativeBounds.maxY - cameraRelativeBounds.minY) * 0.5D;
        double radiusZ = (cameraRelativeBounds.maxZ - cameraRelativeBounds.minZ) * 0.5D;
        double radius = Math.sqrt(radiusX * radiusX + radiusY * radiusY + radiusZ * radiusZ);
        double allowedDistance = maxDistance + radius;
        return centerX * centerX + centerY * centerY + centerZ * centerZ > allowedDistance * allowedDistance;
    }

    private static AABB transformedUnitCubeBounds(Matrix4f pose) {
        Vector3f point = new Vector3f();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    point.set((float)x, (float)y, (float)z);
                    pose.transformPosition(point);
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void restoreAfterSkyboxLayer() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void appendMaskFace(BufferBuilder builder, Matrix4f pose, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        builder.addVertex(pose, x0, y0, z0);
        builder.addVertex(pose, x1, y0, z1);
        builder.addVertex(pose, x1, y1, z2);
        builder.addVertex(pose, x0, y1, z3);
    }

    private static void appendPixelMaskQuad(BufferBuilder builder, Matrix4f pose, float minX, float minY, float maxX, float maxY, float frontZ, float backZ) {
        builder.addVertex(pose, minX, minY, frontZ);
        builder.addVertex(pose, maxX, minY, frontZ);
        builder.addVertex(pose, maxX, maxY, frontZ);
        builder.addVertex(pose, minX, maxY, frontZ);

        builder.addVertex(pose, minX, maxY, backZ);
        builder.addVertex(pose, maxX, maxY, backZ);
        builder.addVertex(pose, maxX, minY, backZ);
        builder.addVertex(pose, minX, minY, backZ);
    }

    private static void renderSkyboxCube(Matrix4f pose) {
        renderSkyboxCube(pose, SKYBOX_SIZE);
    }

    public static void renderSkyboxCube(Matrix4f pose, float size) {
        renderSkyboxCube(pose, size, 1.0F);
    }

    public static void renderSkyboxCube(Matrix4f pose, float size, float greenBlue) {
        RenderSystem.setShaderColor(1.0F, greenBlue, greenBlue, 1.0F);
        float s = size;
        renderSkyboxFace(CubemapFace.FRONT, pose, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        renderSkyboxFace(CubemapFace.BACK, pose, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s);
        renderSkyboxFace(CubemapFace.LEFT, pose, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        renderSkyboxFace(CubemapFace.RIGHT, pose, s, -s, s, s, -s, -s, s, s, -s, s, s, s);
        renderSkyboxFace(CubemapFace.TOP, pose, -s, s, s, s, s, s, s, s, -s, -s, s, -s);
        renderSkyboxFace(CubemapFace.BOTTOM, pose, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderSkyboxFace(
            CubemapFace face,
            Matrix4f pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3
    ) {
        RenderSystem.setShaderTexture(0, face.texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, x0, y0, z0).setUv(0.0F, 1.0F);
        builder.addVertex(pose, x1, y1, z1).setUv(1.0F, 1.0F);
        builder.addVertex(pose, x2, y2, z2).setUv(1.0F, 0.0F);
        builder.addVertex(pose, x3, y3, z3).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private enum CubemapFace {
        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        TOP("top"),
        BOTTOM("bottom");

        private final ResourceLocation texture;

        CubemapFace(String textureName) {
            this.texture = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/inner_skybox/" + textureName + ".png");
        }
    }

    private interface WindowMask {
        Matrix4f pose();

        AABB cameraRelativeBounds();

        AABB worldBounds(Vec3 cameraPosition);

        void append(BufferBuilder builder, float min, float max);

        float tintGreenBlue();
    }

    private record DynamicWindowMask(Matrix4f pose, AABB cameraRelativeBounds, float tintGreenBlue) implements WindowMask {
        @Override
        public AABB worldBounds(Vec3 cameraPosition) {
            return new AABB(
                    cameraRelativeBounds.minX + cameraPosition.x,
                    cameraRelativeBounds.minY + cameraPosition.y,
                    cameraRelativeBounds.minZ + cameraPosition.z,
                    cameraRelativeBounds.maxX + cameraPosition.x,
                    cameraRelativeBounds.maxY + cameraPosition.y,
                    cameraRelativeBounds.maxZ + cameraPosition.z
            );
        }

        @Override
        public void append(BufferBuilder builder, float min, float max) {
            appendWindowMask(builder, this.pose, min, max);
        }

        @Override
        public float tintGreenBlue() {
            return 1.0F;
        }
    }

    private record PixelWindowMask(Matrix4f pose, AABB cameraRelativeBounds, boolean[][] pixels, int pixelCount, float frontZ, float backZ, float tintGreenBlue) implements WindowMask {
        @Override
        public AABB worldBounds(Vec3 cameraPosition) {
            return new AABB(
                    cameraRelativeBounds.minX + cameraPosition.x,
                    cameraRelativeBounds.minY + cameraPosition.y,
                    cameraRelativeBounds.minZ + cameraPosition.z,
                    cameraRelativeBounds.maxX + cameraPosition.x,
                    cameraRelativeBounds.maxY + cameraPosition.y,
                    cameraRelativeBounds.maxZ + cameraPosition.z
            );
        }

        @Override
        public void append(BufferBuilder builder, float min, float max) {
            float pixel = 1.0F / this.pixelCount;
            for (int y = 0; y < this.pixelCount; y++) {
                for (int x = 0; x < this.pixelCount; x++) {
                    if (!this.pixels[y][x]) {
                        continue;
                    }

                    float minX = x * pixel;
                    float maxX = minX + pixel;
                    float maxY = 1.0F - y * pixel;
                    float minY = maxY - pixel;
                    appendPixelMaskQuad(builder, this.pose, minX, minY, maxX, maxY, this.frontZ, this.backZ);
                }
            }
        }
    }

    private static class StaticWorldWindowMask implements WindowMask {
        private final Matrix4f pose = new Matrix4f();
        private final AABB worldBounds;
        private AABB cameraRelativeBounds;
        private long lastSeenGameTime;
        private float tintGreenBlue = 1.0F;

        private StaticWorldWindowMask(BlockPos pos) {
            this.worldBounds = new AABB(pos);
        }

        private void prepare(Vec3 cameraPosition, long gameTime, float tintGreenBlue) {
            double x = this.worldBounds.minX - cameraPosition.x;
            double y = this.worldBounds.minY - cameraPosition.y;
            double z = this.worldBounds.minZ - cameraPosition.z;
            this.pose.translation((float)x, (float)y, (float)z);
            this.cameraRelativeBounds = new AABB(
                    this.worldBounds.minX - cameraPosition.x,
                    this.worldBounds.minY - cameraPosition.y,
                    this.worldBounds.minZ - cameraPosition.z,
                    this.worldBounds.maxX - cameraPosition.x,
                    this.worldBounds.maxY - cameraPosition.y,
                    this.worldBounds.maxZ - cameraPosition.z
            );
            this.lastSeenGameTime = gameTime;
            this.tintGreenBlue = tintGreenBlue;
        }

        @Override
        public Matrix4f pose() {
            return this.pose;
        }

        @Override
        public AABB cameraRelativeBounds() {
            return this.cameraRelativeBounds;
        }

        @Override
        public AABB worldBounds(Vec3 cameraPosition) {
            return this.worldBounds;
        }

        @Override
        public void append(BufferBuilder builder, float min, float max) {
            appendWindowMask(builder, this.pose, min, max);
        }

        @Override
        public float tintGreenBlue() {
            return this.tintGreenBlue;
        }
    }

    private record MatrixKey(
            int m00, int m01, int m02, int m03,
            int m10, int m11, int m12, int m13,
            int m20, int m21, int m22, int m23,
            int m30, int m31, int m32, int m33
    ) {
        private static final float SCALE = 4096.0F;

        private static MatrixKey from(Matrix4f matrix) {
            return new MatrixKey(
                    quantize(matrix.m00()), quantize(matrix.m01()), quantize(matrix.m02()), quantize(matrix.m03()),
                    quantize(matrix.m10()), quantize(matrix.m11()), quantize(matrix.m12()), quantize(matrix.m13()),
                    quantize(matrix.m20()), quantize(matrix.m21()), quantize(matrix.m22()), quantize(matrix.m23()),
                    quantize(matrix.m30()), quantize(matrix.m31()), quantize(matrix.m32()), quantize(matrix.m33())
            );
        }

        private static int quantize(float value) {
            return Math.round(value * SCALE);
        }
    }
}
