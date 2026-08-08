package com.ddd.endgame;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GalaxyInstabilityVisuals {
    private static final int DETONATION_TICKS = 10 * 20;
    private static long lastGameTime = Long.MIN_VALUE;
    private static int carriedTicks;

    private GalaxyInstabilityVisuals() {
    }

    public static float tintProgress(ItemStack renderedStack) {
        if (!isGalaxyMaterial(renderedStack)) {
            return 0.0F;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return 0.0F;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime != lastGameTime) {
            lastGameTime = gameTime;
            carriedTicks = hasGalaxyMaterial(minecraft.player) ? Math.min(carriedTicks + 1, DETONATION_TICKS) : 0;
        }

        return Mth.clamp((float)carriedTicks / (float)DETONATION_TICKS, 0.0F, 1.0F);
    }

    public static float tintGreenBlue(ItemStack renderedStack) {
        return 1.0F - tintProgress(renderedStack);
    }

    private static boolean hasGalaxyMaterial(Player player) {
        return player.getInventory().contains(GalaxyInstabilityVisuals::isGalaxyMaterial);
    }

    private static boolean isGalaxyMaterial(ItemStack stack) {
        return stack.is(dddsendgame.GALAXY_INGOT.get()) || stack.is(dddsendgame.GALAXY_BLOCK_ITEM.get());
    }
}
