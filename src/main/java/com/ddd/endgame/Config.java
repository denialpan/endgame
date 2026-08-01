package com.ddd.endgame;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue ITEM_REQUIREMENT = BUILDER
            .comment("How many of each required item stack must be inserted into the endgame controller.")
            .defineInRange("itemRequirement", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_REQUIREMENT_MB = BUILDER
            .comment("How many millibuckets of each required fluid must be inserted into the endgame controller.")
            .defineInRange("fluidRequirementMb", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue DEBUG_STONE_ONLY = BUILDER
            .comment("Debug mode: require only 20 minecraft:stone and ignore generated item/fluid recipe requirements.")
            .define("debugStoneOnly", false);

    public static final ModConfigSpec.BooleanValue ENDGAME_STICK_GRANTS_CREATIVE = BUILDER
            .comment("Whether holding the endgame stick in inventory sets the player to Creative mode.")
            .define("endgameStickGrantsCreative", true);

    public static final ModConfigSpec.DoubleValue SKYBOX_PITCH_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual pitch speed for the endgame controller inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxPitchRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue SKYBOX_YAW_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual yaw speed for the endgame controller inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxYawRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue SKYBOX_ROLL_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual roll speed for the endgame controller inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxRollRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    static final ModConfigSpec SPEC = BUILDER.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    public static long itemRequirement() {
        return DEBUG_STONE_ONLY.getAsBoolean() ? 20L : ITEM_REQUIREMENT.get();
    }

    public static long fluidRequirementMb() {
        return FLUID_REQUIREMENT_MB.get();
    }
}
