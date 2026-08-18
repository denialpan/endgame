package com.ddd.endgame.block;

import com.ddd.endgame.Xavitia;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class GalaxyFreezerMultiblock {
    private static final Direction SCHEMATIC_FACING = Direction.NORTH;
    private static final int INPUT_CONNECTOR_X = 0;
    private static final int INPUT_CONNECTOR_Y = 3;
    private static final int INPUT_CONNECTOR_Z = 1;
    private static final List<Requirement> REQUIREMENTS = List.of(
            solid(-1, 0, 0), solid(1, 0, 0),
            solid(-1, 0, 1), solid(0, 0, 1), solid(1, 0, 1),
            solid(-1, 0, 2), solid(0, 0, 2), solid(1, 0, 2),

            glass(-1, 1, 0), glass(0, 1, 0), glass(1, 1, 0),
            glass(-1, 1, 1), glass(1, 1, 1),
            glass(-1, 1, 2), glass(0, 1, 2), glass(1, 1, 2),

            glass(-1, 2, 0), glass(0, 2, 0), glass(1, 2, 0),
            glass(-1, 2, 1), glass(1, 2, 1),
            glass(-1, 2, 2), glass(0, 2, 2), glass(1, 2, 2),

            solid(-1, 3, 0), solid(0, 3, 0), solid(1, 3, 0),
            solid(-1, 3, 1), connector(0, 3, 1), solid(1, 3, 1),
            solid(-1, 3, 2), solid(0, 3, 2), solid(1, 3, 2)
    );

    private GalaxyFreezerMultiblock() {
    }

    public static boolean matches(Level level, BlockPos compressorPos, Direction compressorFacing) {
        if (level == null) {
            return false;
        }

        for (PreviewBlock previewBlock : previewBlocks(compressorPos, compressorFacing)) {
            if (!previewBlock.matches(level.getBlockState(previewBlock.pos()))) {
                return false;
            }
        }
        return true;
    }

    public static List<PreviewBlock> previewBlocks(BlockPos compressorPos, Direction compressorFacing) {
        RotationSteps rotation = RotationSteps.from(SCHEMATIC_FACING, compressorFacing);
        List<PreviewBlock> blocks = new ArrayList<>(REQUIREMENTS.size());
        for (Requirement requirement : REQUIREMENTS) {
            BlockPos targetPos = compressorPos.offset(rotation.rotateX(requirement.x(), requirement.z()), requirement.y(), rotation.rotateZ(requirement.x(), requirement.z()));
            blocks.add(new PreviewBlock(targetPos, requirement.block().defaultBlockState(), requirement.predicate()));
        }
        return blocks;
    }

    public static BlockPos inputConnectorPos(BlockPos compressorPos, Direction compressorFacing) {
        RotationSteps rotation = RotationSteps.from(SCHEMATIC_FACING, compressorFacing);
        return compressorPos.offset(
                rotation.rotateX(INPUT_CONNECTOR_X, INPUT_CONNECTOR_Z),
                INPUT_CONNECTOR_Y,
                rotation.rotateZ(INPUT_CONNECTOR_X, INPUT_CONNECTOR_Z)
        );
    }

    public static BlockPos compressorPosForInputConnector(BlockPos connectorPos, Direction compressorFacing) {
        RotationSteps rotation = RotationSteps.from(SCHEMATIC_FACING, compressorFacing);
        return connectorPos.offset(
                -rotation.rotateX(INPUT_CONNECTOR_X, INPUT_CONNECTOR_Z),
                -INPUT_CONNECTOR_Y,
                -rotation.rotateZ(INPUT_CONNECTOR_X, INPUT_CONNECTOR_Z)
        );
    }

    private static Requirement solid(int x, int y, int z) {
        return new Requirement(x, y, z, Xavitia.GALAXY_OBSIDIAN_BLOCK.get(), state -> state.is(Xavitia.GALAXY_OBSIDIAN_BLOCK.get()));
    }

    private static Requirement glass(int x, int y, int z) {
        return new Requirement(x, y, z, Xavitia.GALAXY_GLASS_BLOCK.get(), GalaxyFreezerMultiblock::isAcceptedGlass);
    }

    private static boolean isAcceptedGlass(BlockState state) {
        return state.is(Xavitia.GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.BLUE_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.GREEN_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.RED_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.RAINBOW_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.YELLOW_GALAXY_GLASS_BLOCK.get())
                || state.is(Xavitia.GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.BLUE_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.GREEN_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.RED_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.RAINBOW_GALAXY_FULL_GLASS_BLOCK.get())
                || state.is(Xavitia.YELLOW_GALAXY_FULL_GLASS_BLOCK.get());
    }

    private static Requirement connector(int x, int y, int z) {
        return new Requirement(x, y, z, Xavitia.GALAXY_CONNECTOR_BLOCK.get(), state ->
                state.is(Xavitia.GALAXY_CONNECTOR_BLOCK.get())
                        && state.getValue(OrientableEntityBlock.AXIS) == Direction.Axis.Y
        );
    }

    public record PreviewBlock(BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        public boolean matches(BlockState actualState) {
            return this.predicate.test(actualState);
        }
    }

    private record Requirement(int x, int y, int z, Block block, Predicate<BlockState> predicate) {
        private boolean matches(BlockState state) {
            return this.predicate.test(state);
        }
    }

    private enum RotationSteps {
        NONE,
        CLOCKWISE_90,
        CLOCKWISE_180,
        COUNTERCLOCKWISE_90;

        private static RotationSteps from(Direction from, Direction to) {
            int steps = Math.floorMod(to.get2DDataValue() - from.get2DDataValue(), 4);
            return switch (steps) {
                case 1 -> CLOCKWISE_90;
                case 2 -> CLOCKWISE_180;
                case 3 -> COUNTERCLOCKWISE_90;
                default -> NONE;
            };
        }

        private int rotateX(int x, int z) {
            return switch (this) {
                case NONE -> x;
                case CLOCKWISE_90 -> -z;
                case CLOCKWISE_180 -> -x;
                case COUNTERCLOCKWISE_90 -> z;
            };
        }

        private int rotateZ(int x, int z) {
            return switch (this) {
                case NONE -> z;
                case CLOCKWISE_90 -> x;
                case CLOCKWISE_180 -> -z;
                case COUNTERCLOCKWISE_90 -> -x;
            };
        }
    }
}
