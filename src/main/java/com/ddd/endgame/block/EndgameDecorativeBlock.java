package com.ddd.endgame.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import com.ddd.endgame.GalaxyInstability;
import javax.annotation.Nullable;
import com.ddd.endgame.dddsendgame;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class EndgameDecorativeBlock extends BaseEntityBlock {
    public static final MapCodec<EndgameDecorativeBlock> CODEC = simpleCodec(EndgameDecorativeBlock::new);

    public EndgameDecorativeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndgameDecorativeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!state.is(dddsendgame.GALAXY_BLOCK.get())) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof EndgameDecorativeBlockEntity blockEntity) {
            blockEntity.setGalaxyInstabilityTicks(GalaxyInstability.carriedTicks(stack, placer));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == dddsendgame.ENDGAME_DECORATIVE_BLOCK_ENTITY.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> EndgameDecorativeBlockEntity.tick(tickerLevel, pos, tickerState, (EndgameDecorativeBlockEntity) blockEntity)
                : null;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                return false;
            }
        });
    }
}
