package com.ddd.endgame;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class GalaxyInstability {
    private static final String TICKS_TAG = "GalaxyInstabilityTicks";
    private static final String PLAYER_TICKS_TAG = "GalaxyInstabilityPlayerTicks";
    private static final String DROPPED_TICKS_TAG = "GalaxyInstabilityDroppedTicks";
    private static final String DROPPED_INITIALIZED_TAG = "GalaxyInstabilityDroppedInitialized";

    private GalaxyInstability() {
    }

    public static void tickPlayerStacks(ServerPlayer player) {
        Iterable<ItemStack> activeContainerStacks = activeContainerGalaxyStacks(player);
        int ticks = playerTicks(player);
        ticks = Math.max(ticks, maxTicks(player.getInventory().items));
        ticks = Math.max(ticks, maxTicks(player.getInventory().armor));
        ticks = Math.max(ticks, maxTicks(player.getInventory().offhand));
        ticks = Math.max(ticks, maxTicks(activeContainerStacks));
        ItemStack carried = player.containerMenu.getCarried();
        ticks = Math.max(ticks, ticks(carried));

        if (!hasGalaxyMaterial(player.getInventory().items)
                && !hasGalaxyMaterial(player.getInventory().armor)
                && !hasGalaxyMaterial(player.getInventory().offhand)
                && !hasGalaxyMaterial(activeContainerStacks)
                && !isGalaxyMaterial(carried)) {
            player.getPersistentData().remove(PLAYER_TICKS_TAG);
            return;
        }

        ticks = Math.min(ticks + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        if (ticks >= dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            removeGalaxyMaterials(player.getInventory().items);
            removeGalaxyMaterials(player.getInventory().armor);
            removeGalaxyMaterials(player.getInventory().offhand);
            removeGalaxyMaterials(activeContainerStacks);
            if (isGalaxyMaterial(carried)) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }

        setPlayerTicks(player, ticks);
        setTicks(activeContainerStacks, ticks);
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
        if (entity.level().isClientSide || stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return false;
        }

        initializeDroppedTimer(stack, entity);
        normalizeNearbyDroppedEntityTimers(entity);
        int ticks = Math.min(droppedTicks(entity) + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        setDroppedTicks(entity, ticks);
        setTicks(stack, ticks);
        if (ticks >= dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            stack.setCount(0);
            entity.setItem(ItemStack.EMPTY);
            entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(), dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
            entity.discard();
        }
        return false;
    }

    public static int ticks(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TICKS_TAG);
    }

    public static int carriedTicks(ItemStack stack, @Nullable Entity holder) {
        int ticks = ticks(stack);
        if (holder instanceof Player player) {
            ticks = Math.max(ticks, playerTicks(player));
        }
        return ticks;
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
        int ticks = ticks(stack);
        Entity entity = stack.getEntityRepresentation();
        if (entity instanceof ItemEntity itemEntity) {
            ticks = Math.min(dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS, ticks + itemEntity.tickCount);
        }
        return Math.min(1.0F, (float)ticks / (float)dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
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

    private static int playerTicks(Player player) {
        return player.getPersistentData().getInt(PLAYER_TICKS_TAG);
    }

    private static void setPlayerTicks(Player player, int ticks) {
        player.getPersistentData().putInt(PLAYER_TICKS_TAG, Math.max(0, Math.min(ticks, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS)));
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

    private static Iterable<ItemStack> activeContainerGalaxyStacks(ServerPlayer player) {
        if (player.containerMenu instanceof GalaxyFreezerMenu) {
            return java.util.List.of();
        }

        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (Slot slot : player.containerMenu.slots) {
            if (slot instanceof ResultSlot || slot.container instanceof Inventory) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (isGalaxyMaterial(stack)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static void removeGalaxyMaterials(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isGalaxyMaterial(stack)) {
                stack.setCount(0);
            }
        }
    }

    private static void initializeDroppedTimer(ItemStack stack, ItemEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(DROPPED_INITIALIZED_TAG)) {
            return;
        }

        data.putBoolean(DROPPED_INITIALIZED_TAG, true);
        int ticks = Math.max(droppedTicks(entity), ticks(stack));
        if (entity.getOwner() instanceof Player player) {
            ticks = Math.max(ticks, playerTicks(player));
        }
        setDroppedTicks(entity, ticks);
        if (ticks(stack) != ticks) {
            setTicks(stack, ticks);
            entity.setItem(stack.copy());
        }
    }

    private static int droppedTicks(ItemEntity entity) {
        return Math.max(entity.getPersistentData().getInt(DROPPED_TICKS_TAG), ticks(entity.getItem()));
    }

    private static void setDroppedTicks(ItemEntity entity, int ticks) {
        entity.getPersistentData().putInt(DROPPED_TICKS_TAG, Math.max(0, Math.min(ticks, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS)));
    }

    private static void normalizeNearbyDroppedEntityTimers(ItemEntity entity) {
        if (entity.level().isClientSide || !isGalaxyMaterial(entity.getItem())) {
            return;
        }

        int max = droppedTicks(entity);
        for (ItemEntity nearby : entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(0.5D, 0.0D, 0.5D), nearby -> nearby != entity && isGalaxyMaterial(nearby.getItem()))) {
            initializeDroppedTimer(nearby.getItem(), nearby);
            max = Math.max(max, droppedTicks(nearby));
        }

        setDroppedTicks(entity, max);
        for (ItemEntity nearby : entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(0.5D, 0.0D, 0.5D), nearby -> nearby != entity && isGalaxyMaterial(nearby.getItem()))) {
            setDroppedTicks(nearby, max);
        }
    }
}
