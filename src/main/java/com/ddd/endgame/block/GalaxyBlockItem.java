package com.ddd.endgame.block;

import com.ddd.endgame.GalaxyInstability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class GalaxyBlockItem extends EndgameSkyboxBlockItem {
    public GalaxyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player)) {
            GalaxyInstability.tickStack(stack, level, entity);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return GalaxyInstability.tickDroppedStack(stack, entity);
    }
}
