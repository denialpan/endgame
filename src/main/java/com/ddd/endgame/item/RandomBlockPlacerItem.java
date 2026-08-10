package com.ddd.endgame.item;

import com.ddd.endgame.RandomBlockPlacerItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class RandomBlockPlacerItem extends Item {
    private static final int MAX_RANDOM_ATTEMPTS = 256;

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
        tooltipComponents.add(Component.translatable("item.dddsendgame.random_block_placer.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = level.getBlockState(context.getClickedPos()).canBeReplaced(placeContext)
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && !player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = randomPlaceableState(level, placePos, player);
        if (state == null || !level.setBlock(placePos, state, 11)) {
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

    private static BlockState randomPlaceableState(Level level, BlockPos placePos, Player player) {
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (int attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
            Optional<Holder.Reference<Block>> blockHolder = BuiltInRegistries.BLOCK.getRandom(level.random);
            if (blockHolder.isEmpty()) {
                continue;
            }

            BlockState state = blockHolder.get().value().defaultBlockState();
            if (state.isAir()
                    || !state.getBlock().isEnabled(level.enabledFeatures())
                    || !state.canSurvive(level, placePos)
                    || !level.isUnobstructed(state, placePos, collisionContext)) {
                continue;
            }
            return state;
        }
        return null;
    }
}
