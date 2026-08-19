package com.ddd.endgame.compat;

import com.ddd.endgame.item.ItemFabricatorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class ItemFabricatorVanillaAutomation {
    private static final DefaultDispenseItemBehavior DROP_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior();

    private ItemFabricatorVanillaAutomation() {
    }

    public static boolean dispenseFromDropper(ServerLevel level, BlockState state, BlockPos pos) {
        DispenserBlockEntity dropper = level.getBlockEntity(pos, BlockEntityType.DROPPER).orElse(null);
        if (dropper == null) {
            return false;
        }

        int slot = dropper.getRandomSlot(level.random);
        if (slot < 0) {
            return false;
        }

        ItemStack fabricator = dropper.getItem(slot);
        if (!(fabricator.getItem() instanceof ItemFabricatorItem)) {
            return false;
        }

        ItemStack output = ItemFabricatorItem.selectedItemStack(fabricator, 1);
        if (output.isEmpty()) {
            return true;
        }

        Direction direction = state.getValue(DispenserBlock.FACING);
        Direction insertSide = direction.getOpposite();
        BlockPos destinationPos = pos.relative(direction);
        IItemHandler destinationHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, destinationPos, insertSide);
        if (destinationHandler != null && insertIntoHandler(destinationHandler, output)) {
            return true;
        }

        Container container = HopperBlockEntity.getContainerAt(level, destinationPos);
        if (container != null && HopperBlockEntity.addItem(dropper, container, output.copy(), insertSide).isEmpty()) {
            return true;
        }

        DROP_ITEM_BEHAVIOR.dispense(new BlockSource(level, pos, state, dropper), output.copy());
        return true;
    }

    private static boolean insertIntoHandler(IItemHandler handler, ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack.copy(), false).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
