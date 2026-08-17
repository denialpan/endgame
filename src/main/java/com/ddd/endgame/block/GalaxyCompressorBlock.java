package com.ddd.endgame.block;

import com.ddd.endgame.Xevitia;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class GalaxyCompressorBlock extends HorizontalFacingEntityBlock {
    public static final MapCodec<GalaxyCompressorBlock> CODEC = simpleCodec(GalaxyCompressorBlock::new);

    public GalaxyCompressorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalFacingEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GalaxyCompressorBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && level.getBlockEntity(pos) instanceof GalaxyCompressorBlockEntity blockEntity) {
            popResource(level, pos, savedControllerStack(blockEntity, (ServerLevel)level));
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (FluidUtil.getFluidContained(stack).isPresent()) {
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }

            if (tryDepositHeldFluid(level, pos, player, hand, stack)) {
                return ItemInteractionResult.SUCCESS;
            }
        }

        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean tryDepositHeldFluid(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof GalaxyCompressorBlockEntity blockEntity)) {
            return false;
        }

        blockEntity.initializeRequirementsFromRecipes();
        FluidActionResult result = FluidUtil.tryEmptyContainerAndStow(
                stack,
                blockEntity.fluidHandler(),
                new InvWrapper(player.getInventory()),
                Integer.MAX_VALUE,
                player,
                true
        );
        if (!result.isSuccess()) {
            return false;
        }

        player.setItemInHand(hand, result.getResult());
        return true;
    }

    private static void open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GalaxyCompressorBlockEntity blockEntity) {
            blockEntity.initializeRequirementsFromRecipes();
            serverPlayer.openMenu(blockEntity, buffer -> buffer.writeBlockPos(pos));
        }
    }

    private static ItemStack savedControllerStack(GalaxyCompressorBlockEntity blockEntity, ServerLevel level) {
        ItemStack stack = new ItemStack(Xevitia.GALAXY_COMPRESSOR_ITEM.get());
        blockEntity.saveToItem(stack, level.registryAccess());
        return stack;
    }
}
