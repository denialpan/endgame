package com.ddd.endgame.compat;

import net.minecraft.world.item.ItemDisplayContext;

public final class ModCompatibility {
    private ModCompatibility() {
    }

    public static void beforeSkyboxItemRender(ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GROUND) {
            return;
        }

        GnetumCompat.disableCachingForCurrentElement();
        ImmediatelyFastCompat.flushHudBuffers();
    }

    public static void afterSkyboxItemRender(ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GROUND) {
            return;
        }

        ImmediatelyFastCompat.flushHudBuffers();
    }

    public static boolean isShaderPackInUse() {
        return IrisCompat.isShaderPackInUse();
    }

    public static boolean isPhotonShaderPackInUse() {
        return IrisCompat.isPhotonShaderPackInUse();
    }

    public static boolean isRenderingShadowPass() {
        return IrisCompat.isRenderingShadowPass();
    }
}
