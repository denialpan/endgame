package com.ddd.endgame.galaxy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class GalaxyInstabilityVisuals {
    private static int playerVisualTicks;

    private GalaxyInstabilityVisuals() {
    }

    public static float tintProgress(ItemStack renderedStack) {
        if (isLocalPlayerStack(renderedStack)) {
            return Math.min(1.0F, (float)playerVisualTicks(renderedStack) / (float)GalaxyInstability.detonationTicks(renderedStack));
        }
        return GalaxyInstability.tintProgress(renderedStack);
    }

    public static float tintGreenBlue(ItemStack renderedStack) {
        return 1.0F - tintProgress(renderedStack);
    }

    private static int playerVisualTicks(ItemStack renderedStack) {
        playerVisualTicks = Math.max(playerVisualTicks, GalaxyInstability.ticks(renderedStack));
        return playerVisualTicks;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        Player player = minecraft.player;
        int maxStackTicks = maxGalaxyTicks(player.getInventory().items);
        maxStackTicks = Math.max(maxStackTicks, maxGalaxyTicks(player.getInventory().armor));
        maxStackTicks = Math.max(maxStackTicks, maxGalaxyTicks(player.getInventory().offhand));
        ItemStack carried = player.containerMenu.getCarried();
        maxStackTicks = Math.max(maxStackTicks, GalaxyInstability.ticks(carried));
        if (!hasGalaxyMaterial(player.getInventory().items)
                && !hasGalaxyMaterial(player.getInventory().armor)
                && !hasGalaxyMaterial(player.getInventory().offhand)
                && !GalaxyInstability.isGalaxyMaterial(carried)) {
            playerVisualTicks = 0;
            return;
        }

        playerVisualTicks = Math.max(playerVisualTicks, maxStackTicks) + 1;
    }

    private static boolean isLocalPlayerStack(ItemStack renderedStack) {
        if (!GalaxyInstability.isGalaxyMaterial(renderedStack)) {
            return false;
        }
        Entity entity = renderedStack.getEntityRepresentation();
        if (entity instanceof ItemEntity) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return false;
        }

        return hasGalaxyMaterial(player.getInventory().items)
                || hasGalaxyMaterial(player.getInventory().armor)
                || hasGalaxyMaterial(player.getInventory().offhand)
                || GalaxyInstability.isGalaxyMaterial(player.containerMenu.getCarried());
    }

    private static int maxGalaxyTicks(Iterable<ItemStack> stacks) {
        int max = 0;
        for (ItemStack stack : stacks) {
            if (GalaxyInstability.isGalaxyMaterial(stack)) {
                max = Math.max(max, GalaxyInstability.ticks(stack));
            }
        }
        return max;
    }

    private static boolean hasGalaxyMaterial(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (GalaxyInstability.isGalaxyMaterial(stack)) {
                return true;
            }
        }
        return false;
    }
}
