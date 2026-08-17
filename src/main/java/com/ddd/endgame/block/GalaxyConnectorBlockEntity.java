package com.ddd.endgame.block;

import com.ddd.endgame.galaxy.GalaxyCompressorNetwork;
import com.ddd.endgame.Xevitia;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.Direction;

public class GalaxyConnectorBlockEntity extends BlockEntity {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private final IItemHandler itemHandler = new InputItemHandler();
    private final IFluidHandler fluidHandler = new InputFluidHandler();

    public GalaxyConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(Xevitia.GALAXY_CONNECTOR_BLOCK_ENTITY.get(), pos, blockState);
    }

    public IItemHandler itemHandler() {
        return this.itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return this.fluidHandler;
    }

    @Nullable
    private GalaxyCompressorBlockEntity connectedController() {
        if (this.level == null) {
            return null;
        }

        GalaxyCompressorNetwork.Status status = GalaxyCompressorNetwork.fromInput(this.level, this.worldPosition);
        return status.hasMultipleControllers() ? null : status.singleController(this.level);
    }

    @Nullable
    private GalaxyFreezerBlockEntity connectedGalaxyFreezer() {
        if (this.level == null) {
            return null;
        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos compressorPos = GalaxyFreezerMultiblock.compressorPosForInputConnector(this.worldPosition, facing);
            if (!(this.level.getBlockEntity(compressorPos) instanceof GalaxyFreezerBlockEntity freezer)) {
                continue;
            }
            if (!freezer.getBlockState().is(Xevitia.GALAXY_FREEZER_BLOCK.get())
                    || freezer.getBlockState().getValue(HorizontalFacingEntityBlock.FACING) != facing) {
                continue;
            }
            if (GalaxyFreezerMultiblock.inputConnectorPos(compressorPos, facing).equals(this.worldPosition)
                    && freezer.isMultiblockValid()) {
                return freezer;
            }
        }
        return null;
    }

    private class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            if (compressor != null && slot == SLOT_OUTPUT) {
                return compressor.itemHandler().getStackInSlot(SLOT_OUTPUT);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlot(slot);
            if (stack.isEmpty()) {
                return stack;
            }

            GalaxyFreezerBlockEntity freezer = GalaxyConnectorBlockEntity.this.connectedGalaxyFreezer();
            if (freezer != null) {
                if (slot != SLOT_INPUT) {
                    return stack;
                }
                return freezer.insertFromConnector(stack, simulate);
            }

            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            if (compressor == null) {
                return stack;
            }

            return compressor.itemHandler().insertItem(SLOT_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            if (slot != SLOT_OUTPUT || amount <= 0) {
                return ItemStack.EMPTY;
            }

            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            return compressor == null ? ItemStack.EMPTY : compressor.itemHandler().extractItem(SLOT_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlot(slot);
            return slot == SLOT_INPUT ? 64 : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            validateSlot(slot);
            GalaxyFreezerBlockEntity freezer = GalaxyConnectorBlockEntity.this.connectedGalaxyFreezer();
            if (freezer != null) {
                return slot == SLOT_INPUT && freezer.canInsertFromConnector(stack);
            }

            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            return slot == SLOT_INPUT && compressor != null && compressor.itemHandler().isItemValid(SLOT_INPUT, stack);
        }

        private void validateSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " is not in valid range - [0," + getSlots() + ")");
            }
        }
    }

    private class InputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            validateTank(tank);
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            validateTank(tank);
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            validateTank(tank);
            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            return compressor != null && compressor.fluidHandler().isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            GalaxyCompressorBlockEntity compressor = GalaxyConnectorBlockEntity.this.connectedController();
            return compressor == null ? 0 : compressor.fluidHandler().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }

        private void validateTank(int tank) {
            if (tank != 0) {
                throw new RuntimeException("Tank " + tank + " is not in valid range - [0,1)");
            }
        }
    }
}
