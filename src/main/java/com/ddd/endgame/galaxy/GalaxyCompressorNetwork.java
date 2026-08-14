package com.ddd.endgame.galaxy;

import com.ddd.endgame.block.GalaxyConnectorBlockEntity;
import com.ddd.endgame.block.GalaxyCompressorBlockEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class GalaxyCompressorNetwork {
    private GalaxyCompressorNetwork() {
    }

    public static Status fromController(Level level, BlockPos compressorPos) {
        Set<BlockPos> compressors = new HashSet<>();
        Set<BlockPos> inputs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        compressors.add(compressorPos);
        addAdjacentInputs(level, compressorPos, inputs, queue);
        scan(level, inputs, queue, compressors);
        return new Status(compressors.size() == 1 ? compressors.iterator().next() : null, compressors.size(), inputs.size());
    }

    public static Status fromInput(Level level, BlockPos inputPos) {
        Set<BlockPos> compressors = new HashSet<>();
        Set<BlockPos> inputs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        inputs.add(inputPos);
        queue.add(inputPos);
        scan(level, inputs, queue, compressors);
        return new Status(compressors.size() == 1 ? compressors.iterator().next() : null, compressors.size(), inputs.size());
    }

    private static void scan(Level level, Set<BlockPos> inputs, Queue<BlockPos> queue, Set<BlockPos> compressors) {
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = current.relative(direction);
                BlockEntity adjacent = level.getBlockEntity(adjacentPos);
                if (adjacent instanceof GalaxyCompressorBlockEntity) {
                    compressors.add(adjacentPos);
                } else if (adjacent instanceof GalaxyConnectorBlockEntity && inputs.add(adjacentPos)) {
                    queue.add(adjacentPos);
                }
            }
        }
    }

    private static void addAdjacentInputs(Level level, BlockPos pos, Set<BlockPos> inputs, Queue<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            if (level.getBlockEntity(adjacentPos) instanceof GalaxyConnectorBlockEntity && inputs.add(adjacentPos)) {
                queue.add(adjacentPos);
            }
        }
    }

    public record Status(@Nullable BlockPos compressorPos, int compressorCount, int inputCount) {
        public boolean hasMultipleControllers() {
            return this.compressorCount > 1;
        }

        @Nullable
        public GalaxyCompressorBlockEntity singleController(Level level) {
            if (this.compressorPos == null) {
                return null;
            }

            return level.getBlockEntity(this.compressorPos) instanceof GalaxyCompressorBlockEntity compressor ? compressor : null;
        }
    }
}
