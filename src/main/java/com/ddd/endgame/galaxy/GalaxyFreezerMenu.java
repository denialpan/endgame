package com.ddd.endgame.galaxy;

import com.ddd.endgame.dddsendgame;

import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class GalaxyFreezerMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = GalaxyFreezerBlockEntity.SLOT_COUNT;
    private static final int SLOT_START_X = 62;
    private static final int SLOT_START_Y = 27;
    private static final int ICE_SLOT_X = 125;
    private static final int ICE_SLOT_Y = SLOT_START_Y;
    private static final int SLOT_SPACING = 18;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private final DataSlot coolantTicksSlot;
    private final BlockPos pos;
    @Nullable
    private final GalaxyFreezerBlockEntity blockEntity;

    public GalaxyFreezerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, clientBlockEntity(playerInventory, buffer.readBlockPos()));
    }

    public GalaxyFreezerMenu(int containerId, Inventory playerInventory, GalaxyFreezerBlockEntity blockEntity) {
        super(dddsendgame.GALAXY_FREEZER_MENU.get(), containerId);
        this.pos = blockEntity.getBlockPos();
        this.blockEntity = blockEntity;
        addFreezerSlots(blockEntity.itemHandler());
        addPlayerSlots(playerInventory);
        this.coolantTicksSlot = this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.coolantTicks();
            }

            @Override
            public void set(int value) {
            }
        });
    }

    private GalaxyFreezerMenu(int containerId, Inventory playerInventory, ClientTarget target) {
        super(dddsendgame.GALAXY_FREEZER_MENU.get(), containerId);
        this.pos = target.pos();
        this.blockEntity = target.blockEntity();
        addFreezerSlots(target.itemHandler());
        addPlayerSlots(playerInventory);
        this.coolantTicksSlot = this.addDataSlot(DataSlot.standalone());
    }

    @Nullable
    public GalaxyFreezerBlockEntity blockEntity() {
        return this.blockEntity;
    }

    public int coolantTicks() {
        return this.coolantTicksSlot.get();
    }

    public ItemStack freezerStack(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return this.slots.get(slot).getItem();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
    }

    private void addFreezerSlots(IItemHandler itemHandler) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = row * 3 + col;
                this.addSlot(new FreezerSlot(itemHandler, slot, SLOT_START_X + col * SLOT_SPACING, SLOT_START_Y + row * SLOT_SPACING));
            }
        }
        for (int slot = GalaxyFreezerBlockEntity.ICE_SLOT_START; slot < SLOT_COUNT; slot++) {
            int row = slot - GalaxyFreezerBlockEntity.ICE_SLOT_START;
            this.addSlot(new FreezerSlot(itemHandler, slot, ICE_SLOT_X, ICE_SLOT_Y + row * SLOT_SPACING));
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        PLAYER_INVENTORY_X + col * SLOT_SPACING,
                        PLAYER_INVENTORY_Y + row * SLOT_SPACING
                ));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(
                    playerInventory,
                    col,
                    PLAYER_INVENTORY_X + col * SLOT_SPACING,
                    HOTBAR_Y
            ));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(dddsendgame.GALAXY_INGOT.get())) {
            if (!this.moveItemStackTo(stack, 0, GalaxyFreezerBlockEntity.INGOT_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (GalaxyFreezerBlockEntity.isCoolant(stack)) {
            if (!this.moveItemStackTo(stack, GalaxyFreezerBlockEntity.ICE_SLOT_START, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null
                ? true
                : stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, dddsendgame.GALAXY_FREEZER_BLOCK.get());
    }

    private static ClientTarget clientBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof GalaxyFreezerBlockEntity blockEntity) {
            return new ClientTarget(pos, blockEntity, blockEntity.itemHandler());
        }
        return new ClientTarget(pos, null, new ItemStackHandler(SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot < GalaxyFreezerBlockEntity.INGOT_SLOT_COUNT) {
                    return stack.is(dddsendgame.GALAXY_INGOT.get());
                }
                return GalaxyFreezerBlockEntity.isCoolant(stack);
            }
        });
    }

    private record ClientTarget(BlockPos pos, @Nullable GalaxyFreezerBlockEntity blockEntity, IItemHandler itemHandler) {
    }

    private static class FreezerSlot extends SlotItemHandler {
        private FreezerSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int increment) {
            if (stack.isEmpty() || !this.mayPlace(stack)) {
                return stack;
            }

            int insertCount = Math.min(increment, stack.getCount());
            ItemStack insertStack = stack.copyWithCount(insertCount);
            ItemStack remainder = this.getItemHandler().insertItem(this.index, insertStack, false);
            int inserted = insertCount - remainder.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                this.setChanged();
            }
            return stack;
        }
    }
}
