package com.ddd.endgame.galaxy;

import com.ddd.endgame.Xavitia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GalaxyInstability {
    private static final int GALAXY_BLOCK_DETONATION_TICKS = 5 * 20;
    private static final int EXPLOSION_HEIGHT = 128;
    private static final int GALAXY_INGOT_EXPLOSION_WIDTH = 2;
    private static final int GALAXY_BLOCK_EXPLOSION_WIDTH = 4;
    private static final int GALAXY_FREEZER_EXPLOSION_WIDTH = 8;
    private static final String TICKS_TAG = "GalaxyInstabilityTicks";
    private static final String FROZEN_STABLE_TAG = "GalaxyInstabilityFrozenStable";
    private static final String DROPPED_TICKS_TAG = "GalaxyInstabilityDroppedTicks";
    private static final String DROPPED_INITIALIZED_TAG = "GalaxyInstabilityDroppedInitialized";

    private GalaxyInstability() {
    }

    public static void tickPlayerStacks(ServerPlayer player) {
        ItemStack detonatingStack = tickPlayerOwnedStacks(player);
        if (!detonatingStack.isEmpty()) {
            removeGalaxyMaterials(player.getInventory().items);
            removeGalaxyMaterials(player.getInventory().armor);
            removeGalaxyMaterials(player.getInventory().offhand);
            if (isGalaxyMaterial(player.containerMenu.getCarried())) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            removeExplosionBlocks(player.level(), player.getX(), player.getY(), player.getZ(), explosionWidth(detonatingStack));
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }

        boolean changed = hasGalaxyMaterial(player.getInventory().items)
                || hasGalaxyMaterial(player.getInventory().armor)
                || hasGalaxyMaterial(player.getInventory().offhand)
                || isGalaxyMaterial(player.containerMenu.getCarried());
        for (ItemStack stack : activeContainerGalaxyStacks(player)) {
            changed |= tickVisibleContainerStack(stack);
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    public static void tickStack(ItemStack stack, Level level, @Nullable Entity holder) {
        if (level.isClientSide || stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }

        int detonationTicks = detonationTicks(stack);
        int ticks = Math.min(ticks(stack) + 1, detonationTicks);
        if (ticks < detonationTicks) {
            setTicks(stack, ticks);
            return;
        }

        stack.setCount(0);
        double x = holder == null ? 0.0D : holder.getX();
        double y = holder == null ? 0.0D : holder.getY();
        double z = holder == null ? 0.0D : holder.getZ();
        removeExplosionBlocks(level, x, y, z, explosionWidth(stack));
        if (holder instanceof LivingEntity livingEntity) {
            livingEntity.hurt(livingEntity.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    public static boolean tickDroppedStack(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide || stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return false;
        }

        initializeDroppedTimer(stack, entity);
        int detonationTicks = detonationTicks(stack);
        int ticks = Math.min(droppedTicks(entity) + 1, detonationTicks);
        setDroppedTicks(entity, ticks);
        setTicks(stack, ticks);
        if (ticks >= detonationTicks) {
            removeExplosionBlocks(entity.level(), entity.getX(), entity.getY(), entity.getZ(), explosionWidth(stack));
            stack.setCount(0);
            entity.setItem(ItemStack.EMPTY);
            entity.discard();
        }
        return false;
    }

    public static int ticks(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TICKS_TAG);
    }

    public static int remainingSeconds(ItemStack stack) {
        int remainingTicks = Math.max(0, detonationTicks(stack) - ticks(stack));
        return (remainingTicks + 19) / 20;
    }

    public static int galaxyBlockDetonationTicks() {
        return GALAXY_BLOCK_DETONATION_TICKS;
    }

    public static void removeGalaxyBlockExplosion(Level level, BlockPos origin) {
        removeExplosionBlocks(level, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, GALAXY_BLOCK_EXPLOSION_WIDTH);
    }

    public static void removeGalaxyFreezerExplosion(Level level, BlockPos origin) {
        removeExplosionBlocks(level, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, GALAXY_FREEZER_EXPLOSION_WIDTH);
    }

    public static int detonationTicks(ItemStack stack) {
        return stack.is(Xavitia.GALAXY_BLOCK_ITEM.get()) ? GALAXY_BLOCK_DETONATION_TICKS : Xavitia.GALAXY_INSTABILITY_DETONATION_TICKS;
    }

    public static boolean isFrozenStable(ItemStack stack) {
        return isGalaxyMaterial(stack) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(FROZEN_STABLE_TAG);
    }

    public static int carriedTicks(ItemStack stack, @Nullable Entity holder) {
        return ticks(stack);
    }

    public static void resetTicks(ItemStack stack) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TICKS_TAG);
            tag.remove(FROZEN_STABLE_TAG);
        });
    }

    public static void freezeTicks(ItemStack stack) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }
        stack.remove(DataComponents.CUSTOM_DATA);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(FROZEN_STABLE_TAG, true);
        });
    }

    public static float tintProgress(ItemStack stack) {
        if (!isGalaxyMaterial(stack) || isFrozenStable(stack)) {
            return 0.0F;
        }
        int ticks = ticks(stack);
        Entity entity = stack.getEntityRepresentation();
        if (entity instanceof ItemEntity itemEntity) {
            ticks = Math.min(detonationTicks(stack), ticks + itemEntity.tickCount);
        }
        return Math.min(1.0F, (float)ticks / (float)detonationTicks(stack));
    }

    public static float tintGreenBlue(ItemStack stack) {
        return 1.0F - tintProgress(stack);
    }

    public static boolean isGalaxyMaterial(ItemStack stack) {
        return stack.is(Xavitia.GALAXY_INGOT.get()) || stack.is(Xavitia.GALAXY_BLOCK_ITEM.get());
    }

    private static int explosionWidth(ItemStack stack) {
        return stack.is(Xavitia.GALAXY_BLOCK_ITEM.get()) ? GALAXY_BLOCK_EXPLOSION_WIDTH : GALAXY_INGOT_EXPLOSION_WIDTH;
    }

    private static void removeExplosionBlocks(Level level, double centerX, double centerY, double centerZ, int width) {
        if (level.isClientSide) {
            return;
        }

        int startX = (int)Math.floor(centerX) - width / 2;
        int startZ = (int)Math.floor(centerZ) - width / 2;
        int minY = (int)Math.floor(centerY) - EXPLOSION_HEIGHT / 2;
        int maxY = minY + EXPLOSION_HEIGHT - 1;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = startX; x < startX + width; x++) {
            for (int z = startZ; z < startZ + width; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutablePos.set(x, y, z);
                    BlockState state = level.getBlockState(mutablePos);
                    if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                        level.removeBlock(mutablePos, false);
                    }
                }
            }
        }
    }

    public static void setTicks(ItemStack stack, int ticks) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> updateTicks(tag, Math.min(ticks, detonationTicks(stack))));
    }

    private static void updateTicks(CompoundTag tag, int ticks) {
        tag.putInt(TICKS_TAG, ticks);
        tag.remove(FROZEN_STABLE_TAG);
    }

    private static ItemStack tickPlayerOwnedStacks(ServerPlayer player) {
        ItemStack detonatingStack = tickPlayerOwnedStacks(player.getInventory().items);
        if (!detonatingStack.isEmpty()) {
            return detonatingStack;
        }
        detonatingStack = tickPlayerOwnedStacks(player.getInventory().armor);
        if (!detonatingStack.isEmpty()) {
            return detonatingStack;
        }
        detonatingStack = tickPlayerOwnedStacks(player.getInventory().offhand);
        if (!detonatingStack.isEmpty()) {
            return detonatingStack;
        }
        return tickPlayerOwnedStack(player.containerMenu.getCarried());
    }

    private static ItemStack tickPlayerOwnedStacks(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack detonatingStack = tickPlayerOwnedStack(stack);
            if (!detonatingStack.isEmpty()) {
                return detonatingStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tickPlayerOwnedStack(ItemStack stack) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return ItemStack.EMPTY;
        }
        int detonationTicks = detonationTicks(stack);
        int ticks = Math.min(ticks(stack) + 1, detonationTicks);
        setTicks(stack, ticks);
        return ticks >= detonationTicks ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    private static boolean tickVisibleContainerStack(ItemStack stack) {
        if (stack.isEmpty() || !isGalaxyMaterial(stack)) {
            return false;
        }
        int detonationTicks = detonationTicks(stack);
        int ticks = ticks(stack);
        if (ticks >= detonationTicks) {
            return false;
        }
        setTicks(stack, Math.min(ticks + 1, detonationTicks));
        return true;
    }

    private static boolean hasGalaxyMaterial(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isGalaxyMaterial(stack)) {
                return true;
            }
        }
        return false;
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
        entity.getPersistentData().putInt(DROPPED_TICKS_TAG, Math.max(0, Math.min(ticks, detonationTicks(entity.getItem()))));
    }
}
