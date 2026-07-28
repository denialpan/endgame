package com.ddd.endgame;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EndgameTemplateMenu extends AbstractContainerMenu {
    private static final int TEMPLATE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = TEMPLATE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;
    private final Container container;
    private final BlockPos pos;
    @Nullable
    private final EndgameTemplateBlockEntity blockEntity;

    public EndgameTemplateMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, clientContainer(playerInventory, buffer.readBlockPos()));
    }

    public EndgameTemplateMenu(int containerId, Inventory playerInventory, EndgameTemplateBlockEntity blockEntity) {
        this(containerId, playerInventory, new MenuTarget(blockEntity, blockEntity.getBlockPos(), blockEntity));
    }

    private EndgameTemplateMenu(int containerId, Inventory playerInventory, MenuTarget target) {
        super(dddsendgame.ENDGAME_TEMPLATE_MENU.get(), containerId);
        checkContainerSize(target.container(), TEMPLATE_SLOT_COUNT);
        this.container = target.container();
        this.pos = target.pos();
        this.blockEntity = target.blockEntity();

        this.addSlot(new Slot(this.container, 0, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EndgameTemplateMenu.this.container.canPlaceItem(this.getSlotIndex(), stack);
            }
        });
        this.addSlot(new Slot(this.container, 1, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 48 + column * 18, 194 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 48 + column * 18, 252));
        }
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Nullable
    public EndgameTemplateBlockEntity blockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        original = stack.copy();

        if (index == 1) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index >= PLAYER_INVENTORY_START) {
            if (this.blockEntity != null) {
                int accepted = this.blockEntity.acceptContribution(stack);
                if (accepted <= 0) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null
                ? true
                : stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, dddsendgame.ENDGAME_TEMPLATE_BLOCK.get());
    }

    private static MenuTarget clientContainer(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof EndgameTemplateBlockEntity blockEntity) {
            return new MenuTarget(blockEntity, pos, blockEntity);
        }
        return new MenuTarget(new SimpleContainer(TEMPLATE_SLOT_COUNT), pos, null);
    }

    private record MenuTarget(Container container, BlockPos pos, @Nullable EndgameTemplateBlockEntity blockEntity) {
    }
}
