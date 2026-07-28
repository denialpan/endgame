package com.ddd.endgame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
import net.neoforged.neoforge.items.IItemHandler;

public class EndgameTemplateBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final LinkedHashMap<ResourceLocation, Long> remaining = new LinkedHashMap<>();
    private final IItemHandler itemHandler = new TemplateItemHandler();
    private List<EndgameRequirement> cachedRequirements = List.of();
    private boolean requirementsDirty = true;
    private int requirementsRevision;
    private boolean outputClaimed;

    public EndgameTemplateBlockEntity(BlockPos pos, BlockState blockState) {
        super(dddsendgame.ENDGAME_TEMPLATE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public List<EndgameRequirement> requirements() {
        if (this.requirementsDirty) {
            this.cachedRequirements = this.remaining.entrySet().stream()
                    .map(entry -> BuiltInRegistries.ITEM.getOptional(entry.getKey())
                            .map(item -> new EndgameRequirement(entry.getKey(), item, entry.getValue()))
                            .orElse(null))
                    .filter(requirement -> requirement != null && requirement.item() != Items.AIR)
                    .sorted(Comparator.comparing(EndgameRequirement::complete).thenComparing(requirement -> requirement.itemId().toString()))
                    .toList();
            this.requirementsDirty = false;
        }

        return this.cachedRequirements;
    }

    public long totalRemaining() {
        return this.remaining.values().stream().mapToLong(Long::longValue).sum();
    }

    public long totalRequired() {
        return (long)this.remaining.size() * dddsendgame.ENDGAME_ITEM_REQUIREMENT;
    }

    public int completedRequirementCount() {
        return (int)this.remaining.values().stream().filter(count -> count <= 0L).count();
    }

    public boolean isComplete() {
        return !this.remaining.isEmpty() && this.remaining.values().stream().allMatch(count -> count <= 0L);
    }

    public IItemHandler itemHandler() {
        return this.itemHandler;
    }

    public int requirementsRevision() {
        return this.requirementsRevision;
    }

    public void initializeRequirementsFromRecipes() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Map<ResourceLocation, Item> recipeItems = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : this.level.getRecipeManager().getRecipes()) {
            ItemStack result = holder.value().getResultItem(this.level.registryAccess());
            if (holder.value().isSpecial()
                    || result.isEmpty()
                    || result.is(dddsendgame.ENDGAME_TEST_STICK.get())
                    || result.is(dddsendgame.ENDGAME_TEMPLATE_ITEM.get())) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (itemId != null) {
                recipeItems.putIfAbsent(itemId, result.getItem());
            }
        }

        List<ResourceLocation> sorted = new ArrayList<>(recipeItems.keySet());
        sorted.sort(ResourceLocation::compareTo);

        boolean changed = false;
        for (ResourceLocation itemId : sorted) {
            if (!this.remaining.containsKey(itemId)) {
                this.remaining.put(itemId, dddsendgame.ENDGAME_ITEM_REQUIREMENT);
                this.markRequirementsDirty();
                changed = true;
            }
        }

        if (changed) {
            dddsendgame.LOGGER.info("Endgame template at {} tracks {} recipe output items", this.worldPosition, this.remaining.size());
            this.setChangedAndSync();
        }
    }

    public int acceptContribution(ItemStack stack) {
        if (stack.isEmpty() || this.level == null || this.level.isClientSide) {
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
        this.ensureResult();
        this.setChangedAndSync();
        return accepted;
    }

    public int acceptedContributionCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        this.initializeRequirementsFromRecipes();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Long current = this.remaining.get(itemId);
        return current == null || current <= 0L ? 0 : (int)Math.min((long)stack.getCount(), current);
    }

    private void ensureResult() {
        if (this.isComplete() && !this.outputClaimed && this.items.get(SLOT_OUTPUT).isEmpty() && this.level != null) {
            this.items.set(SLOT_OUTPUT, EndgameTestRecipe.createResult(this.level.registryAccess()));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.dddsendgame.endgame_template");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        this.initializeRequirementsFromRecipes();
        this.ensureResult();
        return new EndgameTemplateMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == SLOT_OUTPUT) {
            this.ensureResult();
        }
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == SLOT_OUTPUT) {
            this.ensureResult();
        }

        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            if (slot == SLOT_OUTPUT && this.items.get(SLOT_OUTPUT).isEmpty()) {
                this.outputClaimed = true;
            }
            this.setChangedAndSync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(this.items, slot);
        if (slot == SLOT_OUTPUT && !removed.isEmpty()) {
            this.outputClaimed = true;
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            ItemStack remainingStack = stack.copy();
            this.acceptContribution(remainingStack);
            this.items.set(SLOT_INPUT, remainingStack);
        } else if (slot == SLOT_OUTPUT) {
            this.items.set(SLOT_OUTPUT, stack);
        }
        this.setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        this.setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && this.remaining.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), 0L) > 0L;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.remaining.clear();

        ListTag requirements = tag.getList("Requirements", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < requirements.size(); index++) {
            CompoundTag requirement = requirements.getCompound(index);
            ResourceLocation itemId = ResourceLocation.tryParse(requirement.getString("Item"));
            if (itemId != null) {
                this.remaining.put(itemId, requirement.getLong("Remaining"));
            }
        }

        this.outputClaimed = tag.getBoolean("OutputClaimed");
        ContainerHelper.loadAllItems(tag, this.items, registries);
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
        tag.putBoolean("OutputClaimed", this.outputClaimed);
        ContainerHelper.saveAllItems(tag, this.items, registries);
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

    private class TemplateItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            validateSlot(slot);
            if (slot == SLOT_OUTPUT) {
                EndgameTemplateBlockEntity.this.ensureResult();
            }
            return EndgameTemplateBlockEntity.this.items.get(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            validateSlot(slot);
            if (slot != SLOT_INPUT || stack.isEmpty()) {
                return stack;
            }

            int accepted = EndgameTemplateBlockEntity.this.acceptedContributionCount(stack);
            if (accepted <= 0) {
                return stack;
            }

            if (!simulate) {
                ItemStack consumed = stack.copyWithCount(accepted);
                EndgameTemplateBlockEntity.this.acceptContribution(consumed);
            }

            return accepted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validateSlot(slot);
            if (slot != SLOT_OUTPUT || amount <= 0) {
                return ItemStack.EMPTY;
            }

            EndgameTemplateBlockEntity.this.ensureResult();
            ItemStack output = EndgameTemplateBlockEntity.this.items.get(SLOT_OUTPUT);
            if (output.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int extracted = Math.min(amount, output.getCount());
            ItemStack result = output.copyWithCount(extracted);
            if (!simulate) {
                EndgameTemplateBlockEntity.this.removeItem(SLOT_OUTPUT, extracted);
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
            return slot == SLOT_INPUT && EndgameTemplateBlockEntity.this.acceptedContributionCount(stack) > 0;
        }

        private void validateSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " is not in valid range - [0," + getSlots() + ")");
            }
        }
    }
}
