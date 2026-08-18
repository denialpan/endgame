package com.ddd.endgame.block;

import com.ddd.endgame.galaxy.GalaxyFreezerMenu;
import com.ddd.endgame.galaxy.GalaxyInstability;
import com.ddd.endgame.Xavitia;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class GalaxyFreezerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INGOT_SLOT_COUNT = 6;
    public static final int ICE_SLOT_COUNT = 2;
    public static final int ICE_SLOT_START = INGOT_SLOT_COUNT;
    public static final int SLOT_COUNT = INGOT_SLOT_COUNT + ICE_SLOT_COUNT;
    private static final long MULTIBLOCK_CHECK_INTERVAL_TICKS = 20L;
    private static final int INGOTS_PER_EXPLOSION_SIZE_STEP = 16;
    private static final int ICE_COOLING_TICKS = 750;
    private static final int PACKED_ICE_COOLING_TICKS = 1_500;
    private static final int BLUE_ICE_COOLING_TICKS = 3_000;
    private static final int COMPRESSED_ICE_1_COOLING_TICKS = 6_000;
    private static final int COMPRESSED_ICE_2_COOLING_TICKS = 12_000;
    private static final int COMPRESSED_ICE_3_COOLING_TICKS = 24_000;
    private static final int COMPRESSED_ICE_4_COOLING_TICKS = 48_000;
    private static final int COMPRESSED_ICE_5_COOLING_TICKS = 96_000;
    private static final String ITEMS_TAG = "Items";
    private static final String COOLANT_TICKS_TAG = "CoolantTicks";
    private boolean multiblockValid;
    private long lastMultiblockCheck = Long.MIN_VALUE;
    private int coolantTicks;
    private final IItemHandler directAutomationHandler = new DirectAutomationItemHandler();
    private final IItemHandler connectorInputHandler = new ConnectorInputItemHandler();
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < INGOT_SLOT_COUNT) {
                return stack.is(Xavitia.GALAXY_INGOT.get()) && GalaxyFreezerBlockEntity.this.isMultiblockValid();
            }
            return isIceSlot(slot) && isCoolant(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            GalaxyFreezerBlockEntity.this.setChanged();
        }
    };

    public GalaxyFreezerBlockEntity(BlockPos pos, BlockState blockState) {
        super(Xavitia.GALAXY_FREEZER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ItemStackHandler itemHandler() {
        return this.itemHandler;
    }

    public IItemHandler directAutomationHandler() {
        return this.directAutomationHandler;
    }

    public IItemHandler connectorInputHandler() {
        return this.connectorInputHandler;
    }

    public ItemStack insertFromConnector(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack;
        int startSlot = isCoolant(stack) ? ICE_SLOT_START : 0;
        int endSlot = isCoolant(stack) ? SLOT_COUNT : INGOT_SLOT_COUNT;
        for (int slot = startSlot; slot < endSlot && !remainder.isEmpty(); slot++) {
            remainder = this.itemHandler.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    public boolean canInsertFromConnector(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isCoolant(stack)) {
            for (int slot = ICE_SLOT_START; slot < SLOT_COUNT; slot++) {
                if (this.itemHandler.isItemValid(slot, stack)) {
                    return true;
                }
            }
            return false;
        }

        for (int slot = 0; slot < INGOT_SLOT_COUNT; slot++) {
            if (this.itemHandler.isItemValid(slot, stack)) {
                return true;
            }
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GalaxyFreezerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        boolean valid = blockEntity.isMultiblockValid();
        ItemStack coolant = blockEntity.activeCoolant();
        boolean coolingActive = valid && !coolant.isEmpty();
        boolean hasGalaxyMaterial = false;
        boolean changed = false;
        if (coolingActive) {
            changed |= blockEntity.compactFrozenIngots();
        }
        for (int slot = 0; slot < INGOT_SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(slot);
            if (!GalaxyInstability.isGalaxyMaterial(stack)) {
                continue;
            }

            hasGalaxyMaterial = true;
            if (coolingActive) {
                if (GalaxyInstability.ticks(stack) > 0 || !GalaxyInstability.isFrozenStable(stack)) {
                    GalaxyInstability.freezeTicks(stack);
                    changed = true;
                }
                continue;
            }

            int ticks = Math.min(GalaxyInstability.ticks(stack) + 1, Xavitia.GALAXY_INSTABILITY_DETONATION_TICKS);
            GalaxyInstability.setTicks(stack, ticks);
            changed = true;
            if (ticks >= Xavitia.GALAXY_INSTABILITY_DETONATION_TICKS) {
                detonate(level, pos, blockEntity);
                return;
            }
        }

        if (coolingActive && hasGalaxyMaterial) {
            blockEntity.coolantTicks++;
            if (blockEntity.coolantTicks >= coolingPeriod(coolant)) {
                blockEntity.coolantTicks = 0;
                blockEntity.consumeCoolant();
            }
            changed = true;
        } else if (blockEntity.coolantTicks != 0) {
            blockEntity.coolantTicks = 0;
            changed = true;
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    private boolean compactFrozenIngots() {
        int totalIngots = 0;
        int occupiedSlots = 0;
        for (int slot = 0; slot < INGOT_SLOT_COUNT; slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (stack.is(Xavitia.GALAXY_INGOT.get())) {
                totalIngots += stack.getCount();
                occupiedSlots++;
            }
        }

        if (totalIngots <= 0) {
            return false;
        }

        ItemStack firstStack = this.itemHandler.getStackInSlot(0);
        if (occupiedSlots == 1
                && firstStack.is(Xavitia.GALAXY_INGOT.get())
                && firstStack.getCount() == totalIngots
                && GalaxyInstability.ticks(firstStack) == 0
                && GalaxyInstability.isFrozenStable(firstStack)) {
            return false;
        }

        int remaining = totalIngots;
        for (int slot = 0; slot < INGOT_SLOT_COUNT; slot++) {
            if (remaining <= 0) {
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                continue;
            }

            ItemStack merged = new ItemStack(Xavitia.GALAXY_INGOT.get());
            int count = Math.min(remaining, merged.getMaxStackSize());
            merged.setCount(count);
            GalaxyInstability.freezeTicks(merged);
            this.itemHandler.setStackInSlot(slot, merged);
            remaining -= count;
        }
        return true;
    }

    public boolean isMultiblockValid() {
        if (this.level == null) {
            return false;
        }

        long gameTime = this.level.getGameTime();
        if (this.lastMultiblockCheck == Long.MIN_VALUE || gameTime - this.lastMultiblockCheck >= MULTIBLOCK_CHECK_INTERVAL_TICKS) {
            this.lastMultiblockCheck = gameTime;
            this.multiblockValid = GalaxyFreezerMultiblock.matches(
                    this.level,
                    this.worldPosition,
                    this.getBlockState().getValue(HorizontalFacingEntityBlock.FACING)
            );
        }
        return this.multiblockValid;
    }

    public void invalidateMultiblockCache() {
        this.lastMultiblockCheck = Long.MIN_VALUE;
    }

    public int coolantTicks() {
        return this.coolantTicks;
    }

    public static boolean isCoolant(ItemStack stack) {
        return stack.is(Items.ICE)
                || stack.is(Items.PACKED_ICE)
                || stack.is(Items.BLUE_ICE)
                || stack.is(Xavitia.COMPRESSED_ICE_1_ITEM.get())
                || stack.is(Xavitia.COMPRESSED_ICE_2_ITEM.get())
                || stack.is(Xavitia.COMPRESSED_ICE_3_ITEM.get())
                || stack.is(Xavitia.COMPRESSED_ICE_4_ITEM.get())
                || stack.is(Xavitia.COMPRESSED_ICE_5_ITEM.get());
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.xavitia.galaxy_freezer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GalaxyFreezerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound(ITEMS_TAG));
        this.coolantTicks = tag.getInt(COOLANT_TICKS_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ITEMS_TAG, this.itemHandler.serializeNBT(registries));
        tag.putInt(COOLANT_TICKS_TAG, this.coolantTicks);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    private static void detonate(Level level, BlockPos pos, GalaxyFreezerBlockEntity blockEntity) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ItemEntity explosionSource = new ItemEntity(level, x, y, z, new ItemStack(Xavitia.GALAXY_INGOT.get()));
        int galaxyMaterialCount = 0;
        for (int slot = 0; slot < INGOT_SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(slot);
            if (GalaxyInstability.isGalaxyMaterial(stack)) {
                galaxyMaterialCount += stack.getCount();
                stack.setCount(0);
            }
        }
        float explosionRadius = Xavitia.GALAXY_INSTABILITY_EXPLOSION_RADIUS
                * Math.max(1, (int)Math.ceil((double)galaxyMaterialCount / (double)INGOTS_PER_EXPLOSION_SIZE_STEP));
        blockEntity.setChanged();
        destroyFreezerMultiblock(level, pos, blockEntity);
        level.explode(
                explosionSource,
                x,
                y,
                z,
                explosionRadius,
                false,
                Level.ExplosionInteraction.BLOCK
        );
        explosionSource.discard();
    }

    private static void destroyFreezerMultiblock(Level level, BlockPos pos, GalaxyFreezerBlockEntity blockEntity) {
        level.removeBlock(pos, false);
        for (GalaxyFreezerMultiblock.PreviewBlock previewBlock : GalaxyFreezerMultiblock.previewBlocks(
                pos,
                blockEntity.getBlockState().getValue(HorizontalFacingEntityBlock.FACING)
        )) {
            if (previewBlock.matches(level.getBlockState(previewBlock.pos()))) {
                level.removeBlock(previewBlock.pos(), false);
            }
        }
    }

    private class DirectAutomationItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return GalaxyFreezerBlockEntity.this.itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return GalaxyFreezerBlockEntity.this.itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return GalaxyFreezerBlockEntity.this.itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return GalaxyFreezerBlockEntity.this.itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private class ConnectorInputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return GalaxyFreezerBlockEntity.this.itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isIceSlot(slot) && isCoolant(stack)) {
                return stack;
            }
            if (slot >= INGOT_SLOT_COUNT && !isCoolant(stack)) {
                return stack;
            }
            return GalaxyFreezerBlockEntity.this.itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return GalaxyFreezerBlockEntity.this.itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GalaxyFreezerBlockEntity.this.itemHandler.isItemValid(slot, stack);
        }
    }

    private static boolean isIceSlot(int slot) {
        return slot >= ICE_SLOT_START && slot < SLOT_COUNT;
    }

    private ItemStack activeCoolant() {
        for (int slot = ICE_SLOT_START; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (isCoolant(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void consumeCoolant() {
        for (int slot = ICE_SLOT_START; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (isCoolant(stack)) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static int coolingPeriod(ItemStack stack) {
        if (stack.is(Xavitia.COMPRESSED_ICE_5_ITEM.get())) {
            return COMPRESSED_ICE_5_COOLING_TICKS;
        }
        if (stack.is(Xavitia.COMPRESSED_ICE_4_ITEM.get())) {
            return COMPRESSED_ICE_4_COOLING_TICKS;
        }
        if (stack.is(Xavitia.COMPRESSED_ICE_3_ITEM.get())) {
            return COMPRESSED_ICE_3_COOLING_TICKS;
        }
        if (stack.is(Xavitia.COMPRESSED_ICE_2_ITEM.get())) {
            return COMPRESSED_ICE_2_COOLING_TICKS;
        }
        if (stack.is(Xavitia.COMPRESSED_ICE_1_ITEM.get())) {
            return COMPRESSED_ICE_1_COOLING_TICKS;
        }
        if (stack.is(Items.ICE)) {
            return ICE_COOLING_TICKS;
        }
        if (stack.is(Items.PACKED_ICE)) {
            return PACKED_ICE_COOLING_TICKS;
        }
        return BLUE_ICE_COOLING_TICKS;
    }

    public static int coolingPeriodSeconds(ItemStack stack) {
        return (coolingPeriod(stack) + 19) / 20;
    }
}
