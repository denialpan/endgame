package com.ddd.endgame.compat;

import net.minecraft.world.item.ItemDisplayContext;

import java.lang.reflect.Method;

public final class RenderOptimizationCompat {
    private static boolean checked;
    private static Method immediatelyFastFlushHudBuffersMethod;
    private static Method gnetumDisableCachingForCurrentElementMethod;

    private RenderOptimizationCompat() {
    }

    public static void beforeSkyboxItemRender(ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GROUND) {
            return;
        }

        initializeIfNeeded();
        disableGnetumCurrentElementCaching();
        flushImmediatelyFastHudBuffers();
    }

    public static void afterSkyboxItemRender(ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GROUND) {
            return;
        }

        initializeIfNeeded();
        flushImmediatelyFastHudBuffers();
    }

    private static void initializeIfNeeded() {
        if (checked) {
            return;
        }

        checked = true;
        try {
            Class<?> batchingBuffersClass = Class.forName("net.raphimc.immediatelyfast.feature.batching.BatchingBuffers");
            immediatelyFastFlushHudBuffersMethod = batchingBuffersClass.getMethod("tryForceDrawHudBuffers");
        } catch (ReflectiveOperationException ignored) {
            immediatelyFastFlushHudBuffersMethod = null;
        }

        try {
            Class<?> gnetumClass = Class.forName("me.decce.gnetum.Gnetum");
            gnetumDisableCachingForCurrentElementMethod = gnetumClass.getMethod("disableCachingForCurrentElement", String.class);
        } catch (ReflectiveOperationException ignored) {
            gnetumDisableCachingForCurrentElementMethod = null;
        }
    }

    private static void disableGnetumCurrentElementCaching() {
        if (gnetumDisableCachingForCurrentElementMethod == null) {
            return;
        }

        try {
            gnetumDisableCachingForCurrentElementMethod.invoke(null, "dddsendgame skybox item uses stencil state");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void flushImmediatelyFastHudBuffers() {
        if (immediatelyFastFlushHudBuffersMethod == null) {
            return;
        }

        try {
            immediatelyFastFlushHudBuffersMethod.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }
}
