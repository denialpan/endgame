package com.ddd.endgame.item;

import com.ddd.endgame.RandomBlockPlacerItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class RandomBlockPlacerItem extends Item {
    private static final String BLOCK_INDEX_TAG = "BlockFabricatorIndex";
    private static List<Block> placeableBlocks;

    public RandomBlockPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RandomBlockPlacerItemRenderer.INSTANCE;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.random_block_placer.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.translatable("item.dddsendgame.random_block_placer.selected", selectedBlock(stack).getName()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        return extractSelectedStack(level, player, stack)
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && context.isSecondaryUseActive()) {
            return extractSelectedStack(level, player, stack)
                    ? InteractionResult.sidedSuccess(level.isClientSide)
                    : InteractionResult.FAIL;
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = level.getBlockState(context.getClickedPos()).canBeReplaced(placeContext)
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = selectedPlaceableState(stack, level, placePos, player);
        if (state.isAir() || !level.setBlock(placePos, state, 11)) {
            return InteractionResult.FAIL;
        }

        BlockState placedState = level.getBlockState(placePos);
        SoundType soundType = placedState.getSoundType(level, placePos, player);
        level.playSound(
                player,
                placePos,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, placePos, GameEvent.Context.of(player, placedState));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack selected = selectedItemStack(stack, Integer.MAX_VALUE);
        if (selected.isEmpty()) {
            return false;
        }

        ItemStack remainder = slot.safeInsert(selected.copy(), selected.getCount());
        return remainder.getCount() != selected.getCount();
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack selected = selectedItemStack(stack, Integer.MAX_VALUE);
        if (selected.isEmpty()) {
            return false;
        }
        if (!other.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(other, selected) || other.getCount() >= other.getMaxStackSize()) {
                return false;
            }
            ItemStack filled = other.copy();
            filled.setCount(Math.min(filled.getMaxStackSize(), filled.getCount() + selected.getCount()));
            access.set(filled);
            return true;
        }
        access.set(selected);
        return true;
    }

    public static Block selectedBlock(ItemStack stack) {
        List<Block> blocks = placeableBlocks();
        if (blocks.isEmpty()) {
            return Blocks.AIR;
        }
        return blocks.get(Mth.positiveModulo(selectedIndex(stack), blocks.size()));
    }

    public static ItemStack selectedItemStack(ItemStack fabricatorStack, int amount) {
        Item item = selectedBlock(fabricatorStack).asItem();
        if (item == net.minecraft.world.item.Items.AIR || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
        return stack;
    }

    public static IItemHandler infiniteItemHandler(ItemStack fabricatorStack) {
        return new InfiniteSelectedBlockHandler(fabricatorStack);
    }

    private static boolean extractSelectedStack(Level level, Player player, ItemStack fabricatorStack) {
        ItemStack extracted = selectedItemStack(fabricatorStack, Integer.MAX_VALUE);
        if (extracted.isEmpty()) {
            return false;
        }

        if (!level.isClientSide) {
            ItemStack toGive = extracted.copy();
            if (!player.getInventory().add(toGive) || !toGive.isEmpty()) {
                player.drop(toGive, false);
            }
        }
        return true;
    }

    public static Block cycleSelectedBlock(ItemStack stack, int direction) {
        List<Block> blocks = placeableBlocks();
        if (blocks.isEmpty()) {
            return Blocks.AIR;
        }

        int step = direction >= 0 ? 1 : -1;
        int index = Mth.positiveModulo(selectedIndex(stack) + step, blocks.size());
        setSelectedIndex(stack, index);
        return blocks.get(index);
    }

    private static int selectedIndex(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(BLOCK_INDEX_TAG);
    }

    private static void setSelectedIndex(ItemStack stack, int index) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(BLOCK_INDEX_TAG, index));
    }

    private static BlockState selectedPlaceableState(ItemStack stack, Level level, BlockPos placePos, Player player) {
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        BlockState state = selectedBlock(stack).defaultBlockState();
        if (!state.isAir()
                && state.getBlock().isEnabled(level.enabledFeatures())
                && state.canSurvive(level, placePos)
                && level.isUnobstructed(state, placePos, collisionContext)) {
            return state;
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static List<Block> placeableBlocks() {
        if (placeableBlocks != null) {
            return placeableBlocks;
        }

        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR || block.defaultBlockState().isAir()) {
                continue;
            }
            blocks.add(block);
        }
        blocks.sort(Comparator
                .comparing((Block block) -> block.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        placeableBlocks = List.copyOf(blocks);
        return placeableBlocks;
    }

    private static class InfiniteSelectedBlockHandler implements IItemHandler {
        private final ItemStack fabricatorStack;

        private InfiniteSelectedBlockHandler(ItemStack fabricatorStack) {
            this.fabricatorStack = fabricatorStack;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? selectedItemStack(this.fabricatorStack, Integer.MAX_VALUE) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? selectedItemStack(this.fabricatorStack, amount) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? selectedItemStack(this.fabricatorStack, Integer.MAX_VALUE).getMaxStackSize() : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
