package com.ddd.endgame;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class EndgameTemplateInputBlockEntity extends BlockEntity {
    private static final int SLOT_INPUT = 0;
    private final IItemHandler itemHandler = new InputItemHandler();
    private final IFluidHandler fluidHandler = new InputFluidHandler();

    public EndgameTemplateInputBlockEntity(BlockPos pos, BlockState blockState) {
        super(dddsendgame.ENDGAME_TEMPLATE_INPUT_BLOCK_ENTITY.get(), pos, blockState);
    }

    public IItemHandler itemHandler() {
        return this.itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return this.fluidHandler;
    }

    @Nullable
    private EndgameTemplateBlockEntity connectedTemplate() {
        if (this.level == null) {
            return null;
        }

        EndgameTemplateNetwork.Status status = EndgameTemplateNetwork.fromInput(this.level, this.worldPosition);
        return status.hasMultipleControllers() ? null : status.singleController(this.level);
    }

    private class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlot(slot);
            if (stack.isEmpty()) {
                return stack;
            }

            EndgameTemplateBlockEntity template = EndgameTemplateInputBlockEntity.this.connectedTemplate();
            if (template == null) {
                return stack;
            }

            return template.itemHandler().insertItem(SLOT_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlot(slot);
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            validateSlot(slot);
            EndgameTemplateBlockEntity template = EndgameTemplateInputBlockEntity.this.connectedTemplate();
            return template != null && template.itemHandler().isItemValid(SLOT_INPUT, stack);
        }

        private void validateSlot(int slot) {
            if (slot != SLOT_INPUT) {
                throw new RuntimeException("Slot " + slot + " is not in valid range - [0,1)");
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
            EndgameTemplateBlockEntity template = EndgameTemplateInputBlockEntity.this.connectedTemplate();
            return template != null && template.fluidHandler().isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            EndgameTemplateBlockEntity template = EndgameTemplateInputBlockEntity.this.connectedTemplate();
            return template == null ? 0 : template.fluidHandler().fill(resource, action);
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
