package com.ddd.endgame.block;

import com.ddd.endgame.dddsendgame;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EndgameDecorativeBlockEntity extends BlockEntity {
    public EndgameDecorativeBlockEntity(BlockPos pos, BlockState blockState) {
        super(dddsendgame.ENDGAME_DECORATIVE_BLOCK_ENTITY.get(), pos, blockState);
    }
}
