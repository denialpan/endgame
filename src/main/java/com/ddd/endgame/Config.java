package com.ddd.endgame;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.LongValue ITEM_REQUIREMENT = BUILDER
            .comment("How many of each required item stack must be inserted into the endgame template.")
            .defineInRange("itemRequirement", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_REQUIREMENT_MB = BUILDER
            .comment("How many millibuckets of each required fluid must be inserted into the endgame template.")
            .defineInRange("fluidRequirementMb", 1_048_576L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue DEBUG_STONE_ONLY = BUILDER
            .comment("Debug mode: require only 20 minecraft:stone and ignore generated item/fluid recipe requirements.")
            .define("debugStoneOnly", false);

    public static final ModConfigSpec.BooleanValue ENDGAME_STICK_GRANTS_CREATIVE = BUILDER
            .comment("Whether holding the endgame stick in inventory sets the player to Creative mode.")
            .define("endgameStickGrantsCreative", true);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static long itemRequirement() {
        return DEBUG_STONE_ONLY.getAsBoolean() ? 20L : ITEM_REQUIREMENT.get();
    }

    public static long fluidRequirementMb() {
        return FLUID_REQUIREMENT_MB.get();
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
