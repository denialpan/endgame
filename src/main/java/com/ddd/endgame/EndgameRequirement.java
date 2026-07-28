package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record EndgameRequirement(ResourceLocation itemId, Item item, long remaining) {
    public ItemStack displayStack() {
        return new ItemStack(this.item);
    }

    public boolean complete() {
        return this.remaining <= 0L;
    }
}
