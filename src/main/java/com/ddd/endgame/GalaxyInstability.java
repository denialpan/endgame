package com.ddd.endgame;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class GalaxyInstability {
    private static final String TICKS_TAG = "GalaxyInstabilityTicks";

    private GalaxyInstability() {
    }

    public static void tickPlayerStacks(ServerPlayer player) {
        int ticks = maxTicks(player.getInventory().items);
        ticks = Math.max(ticks, maxTicks(player.getInventory().armor));
        ticks = Math.max(ticks, maxTicks(player.getInventory().offhand));
        ItemStack carried = player.containerMenu.getCarried();
        ticks = Math.max(ticks, ticks(carried));

        if (!hasGalaxyMaterial(player.getInventory().items)
                && !hasGalaxyMaterial(player.getInventory().armor)
                && !hasGalaxyMaterial(player.getInventory().offhand)
                && !isGalaxyMaterial(carried)) {
            return;
        }

        ticks = Math.min(ticks + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        if (ticks >= dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            removeGalaxyMaterials(player.getInventory().items);
            removeGalaxyMaterials(player.getInventory().armor);
            removeGalaxyMaterials(player.getInventory().offhand);
            if (isGalaxyMaterial(carried)) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }

        setTicks(player.getInventory().items, ticks);
        setTicks(player.getInventory().armor, ticks);
        setTicks(player.getInventory().offhand, ticks);
        if (isGalaxyMaterial(carried)) {
            setTicks(carried, ticks);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void tickStack(ItemStack stack, Level level, @Nullable Entity holder) {
        if (level.isClientSide || stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }

        int ticks = Math.min(ticks(stack) + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        if (ticks < dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            setTicks(stack, ticks);
            return;
        }

        stack.setCount(0);
        double x = holder == null ? 0.0D : holder.getX();
        double y = holder == null ? 0.0D : holder.getY();
        double z = holder == null ? 0.0D : holder.getZ();
        level.explode(holder, x, y, z, dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
        if (holder instanceof LivingEntity livingEntity) {
            livingEntity.hurt(livingEntity.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    public static boolean tickDroppedStack(ItemStack stack, ItemEntity entity) {
        normalizeNearbyDroppedStackTimers(stack, entity);
        tickStack(stack, entity.level(), entity);
        return false;
    }

    public static int ticks(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TICKS_TAG);
    }

    public static void resetTicks(ItemStack stack) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TICKS_TAG));
    }

    public static float tintProgress(ItemStack stack) {
        if (!isGalaxyMaterial(stack)) {
            return 0.0F;
        }
        return Math.min(1.0F, (float)ticks(stack) / (float)dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
    }

    public static float tintGreenBlue(ItemStack stack) {
        return 1.0F - tintProgress(stack);
    }

    public static boolean isGalaxyMaterial(ItemStack stack) {
        return stack.is(dddsendgame.GALAXY_INGOT.get()) || stack.is(dddsendgame.GALAXY_BLOCK_ITEM.get());
    }

    public static void setTicks(ItemStack stack, int ticks) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> updateTicks(tag, ticks));
    }

    private static void updateTicks(CompoundTag tag, int ticks) {
        tag.putInt(TICKS_TAG, ticks);
    }

    private static int maxTicks(Iterable<ItemStack> stacks) {
        int max = 0;
        for (ItemStack stack : stacks) {
            if (isGalaxyMaterial(stack)) {
                max = Math.max(max, ticks(stack));
            }
        }
        return max;
    }

    private static boolean hasGalaxyMaterial(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isGalaxyMaterial(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void setTicks(Iterable<ItemStack> stacks, int ticks) {
        for (ItemStack stack : stacks) {
            setTicks(stack, ticks);
        }
    }

    private static void removeGalaxyMaterials(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isGalaxyMaterial(stack)) {
                stack.setCount(0);
            }
        }
    }

    private static void normalizeNearbyDroppedStackTimers(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide || stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }

        int max = ticks(stack);
        for (ItemEntity nearby : entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(0.5D, 0.0D, 0.5D), nearby -> nearby != entity && isGalaxyMaterial(nearby.getItem()))) {
            max = Math.max(max, ticks(nearby.getItem()));
        }

        setTicks(stack, max);
        for (ItemEntity nearby : entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(0.5D, 0.0D, 0.5D), nearby -> nearby != entity && isGalaxyMaterial(nearby.getItem()))) {
            setTicks(nearby.getItem(), max);
        }
    }
}
