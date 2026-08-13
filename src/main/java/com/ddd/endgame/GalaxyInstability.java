package com.ddd.endgame;

import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import com.ddd.endgame.mixin.ChunkMapAccessor;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

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
        setTicks(player.getInventory().items, ticks);
        setTicks(player.getInventory().armor, ticks);
        setTicks(player.getInventory().offhand, ticks);
        setTicks(activeContainerStacks, ticks);
        setTicks(carried, ticks);
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

    public static void tickLoadedInventories(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickBlockEntityInventories(level);
            tickEntityInventories(level);
        }
    }

    public static void tickContainer(Container container, Level level, double x, double y, double z, @Nullable Entity source) {
        int ticks = maxTicks(container);
        if (ticks < 0) {
            return;
        }

        ticks = Math.min(ticks + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        if (ticks >= dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            removeGalaxyMaterials(container);
            container.setChanged();
            level.explode(source, x, y, z, dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
            killHolder(source);
            return;
        }

        setTicks(container, ticks);
        container.setChanged();
    }

    public static void tickItemHandler(IItemHandler handler, Level level, double x, double y, double z, @Nullable Entity source, @Nullable BlockEntity owner) {
        int ticks = maxTicks(handler);
        if (ticks < 0) {
            return;
        }

        ticks = Math.min(ticks + 1, dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS);
        if (ticks >= dddsendgame.GALAXY_INSTABILITY_DETONATION_TICKS) {
            removeGalaxyMaterials(handler);
            setChanged(owner);
            level.explode(source, x, y, z, dddsendgame.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
            killHolder(source);
            return;
        }

        setTicks(handler, ticks);
        setChanged(owner);
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

    private static int maxTicks(Container container) {
        int max = -1;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isGalaxyMaterial(stack)) {
                max = Math.max(max, ticks(stack));
            }
        }
        return max;
    }

    private static int maxTicks(IItemHandler handler) {
        int max = -1;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
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

    private static void setTicks(Container container, int ticks) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            setTicks(container.getItem(slot), ticks);
        }
    }

    private static void setTicks(IItemHandler handler, int ticks) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!isGalaxyMaterial(stack)) {
                continue;
            }

            if (handler instanceof IItemHandlerModifiable modifiable) {
                ItemStack copy = stack.copy();
                setTicks(copy, ticks);
                modifiable.setStackInSlot(slot, copy);
            } else {
                setTicks(stack, ticks);
            }
        }
    }

    private static Iterable<ItemStack> activeContainerGalaxyStacks(ServerPlayer player) {
        if (player.containerMenu instanceof GalaxyFreezerMenu
                || !(player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu)) {
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

    private static void removeGalaxyMaterials(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isGalaxyMaterial(container.getItem(slot))) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void removeGalaxyMaterials(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!isGalaxyMaterial(stack)) {
                continue;
            }

            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, ItemStack.EMPTY);
                continue;
            }

            int safety = 0;
            while (isGalaxyMaterial(handler.getStackInSlot(slot)) && safety++ < 64) {
                ItemStack current = handler.getStackInSlot(slot);
                ItemStack extracted = handler.extractItem(slot, current.getCount(), false);
                if (extracted.isEmpty()) {
                    break;
                }
            }
        }
    }

    private static void tickBlockEntityInventories(ServerLevel level) {
        for (ChunkHolder holder : ((ChunkMapAccessor)level.getChunkSource().chunkMap).dddsendgame$getChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) {
                continue;
            }

            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof GalaxyFreezerBlockEntity) {
                    continue;
                }

                BlockPos pos = blockEntity.getBlockPos();
                double x = pos.getX() + 0.5D;
                double y = pos.getY() + 0.5D;
                double z = pos.getZ() + 0.5D;
                if (blockEntity instanceof Container container) {
                    tickContainer(container, level, x, y, z, null);
                    continue;
                }

                IItemHandler handler = itemHandler(level, pos);
                if (handler != null) {
                    tickItemHandler(handler, level, x, y, z, null, blockEntity);
                }
            }
        }
    }

    private static void tickEntityInventories(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Player || entity instanceof ItemEntity) {
                continue;
            }

            IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY);
            if (handler != null) {
                tickItemHandler(handler, level, entity.getX(), entity.getY(), entity.getZ(), entity, null);
            }
        }
    }

    @Nullable
    private static IItemHandler itemHandler(ServerLevel level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }

        for (Direction direction : Direction.values()) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private static void setChanged(@Nullable BlockEntity owner) {
        if (owner != null) {
            owner.setChanged();
        }
    }

    private static void killHolder(@Nullable Entity source) {
        if (source instanceof LivingEntity livingEntity) {
            livingEntity.hurt(livingEntity.damageSources().genericKill(), Float.MAX_VALUE);
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
