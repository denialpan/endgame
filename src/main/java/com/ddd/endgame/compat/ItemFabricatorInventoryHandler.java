package com.ddd.endgame.compat;

import com.ddd.endgame.item.ItemFabricatorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemFabricatorInventoryHandler implements IItemHandler {
    private final IItemHandler delegate;

    public ItemFabricatorInventoryHandler(IItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return this.delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack stack = this.delegate.getStackInSlot(slot);
        return isFabricator(stack) ? ItemFabricatorItem.selectedItemStack(stack, Integer.MAX_VALUE) : stack;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = this.delegate.getStackInSlot(slot);
        return isFabricator(stack) ? ItemFabricatorItem.selectedItemStack(stack, amount) : this.delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        ItemStack stack = this.delegate.getStackInSlot(slot);
        return isFabricator(stack) ? ItemFabricatorItem.selectedItemStack(stack, Integer.MAX_VALUE).getMaxStackSize() : this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }

    private static boolean isFabricator(ItemStack stack) {
        return stack.getItem() instanceof ItemFabricatorItem;
    }
}
