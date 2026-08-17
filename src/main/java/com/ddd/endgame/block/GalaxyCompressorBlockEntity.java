package com.ddd.endgame.block;

import com.ddd.endgame.Config;
import com.ddd.endgame.galaxy.GalaxyCompressorMenu;
import com.ddd.endgame.galaxy.GalaxyCompressorNetwork;
import com.ddd.endgame.EndgameRequirement;
import com.ddd.endgame.dddsendgame;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class GalaxyCompressorBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private final LinkedHashMap<ResourceLocation, Long> remaining = new LinkedHashMap<>();
    private final LinkedHashMap<ResourceLocation, Long> fluidRemaining = new LinkedHashMap<>();
    private final IItemHandler itemHandler = new ControllerItemHandler();
    private final IFluidHandler fluidHandler = new ControllerFluidHandler();
    private List<EndgameRequirement> cachedRequirements = List.of();
    private boolean requirementsDirty = true;
    private int requirementsRevision;

    public GalaxyCompressorBlockEntity(BlockPos pos, BlockState blockState) {
        super(dddsendgame.GALAXY_COMPRESSOR_BLOCK_ENTITY.get(), pos, blockState);
    }

    public List<EndgameRequirement> requirements() {
        if (this.requirementsDirty) {
            List<EndgameRequirement> requirements = new ArrayList<>();
            long itemRequirement = Config.itemRequirement();
            long fluidRequirement = Config.fluidRequirementMb();
            this.remaining.entrySet().stream()
                    .map(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey())
                            .map(item -> EndgameRequirement.item(entry.getKey(), item, entry.getValue(), itemRequirement))
                            .orElse(null))
                    .filter(requirement -> requirement != null && !requirement.displayStack().is(Items.AIR))
                    .forEach(requirements::add);
            this.fluidRemaining.entrySet().stream()
                    .map(entry -> BuiltInRegistries.FLUID.getOptional(entry.getKey())
                            .map(fluid -> EndgameRequirement.fluid(entry.getKey(), fluid, entry.getValue(), fluidRequirement))
                            .orElse(null))
                    .filter(requirement -> requirement != null && requirement.fluid() && BuiltInRegistries.FLUID.get(requirement.id()) != Fluids.EMPTY)
                    .forEach(requirements::add);
            requirements.sort(Comparator.comparing(EndgameRequirement::complete).thenComparing(requirement -> requirement.id().toString()));
            this.cachedRequirements = List.copyOf(requirements);
            this.requirementsDirty = false;
        }

        return this.cachedRequirements;
    }

    public long totalRemaining() {
        return this.remaining.values().stream().mapToLong(Long::longValue).sum()
                + this.fluidRemaining.values().stream().mapToLong(Long::longValue).sum();
    }

    public long totalRequired() {
        return (long)this.remaining.size() * Config.itemRequirement() + (long)this.fluidRemaining.size() * Config.fluidRequirementMb();
    }

    public int completedRequirementCount() {
        return (int)this.remaining.values().stream().filter(count -> count <= 0L).count()
                + (int)this.fluidRemaining.values().stream().filter(count -> count <= 0L).count();
    }

    public boolean isComplete() {
        return (!this.remaining.isEmpty() || !this.fluidRemaining.isEmpty())
                && this.remaining.values().stream().allMatch(count -> count <= 0L)
                && this.fluidRemaining.values().stream().allMatch(count -> count <= 0L);
    }

    public IItemHandler itemHandler() {
        return this.itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return this.fluidHandler;
    }

    public int requirementsRevision() {
        return this.requirementsRevision;
    }

    public GalaxyCompressorNetwork.Status networkStatus() {
        return this.level == null ? new GalaxyCompressorNetwork.Status(this.worldPosition, 1, 0) : GalaxyCompressorNetwork.fromController(this.level, this.worldPosition);
    }

    public int connectedInputCount() {
        return this.networkStatus().inputCount();
    }

    public boolean hasMultipleConnectedControllers() {
        return this.networkStatus().hasMultipleControllers();
    }

    public void initializeRequirementsFromRecipes() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Map<ResourceLocation, Item> recipeItems = new LinkedHashMap<>();
        Map<ResourceLocation, Fluid> recipeFluids = new LinkedHashMap<>();
        if (Config.DEBUG_STONE_ONLY.getAsBoolean()) {
            recipeItems.put(BuiltInRegistries.ITEM.getKey(Items.STONE), Items.STONE);
        } else {
            addRecipeFluid(recipeFluids, Fluids.WATER);
            addRecipeFluid(recipeFluids, Fluids.LAVA);
            for (RecipeHolder<?> holder : this.level.getRecipeManager().getRecipes()) {
                Object recipe = holder.value();
                addModernIndustrializationMachineOutputs(recipeItems, recipeFluids, recipe);
                if (holder.value().isSpecial()) {
                    continue;
                }

                addRecipeOutputItem(recipeItems, holder.value().getResultItem(this.level.registryAccess()));
            }
        }

        List<ResourceLocation> sorted = new ArrayList<>(recipeItems.keySet());
        sorted.sort(ResourceLocation::compareTo);

        boolean changed = false;
        for (ResourceLocation itemId : sorted) {
            if (!this.remaining.containsKey(itemId)) {
                this.remaining.put(itemId, Config.itemRequirement());
                this.markRequirementsDirty();
                changed = true;
            }
        }

        List<ResourceLocation> sortedFluids = new ArrayList<>(recipeFluids.keySet());
        sortedFluids.sort(ResourceLocation::compareTo);
        for (ResourceLocation fluidId : sortedFluids) {
            if (!this.fluidRemaining.containsKey(fluidId)) {
                this.fluidRemaining.put(fluidId, Config.fluidRequirementMb());
                this.markRequirementsDirty();
                changed = true;
            }
        }

        changed |= clampRequirementsToConfiguredAmounts();

        if (Config.DEBUG_STONE_ONLY.getAsBoolean()) {
            changed |= this.remaining.keySet().removeIf(itemId -> !recipeItems.containsKey(itemId));
            changed |= !this.fluidRemaining.isEmpty();
            this.fluidRemaining.clear();
            ResourceLocation stoneId = BuiltInRegistries.ITEM.getKey(Items.STONE);
            if (this.remaining.getOrDefault(stoneId, Config.itemRequirement()) > Config.itemRequirement()) {
                this.remaining.put(stoneId, Config.itemRequirement());
                changed = true;
            }
            if (changed) {
                this.markRequirementsDirty();
            }
        }

        if (changed) {
            dddsendgame.LOGGER.info("Endgame compressor at {} tracks {} recipe output items and {} fluids", this.worldPosition, this.remaining.size(), this.fluidRemaining.size());
            this.setChangedAndSync();
        }
    }

    private boolean clampRequirementsToConfiguredAmounts() {
        boolean changed = false;
        long itemRequirement = Config.itemRequirement();
        for (Map.Entry<ResourceLocation, Long> entry : this.remaining.entrySet()) {
            if (entry.getValue() > itemRequirement) {
                entry.setValue(itemRequirement);
                changed = true;
            }
        }

        long fluidRequirement = Config.fluidRequirementMb();
        for (Map.Entry<ResourceLocation, Long> entry : this.fluidRemaining.entrySet()) {
            if (entry.getValue() > fluidRequirement) {
                entry.setValue(fluidRequirement);
                changed = true;
            }
        }

        if (changed) {
            this.markRequirementsDirty();
        }
        return changed;
    }

    private static void addRecipeOutputItem(Map<ResourceLocation, Item> recipeItems, ItemStack result) {
        if (result.isEmpty()
                || result.is(dddsendgame.THE_STICK.get())
                || result.is(dddsendgame.GALAXY_COMPRESSOR_ITEM.get())) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (itemId != null) {
            recipeItems.putIfAbsent(itemId, result.getItem());
        }
    }

    private static void addRecipeFluid(Map<ResourceLocation, Fluid> recipeFluids, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return;
        }

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null) {
            recipeFluids.putIfAbsent(fluidId, fluid);
        }
    }

    private static void addModernIndustrializationMachineOutputs(Map<ResourceLocation, Item> recipeItems, Map<ResourceLocation, Fluid> recipeFluids, Object recipe) {
        if (!"aztech.modern_industrialization.machines.recipe.MachineRecipe".equals(recipe.getClass().getName())) {
            return;
        }

        try {
            Field itemOutputsField = recipe.getClass().getField("itemOutputs");
            Object itemOutputs = itemOutputsField.get(recipe);
            if (itemOutputs instanceof Iterable<?> itemOutputEntries) {
                for (Object output : itemOutputEntries) {
                    Method getStack = output.getClass().getMethod("getStack");
                    Object stack = getStack.invoke(output);
                    if (stack instanceof ItemStack itemStack) {
                        addRecipeOutputItem(recipeItems, itemStack);
                    }
                }
            }

            Field fluidOutputsField = recipe.getClass().getField("fluidOutputs");
            Object fluidOutputs = fluidOutputsField.get(recipe);
            if (fluidOutputs instanceof Iterable<?> fluidOutputEntries) {
                for (Object output : fluidOutputEntries) {
                    Method fluid = output.getClass().getMethod("fluid");
                    Object fluidOutput = fluid.invoke(output);
                    if (fluidOutput instanceof Fluid outputFluid) {
                        addRecipeFluid(recipeFluids, outputFluid);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            dddsendgame.LOGGER.debug("Unable to inspect Modern Industrialization machine recipe {}", recipe, exception);
        }
    }

    public int acceptContribution(ItemStack stack) {
        if (stack.isEmpty() || this.level == null || this.level.isClientSide || this.hasMultipleConnectedControllers()) {
            return 0;
        }

        this.initializeRequirementsFromRecipes();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Long current = this.remaining.get(itemId);
        if (current == null || current <= 0L) {
            return 0;
        }

        int accepted = (int)Math.min((long)stack.getCount(), current);
        this.remaining.put(itemId, current - accepted);
        this.markRequirementsDirty();
        stack.shrink(accepted);
        this.setChangedAndSync();
        return accepted;
    }

    public int acceptedContributionCount(ItemStack stack) {
        if (stack.isEmpty() || this.hasMultipleConnectedControllers()) {
            return 0;
        }

        this.initializeRequirementsFromRecipes();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Long current = this.remaining.get(itemId);
        return current == null || current <= 0L ? 0 : (int)Math.min((long)stack.getCount(), current);
    }

    private void resetRequirements() {
        if (this.remaining.isEmpty() && this.fluidRemaining.isEmpty()) {
            return;
        }

        this.remaining.replaceAll((itemId, count) -> Config.itemRequirement());
        this.fluidRemaining.replaceAll((fluidId, count) -> Config.fluidRequirementMb());
        this.markRequirementsDirty();
        this.setChangedAndSync();
    }

    private int acceptFluidContribution(FluidStack stack, IFluidHandler.FluidAction action) {
        if (stack.isEmpty() || this.level == null || this.level.isClientSide || this.hasMultipleConnectedControllers()) {
            return 0;
        }

        this.initializeRequirementsFromRecipes();
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        Long current = this.fluidRemaining.get(fluidId);
        if (current == null || current <= 0L) {
            return 0;
        }

        int accepted = (int)Math.min((long)stack.getAmount(), current);
        if (action.execute()) {
            this.fluidRemaining.put(fluidId, current - accepted);
            this.markRequirementsDirty();
            this.setChangedAndSync();
        }
        return accepted;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.dddsendgame.galaxy_compressor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        this.initializeRequirementsFromRecipes();
        return new GalaxyCompressorMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            ItemStack remainingStack = stack.copy();
            this.acceptContribution(remainingStack);
        }
        this.setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && !this.hasMultipleConnectedControllers() && this.remaining.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), 0L) > 0L;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.remaining.clear();
        this.fluidRemaining.clear();

        ListTag requirements = tag.getList("Requirements", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < requirements.size(); index++) {
            CompoundTag requirement = requirements.getCompound(index);
            ResourceLocation itemId = ResourceLocation.tryParse(requirement.getString("Item"));
            if (itemId != null) {
                this.remaining.put(itemId, requirement.getLong("Remaining"));
            }
        }

        ListTag fluidRequirements = tag.getList("FluidRequirements", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < fluidRequirements.size(); index++) {
            CompoundTag requirement = fluidRequirements.getCompound(index);
            ResourceLocation fluidId = ResourceLocation.tryParse(requirement.getString("Fluid"));
            if (fluidId != null) {
                this.fluidRemaining.put(fluidId, requirement.getLong("Remaining"));
            }
        }

        this.markRequirementsDirty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag requirements = new ListTag();
        for (Map.Entry<ResourceLocation, Long> entry : this.remaining.entrySet()) {
            CompoundTag requirement = new CompoundTag();
            requirement.putString("Item", entry.getKey().toString());
            requirement.putLong("Remaining", entry.getValue());
            requirements.add(requirement);
        }

        tag.put("Requirements", requirements);

        ListTag fluidRequirements = new ListTag();
        for (Map.Entry<ResourceLocation, Long> entry : this.fluidRemaining.entrySet()) {
            CompoundTag requirement = new CompoundTag();
            requirement.putString("Fluid", entry.getKey().toString());
            requirement.putLong("Remaining", entry.getValue());
            fluidRequirements.add(requirement);
        }

        tag.put("FluidRequirements", fluidRequirements);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private void markRequirementsDirty() {
        this.requirementsDirty = true;
        this.requirementsRevision++;
    }

    private class ControllerItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            if (slot == SLOT_OUTPUT && GalaxyCompressorBlockEntity.this.level != null && GalaxyCompressorBlockEntity.this.isComplete()) {
                return new ItemStack(dddsendgame.GALAXY_INGOT.get());
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlot(slot);
            if (slot != SLOT_INPUT || stack.isEmpty()) {
                return stack;
            }

            int accepted = GalaxyCompressorBlockEntity.this.acceptedContributionCount(stack);
            if (accepted <= 0) {
                return stack;
            }

            if (!simulate) {
                ItemStack consumed = stack.copyWithCount(accepted);
                GalaxyCompressorBlockEntity.this.acceptContribution(consumed);
            }

            return accepted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            if (slot != SLOT_OUTPUT || amount <= 0) {
                return ItemStack.EMPTY;
            }

            if (GalaxyCompressorBlockEntity.this.level == null || !GalaxyCompressorBlockEntity.this.isComplete()) {
                return ItemStack.EMPTY;
            }

            ItemStack result = new ItemStack(dddsendgame.GALAXY_INGOT.get());
            if (!simulate) {
                GalaxyCompressorBlockEntity.this.resetRequirements();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            validateSlot(slot);
            return slot == SLOT_INPUT ? 64 : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            validateSlot(slot);
            return slot == SLOT_INPUT && GalaxyCompressorBlockEntity.this.acceptedContributionCount(stack) > 0;
        }

        private void validateSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " is not in valid range - [0," + getSlots() + ")");
            }
        }
    }

    private class ControllerFluidHandler implements IFluidHandler {
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
            return GalaxyCompressorBlockEntity.this.acceptFluidContribution(stack, FluidAction.SIMULATE) > 0;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return GalaxyCompressorBlockEntity.this.acceptFluidContribution(resource, action);
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
