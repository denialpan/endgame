package com.ddd.endgame;

import net.minecraft.world.item.ItemStack;

public final class GalaxyInstabilityVisuals {
    private GalaxyInstabilityVisuals() {
    }

    public static float tintProgress(ItemStack renderedStack) {
        return GalaxyInstability.tintProgress(renderedStack);
    }

    public static float tintGreenBlue(ItemStack renderedStack) {
        return GalaxyInstability.tintGreenBlue(renderedStack);
    }
}
