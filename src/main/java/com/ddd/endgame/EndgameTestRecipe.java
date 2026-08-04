package com.ddd.endgame;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;

public final class EndgameTestRecipe {
    private EndgameTestRecipe() {
    }

    public static ItemStack createResult(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(dddsendgame.ENDGAME_TEST_STICK.get());
        registries.lookupOrThrow(Registries.ENCHANTMENT)
                .listElements()
                .filter(enchantment -> !enchantment.is(Enchantments.VANISHING_CURSE))
                .forEach(enchantment -> stack.enchant(enchantment, Integer.MAX_VALUE));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("item.dddsendgame.endgame_test_stick.lore").withStyle(ChatFormatting.DARK_PURPLE));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.dddsendgame.endgame_test_stick").withStyle(ChatFormatting.LIGHT_PURPLE));
        return stack;
    }
}
