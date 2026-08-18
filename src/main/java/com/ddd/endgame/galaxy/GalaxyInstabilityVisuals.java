package com.ddd.endgame.galaxy;

import net.minecraft.world.item.ItemStack;

public final class GalaxyInstabilityVisuals {
    private GalaxyInstabilityVisuals() {
    }

    public static float tintProgress(ItemStack renderedStack) {
        return GalaxyInstability.tintProgress(renderedStack);
    }

    public static float tintGreenBlue(ItemStack renderedStack) {
        return 1.0F - tintProgress(renderedStack);
    }

    public static void clientTick() {
    }
}
