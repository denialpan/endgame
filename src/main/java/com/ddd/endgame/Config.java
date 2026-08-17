package com.ddd.endgame;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public enum RenderStageMode implements TranslatableEnum {
        AUTO,
        VANILLA_STENCIL,
        IRIS_SAFE,
        DISABLED;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("xevitia.configuration.skyboxIrisCompatibilityMode." + name().toLowerCase());
        }
    }

    public enum RequirementDetectionMode implements TranslatableEnum {
        AUTO,
        MANUAL;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("xevitia.configuration.requirementDetectionMode." + name().toLowerCase());
        }
    }

    public static final ModConfigSpec.LongValue ITEM_REQUIREMENT = BUILDER
            .comment("Manual mode: how many of each required item stack must be inserted into the galaxy compressor.")
            .defineInRange("itemRequirement", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_REQUIREMENT_MB = BUILDER
            .comment("Manual mode: how many millibuckets of each required fluid must be inserted into the galaxy compressor.")
            .defineInRange("fluidRequirementMb", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue DEBUG_STONE_ONLY = BUILDER
            .comment("Debug mode: require only 20 minecraft:stone and ignore generated item/fluid recipe requirements.")
            .define("debugStoneOnly", false);

    public static final ModConfigSpec.EnumValue<RequirementDetectionMode> REQUIREMENT_DETECTION_MODE = BUILDER
            .comment(
                    "Controls how the galaxy compressor determines the amount required for each item and fluid.",
                    "AUTO starts at 999 for vanilla, uses 9999 if AE2 is loaded, multiplies by 100 if Create is loaded, and multiplies by 1000 if Modern Industrialization is loaded.",
                    "MANUAL uses itemRequirement and fluidRequirementMb.")
            .defineEnum("requirementDetectionMode", RequirementDetectionMode.AUTO);

    public static final ModConfigSpec.BooleanValue THE_STICK_GRANTS_CREATIVE = BUILDER
            .comment("Whether holding The Stick in inventory sets the player to Creative mode.")
            .define("theStickGrantsCreative", true);

    public static final ModConfigSpec.BooleanValue THE_STICK_GRANTS_SERVER_COMMANDS = BUILDER
            .comment("Whether holding The Stick in main hand or offhand grants server command permissions.")
            .define("theStickGrantsServerCommands", false);

    public static final ModConfigSpec.IntValue ENTITY_PURGE_RADIUS = BUILDER
            .comment("Radius in blocks used by the entity purge item.")
            .defineInRange("entityPurgeRadius", 32, 1, 1024);

    public static final ModConfigSpec.BooleanValue ENTITY_PURGE_KILLS_PLAYERS = BUILDER
            .comment("Whether the entity purge item can kill players other than the user.")
            .define("entityPurgeKillsPlayers", false);

    public static final ModConfigSpec.DoubleValue PITCH_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual pitch speed for the endgame compressor inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxPitchRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue YAW_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual yaw speed for the endgame compressor inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxYawRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue ROLL_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual roll speed for the endgame compressor inner skybox rotation, in degrees per second. Use 0 to disable; negative values reverse direction.")
            .defineInRange("skyboxRollRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue RENDER_DISTANCE = CLIENT_BUILDER
            .comment("Client-side maximum distance for rendering endgame skybox windows, in blocks. Use 0 to disable distance culling.")
            .defineInRange("skyboxRenderDistance", 96.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.BooleanValue DROPPED_ITEM_WINDOWS = CLIENT_BUILDER
            .comment("Whether dropped endgame skybox block items render the animated skybox window effect.")
            .define("skyboxDroppedItemWindows", true);

    public static final ModConfigSpec.IntValue MAX_BLOCK_ENTITY_WINDOWS = CLIENT_BUILDER
            .comment("Client-side maximum number of in-world endgame block skybox windows rendered per frame. Use 0 to disable this cap.")
            .defineInRange("skyboxMaxBlockEntityWindows", 4096, 0, 262144);

    public static final ModConfigSpec.DoubleValue DISTANT_ANIMATION_DISTANCE = CLIENT_BUILDER
            .comment("Distance in blocks where skybox animation updates at a reduced rate. Use 0 to disable distant animation throttling.")
            .defineInRange("skyboxDistantAnimationDistance", 48.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.IntValue DISTANT_ANIMATION_FRAME_INTERVAL = CLIENT_BUILDER
            .comment("Render-frame interval for updating distant skybox animation. Higher values reduce animation update rate for distant windows.")
            .defineInRange("skyboxDistantAnimationFrameInterval", 4, 1, 120);

    public static final ModConfigSpec.DoubleValue DISABLE_ROTATION_DISTANCE = CLIENT_BUILDER
            .comment("Distance in blocks where skybox rotation is disabled. Use 0 to keep rotation enabled at all rendered distances.")
            .defineInRange("skyboxDisableRotationDistance", 0.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.EnumValue<RenderStageMode> IRIS_COMPATIBILITY_MODE = CLIENT_BUILDER
            .comment(
                    "Controls the render stage used by in-world endgame skybox stencil windows.",
                    "AUTO uses the standard vanilla stencil stage unless Iris reports an active shaderpack, then uses the shader-safe late stage.",
                    "VANILLA_STENCIL renders after block entities and gives the most vanilla-like depth ordering.",
                    "IRIS_SAFE renders after translucent blocks to avoid common shaderpack depth issues, at the cost of being later in the frame.",
                    "DISABLED skips the in-world skybox window pass.")
            .defineEnum("skyboxIrisCompatibilityMode", RenderStageMode.AUTO);

    static final ModConfigSpec SPEC = BUILDER.build();
    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    public static long itemRequirement() {
        return DEBUG_STONE_ONLY.getAsBoolean() ? 20L : configuredRequirementAmount(ITEM_REQUIREMENT.get());
    }

    public static long fluidRequirementMb() {
        return configuredRequirementAmount(FLUID_REQUIREMENT_MB.get());
    }

    private static long configuredRequirementAmount(long manualAmount) {
        if (REQUIREMENT_DETECTION_MODE.get() == RequirementDetectionMode.MANUAL) {
            return manualAmount;
        }

        long amount = isModLoaded("ae2") ? 9_999L : 999L;
        if (isModLoaded("create")) {
            amount = saturatedMultiply(amount, 100L);
        }
        if (isModLoaded("modern_industrialization")) {
            amount = saturatedMultiply(amount, 1_000L);
        }
        return amount;
    }

    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    private static long saturatedMultiply(long value, long multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }
}
