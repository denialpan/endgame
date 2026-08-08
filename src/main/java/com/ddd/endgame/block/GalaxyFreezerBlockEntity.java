package com.ddd.endgame.block;

import com.ddd.endgame.GalaxyFreezerMenu;
import com.ddd.endgame.GalaxyInstability;
import com.ddd.endgame.dddsendgame;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class GalaxyFreezerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 6;
    private static final long MULTIBLOCK_CHECK_INTERVAL_TICKS = 20L;
    private static final String ITEMS_TAG = "Items";
    private boolean multiblockValid;
    private long lastMultiblockCheck = Long.MIN_VALUE;
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(dddsendgame.GALAXY_INGOT.get()) && GalaxyFreezerBlockEntity.this.isMultiblockValid();
        }

        @Override
        protected void onContentsChanged(int slot) {
            GalaxyInstability.resetTicks(this.getStackInSlot(slot));
            GalaxyFreezerBlockEntity.this.setChanged();
        }
    };

    public GalaxyFreezerBlockEntity(BlockPos pos, BlockState blockState) {
        super(dddsendgame.GALAXY_FREEZER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ItemStackHandler itemHandler() {
        return this.itemHandler;
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
        return Component.translatable("container.dddsendgame.galaxy_freezer");
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
        resetStoredInstabilityTimers();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ITEMS_TAG, this.itemHandler.serializeNBT(registries));
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

    private void resetStoredInstabilityTimers() {
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            GalaxyInstability.resetTicks(this.itemHandler.getStackInSlot(slot));
        }
    }
}
