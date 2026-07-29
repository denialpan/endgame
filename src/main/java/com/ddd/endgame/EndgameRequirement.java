package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public record EndgameRequirement(ResourceLocation id, ItemStack displayStack, FluidStack displayFluid, Component displayName, boolean fluid, long remaining) {
    public static EndgameRequirement item(ResourceLocation itemId, Item item, long remaining) {
        ItemStack stack = new ItemStack(item);
        return new EndgameRequirement(itemId, stack, FluidStack.EMPTY, stack.getHoverName(), false, remaining);
    }

    public static EndgameRequirement fluid(ResourceLocation fluidId, Fluid fluid, long remaining) {
        FluidStack fluidStack = new FluidStack(fluid, 1000);
        return new EndgameRequirement(fluidId, ItemStack.EMPTY, fluidStack, fluidStack.getHoverName(), true, remaining);
    }

    public boolean complete() {
        return this.remaining <= 0L;
    }
}
