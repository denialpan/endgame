package com.ddd.endgame.galaxy;

import com.ddd.endgame.Config;
import com.ddd.endgame.Xavitia;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class GalaxyCompressorRequirements {
    private static final Map<MinecraftServer, Snapshot> CACHE = new WeakHashMap<>();

    private GalaxyCompressorRequirements() {
    }

    public static void warm(MinecraftServer server) {
        get(server);
    }

    public static void invalidate(MinecraftServer server) {
        synchronized (CACHE) {
            CACHE.remove(server);
        }
    }

    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public static Snapshot get(ServerLevel level) {
        return get(level.getServer());
    }

    public static Snapshot get(MinecraftServer server) {
        boolean debugStoneOnly = Config.DEBUG_STONE_ONLY.getAsBoolean();
        synchronized (CACHE) {
            Snapshot snapshot = CACHE.get(server);
            if (snapshot != null && snapshot.debugStoneOnly() == debugStoneOnly) {
                return snapshot;
            }

            Snapshot built = build(server, debugStoneOnly);
            CACHE.put(server, built);
            return built;
        }
    }

    private static Snapshot build(MinecraftServer server, boolean debugStoneOnly) {
        Map<ResourceLocation, Item> recipeItems = new LinkedHashMap<>();
        Map<ResourceLocation, Fluid> recipeFluids = new LinkedHashMap<>();
        if (debugStoneOnly) {
            recipeItems.put(BuiltInRegistries.ITEM.getKey(Items.STONE), Items.STONE);
        } else {
            addRecipeFluid(recipeFluids, Fluids.WATER);
            addRecipeFluid(recipeFluids, Fluids.LAVA);
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                Object recipe = holder.value();
                addModernIndustrializationMachineOutputs(recipeItems, recipeFluids, recipe);
                if (holder.value().isSpecial()) {
                    continue;
                }

                addRecipeOutputItem(recipeItems, holder.value().getResultItem(server.registryAccess()));
            }
        }

        List<ResourceLocation> itemIds = new ArrayList<>(recipeItems.keySet());
        itemIds.sort(ResourceLocation::compareTo);
        List<ResourceLocation> fluidIds = new ArrayList<>(recipeFluids.keySet());
        fluidIds.sort(ResourceLocation::compareTo);
        Xavitia.LOGGER.info("Detected {} recipe output items and {} fluids for galaxy compressor requirements", itemIds.size(), fluidIds.size());
        return new Snapshot(List.copyOf(itemIds), List.copyOf(fluidIds), debugStoneOnly);
    }

    private static void addRecipeOutputItem(Map<ResourceLocation, Item> recipeItems, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (itemId != null && !Xavitia.MODID.equals(itemId.getNamespace())) {
            recipeItems.putIfAbsent(itemId, result.getItem());
        }
    }

    private static void addRecipeFluid(Map<ResourceLocation, Fluid> recipeFluids, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return;
        }

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null) {
            recipeFluids.putIfAbsent(fluidId, fluid);
        }
    }

    private static void addModernIndustrializationMachineOutputs(Map<ResourceLocation, Item> recipeItems, Map<ResourceLocation, Fluid> recipeFluids, Object recipe) {
        if (!"aztech.modern_industrialization.machines.recipe.MachineRecipe".equals(recipe.getClass().getName())) {
            return;
        }

        try {
            Field itemOutputsField = recipe.getClass().getField("itemOutputs");
            Object itemOutputs = itemOutputsField.get(recipe);
            if (itemOutputs instanceof Iterable<?> itemOutputEntries) {
                for (Object output : itemOutputEntries) {
                    Method getStack = output.getClass().getMethod("getStack");
                    Object stack = getStack.invoke(output);
                    if (stack instanceof ItemStack itemStack) {
                        addRecipeOutputItem(recipeItems, itemStack);
                    }
                }
            }

            Field fluidOutputsField = recipe.getClass().getField("fluidOutputs");
            Object fluidOutputs = fluidOutputsField.get(recipe);
            if (fluidOutputs instanceof Iterable<?> fluidOutputEntries) {
                for (Object output : fluidOutputEntries) {
                    Method fluid = output.getClass().getMethod("fluid");
                    Object fluidOutput = fluid.invoke(output);
                    if (fluidOutput instanceof Fluid outputFluid) {
                        addRecipeFluid(recipeFluids, outputFluid);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Xavitia.LOGGER.debug("Unable to inspect Modern Industrialization machine recipe {}", recipe, exception);
        }
    }

    public record Snapshot(List<ResourceLocation> itemIds, List<ResourceLocation> fluidIds, boolean debugStoneOnly) {
    }
}
