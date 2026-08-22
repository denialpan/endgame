package com.ddd.endgame;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            return Component.translatable("xavitia.configuration.skyboxIrisCompatibilityMode." + name().toLowerCase());
        }
    }

    public enum RequirementDetectionMode implements TranslatableEnum {
        AUTO,
        MANUAL;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("xavitia.configuration.requirementDetectionMode." + name().toLowerCase());
        }
    }

    public static final ModConfigSpec.LongValue ITEM_REQUIREMENT = BUILDER
            .comment("How many of each item stack is required.")
            .defineInRange("itemRequirement", 999L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_REQUIREMENT_MB = BUILDER
            .comment("How many millibuckets of each fluid is required.")
            .defineInRange("fluidRequirementMb", 999L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.EnumValue<RequirementDetectionMode> REQUIREMENT_DETECTION_MODE = BUILDER
            .comment(
                    "Auto calculates requiresments based on supported installed mods:",
                    "vanilla: 999\nAE2: 9999\nCreate: x100\nModern Industrialization: x1000")
            .defineEnum("requirementDetectionMode", RequirementDetectionMode.AUTO);

    public static final ModConfigSpec.BooleanValue THE_STICK_GRANTS_CREATIVE = BUILDER
            .comment("Whether holding The Stick in inventory sets the player to Creative mode.")
            .define("theStickGrantsCreative", true);

    public static final ModConfigSpec.BooleanValue THE_STICK_GRANTS_SERVER_COMMANDS = BUILDER
            .comment("Whether holding The Stick in inventory grants server commands.")
            .define("theStickGrantsServerCommands", false);

    public static final ModConfigSpec.IntValue ENTITY_KILLED_RADIUS = BUILDER
            .comment("Killing radius in blocks used by the mob annihilator.")
            .defineInRange("entityKilledRadius", 32, 1, 1024);

    public static final ModConfigSpec.BooleanValue ENTITY_KILLED_KILLS_PLAYERS = BUILDER
            .comment("Whether the mob annihilator can kill other players.")
            .define("entityKilledKillsPlayers", false);

    public static final ModConfigSpec.DoubleValue PITCH_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual pitch speed galaxy effect, in degrees per second. Use 0 to disable, negative values reverse direction.")
            .defineInRange("skyboxPitchRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue YAW_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual yaw speed galaxy effect, in degrees per second. Use 0 to disable, negative values reverse direction.")
            .defineInRange("skyboxYawRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue ROLL_ROTATION_SPEED = CLIENT_BUILDER
            .comment("Client-side visual roll speed galaxy effect, in degrees per second. Use 0 to disable, negative values reverse direction.")
            .defineInRange("skyboxRollRotationSpeedDegreesPerSecond", 0.1D, -360.0D, 360.0D);

    public static final ModConfigSpec.DoubleValue RENDER_DISTANCE = CLIENT_BUILDER
            .comment("Client-side maximum distance for galaxy effect, in blocks. Use 0 to disable distance culling.")
            .defineInRange("skyboxRenderDistance", 96.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.BooleanValue DROPPED_ITEM_WINDOWS = CLIENT_BUILDER
            .comment("Whether dropped galaxy block items render the animated skybox window effect.")
            .define("skyboxDroppedItemWindows", true);

    public static final ModConfigSpec.IntValue MAX_BLOCK_ENTITY_WINDOWS = CLIENT_BUILDER
            .comment("Client-side maximum galaxy items rendered per frame. Use 0 to disable this cap.")
            .defineInRange("skyboxMaxBlockEntityWindows", 4096, 0, 262144);

    public static final ModConfigSpec.DoubleValue DISTANT_ANIMATION_DISTANCE = CLIENT_BUILDER
            .comment("Distance in blocks where galaxy effect updates at a reduced rate. Use 0 to disable distant animation throttling.")
            .defineInRange("skyboxDistantAnimationDistance", 48.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.IntValue DISTANT_ANIMATION_FRAME_INTERVAL = CLIENT_BUILDER
            .comment("Render-frame interval for updating distant galaxy effect. Higher values reduce animation update rate for distant windows.")
            .defineInRange("skyboxDistantAnimationFrameInterval", 4, 1, 120);

    public static final ModConfigSpec.DoubleValue DISABLE_ROTATION_DISTANCE = CLIENT_BUILDER
            .comment("Distance in blocks where galaxy rotation rotation is disabled. Use 0 to keep rotation enabled at all rendered distances.")
            .defineInRange("skyboxDisableRotationDistance", 0.0D, 0.0D, 1024.0D);

    public static final ModConfigSpec.EnumValue<RenderStageMode> IRIS_COMPATIBILITY_MODE = CLIENT_BUILDER
            .comment(
                    "Controls the render stage used the galaxy effect.",
                    "AUTO: uses the standard vanilla stencil stage unless Iris reports an active shaderpack.\n",
                    "VANILLA_STENCIL: renders after block entities and gives the most compatibility.\n",
                    "IRIS_SAFE: renders after translucent blocks to avoid common shader depth issues, at the cost of being later in the frame.\n",
                    "DISABLED: disables the galaxy effect.")
            .defineEnum("skyboxIrisCompatibilityMode", RenderStageMode.AUTO);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIREMENT_MOD_BLACKLIST = BUILDER
            .comment("Mod ids whose items and fluids should not be detected as Galaxy Compressor requirements. Example: [\"minecraft\", \"create\"]")
            .defineListAllowEmpty("requirementModBlacklist", List.of(), () -> "minecraft", Config::isValidModIdConfigValue);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIREMENT_ITEM_BLACKLIST = BUILDER
            .comment("Item ids that should not be detected as Galaxy Compressor requirements. Example: [\"minecraft:stone\", \"minecraft:dirt\"]")
            .defineListAllowEmpty("requirementItemBlacklist", List.of(), () -> "minecraft:stone", Config::isValidResourceLocationConfigValue);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REQUIREMENT_FLUID_BLACKLIST = BUILDER
            .comment("Fluid ids that should not be detected as Galaxy Compressor requirements. Example: [\"minecraft:water\", \"minecraft:lava\"]")
            .defineListAllowEmpty("requirementFluidBlacklist", List.of(), () -> "minecraft:water", Config::isValidResourceLocationConfigValue);

    public static final ModConfigSpec.BooleanValue DEBUG_STONE_ONLY = BUILDER
            .comment("Debug mode: require only 20 minecraft:stone and ignore all item/fluid recipe requirements.")
            .define("debugStoneOnly", false);


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

    public static boolean isRequirementModBlacklisted(String modId) {
        return requirementModBlacklist().contains(modId.toLowerCase(Locale.ROOT));
    }

    public static boolean isRequirementItemBlacklisted(ResourceLocation itemId) {
        return requirementItemBlacklist().contains(itemId);
    }

    public static boolean isRequirementFluidBlacklisted(ResourceLocation fluidId) {
        return requirementFluidBlacklist().contains(fluidId);
    }

    public static String requirementBlacklistSignature() {
        return String.join(",", requirementModBlacklist())
                + "|" + requirementItemBlacklist()
                + "|" + requirementFluidBlacklist();
    }

    private static Set<String> requirementModBlacklist() {
        Set<String> modIds = new LinkedHashSet<>();
        for (String modId : REQUIREMENT_MOD_BLACKLIST.get()) {
            modIds.add(modId.toLowerCase(Locale.ROOT));
        }
        return modIds;
    }

    private static Set<ResourceLocation> requirementItemBlacklist() {
        Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        for (String id : REQUIREMENT_ITEM_BLACKLIST.get()) {
            ResourceLocation itemId = ResourceLocation.tryParse(id);
            if (itemId != null) {
                itemIds.add(itemId);
            }
        }
        return itemIds;
    }

    private static Set<ResourceLocation> requirementFluidBlacklist() {
        Set<ResourceLocation> fluidIds = new LinkedHashSet<>();
        for (String id : REQUIREMENT_FLUID_BLACKLIST.get()) {
            ResourceLocation fluidId = ResourceLocation.tryParse(id);
            if (fluidId != null) {
                fluidIds.add(fluidId);
            }
        }
        return fluidIds;
    }

    private static boolean isValidModIdConfigValue(Object value) {
        return value instanceof String modId && modId.matches("[a-z0-9_.-]+");
    }

    private static boolean isValidResourceLocationConfigValue(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    private static long saturatedMultiply(long value, long multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }
}
