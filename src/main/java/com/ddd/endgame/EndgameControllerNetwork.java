package com.ddd.endgame;

import com.ddd.endgame.block.EndgameConnectorBlockEntity;
import com.ddd.endgame.block.EndgameControllerBlockEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class EndgameControllerNetwork {
    private EndgameControllerNetwork() {
    }

    public static Status fromController(Level level, BlockPos controllerPos) {
        Set<BlockPos> controllers = new HashSet<>();
        Set<BlockPos> inputs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        controllers.add(controllerPos);
        addAdjacentInputs(level, controllerPos, inputs, queue);
        scan(level, inputs, queue, controllers);
        return new Status(controllers.size() == 1 ? controllers.iterator().next() : null, controllers.size(), inputs.size());
    }

    public static Status fromInput(Level level, BlockPos inputPos) {
        Set<BlockPos> controllers = new HashSet<>();
        Set<BlockPos> inputs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        inputs.add(inputPos);
        queue.add(inputPos);
        scan(level, inputs, queue, controllers);
        return new Status(controllers.size() == 1 ? controllers.iterator().next() : null, controllers.size(), inputs.size());
    }

    private static void scan(Level level, Set<BlockPos> inputs, Queue<BlockPos> queue, Set<BlockPos> controllers) {
        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos adjacentPos = current.relative(direction);
                BlockEntity adjacent = level.getBlockEntity(adjacentPos);
                if (adjacent instanceof EndgameControllerBlockEntity) {
                    controllers.add(adjacentPos);
                } else if (adjacent instanceof EndgameConnectorBlockEntity && inputs.add(adjacentPos)) {
                    queue.add(adjacentPos);
                }
            }
        }
    }

    private static void addAdjacentInputs(Level level, BlockPos pos, Set<BlockPos> inputs, Queue<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            if (level.getBlockEntity(adjacentPos) instanceof EndgameConnectorBlockEntity && inputs.add(adjacentPos)) {
                queue.add(adjacentPos);
            }
        }
    }

    public record Status(@Nullable BlockPos controllerPos, int controllerCount, int inputCount) {
        public boolean hasMultipleControllers() {
            return this.controllerCount > 1;
        }

        @Nullable
        public EndgameControllerBlockEntity singleController(Level level) {
            if (this.controllerPos == null) {
                return null;
            }

            return level.getBlockEntity(this.controllerPos) instanceof EndgameControllerBlockEntity controller ? controller : null;
        }
    }
}
