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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

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
        String blacklistSignature = Config.requirementBlacklistSignature();
        synchronized (CACHE) {
            Snapshot snapshot = CACHE.get(server);
            if (snapshot != null && snapshot.debugStoneOnly() == debugStoneOnly && snapshot.blacklistSignature().equals(blacklistSignature)) {
                return snapshot;
            }

            Snapshot built = build(server, debugStoneOnly, blacklistSignature);
            CACHE.put(server, built);
            return built;
        }
    }

    private static Snapshot build(MinecraftServer server, boolean debugStoneOnly, String blacklistSignature) {
        Map<ResourceLocation, Item> recipeItems = new LinkedHashMap<>();
        Map<ResourceLocation, Fluid> recipeFluids = new LinkedHashMap<>();
        if (debugStoneOnly) {
            recipeItems.put(BuiltInRegistries.ITEM.getKey(Items.STONE), Items.STONE);
        } else {
            addTaggedBlockItems(
                    recipeItems,
                    BlockTags.BASE_STONE_OVERWORLD,
                    BlockTags.BASE_STONE_NETHER,
                    BlockTags.DIRT,
                    BlockTags.LOGS,
                    BlockTags.LEAVES,
                    BlockTags.SAPLINGS,
                    BlockTags.FLOWERS,
                    BlockTags.CROPS
            );
            addRecipeFluid(recipeFluids, Fluids.WATER);
            addRecipeFluid(recipeFluids, Fluids.LAVA);
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                Object recipe = holder.value();
                addCreateRecipeOutputs(recipeItems, recipeFluids, recipe);
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
        return new Snapshot(List.copyOf(itemIds), List.copyOf(fluidIds), debugStoneOnly, blacklistSignature);
    }

    private static void addRecipeOutputItem(Map<ResourceLocation, Item> recipeItems, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (itemId != null && shouldIncludeItemRequirement(itemId)) {
            recipeItems.putIfAbsent(itemId, result.getItem());
        }
    }

    @SafeVarargs
    private static void addTaggedBlockItems(Map<ResourceLocation, Item> recipeItems, TagKey<Block>... tags) {
        for (Block block : BuiltInRegistries.BLOCK) {
            for (TagKey<Block> tag : tags) {
                if (block.defaultBlockState().is(tag)) {
                    Item item = block.asItem();
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                    if (itemId != null && item != Items.AIR && shouldIncludeItemRequirement(itemId)) {
                        recipeItems.putIfAbsent(itemId, item);
                    }
                    break;
                }
            }
        }
    }

    private static void addRecipeFluid(Map<ResourceLocation, Fluid> recipeFluids, Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return;
        }

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null && shouldIncludeFluidRequirement(fluidId)) {
            recipeFluids.putIfAbsent(fluidId, fluid);
        }
    }

    private static boolean shouldIncludeItemRequirement(ResourceLocation itemId) {
        return !Xavitia.MODID.equals(itemId.getNamespace())
                && !Config.isRequirementModBlacklisted(itemId.getNamespace())
                && !Config.isRequirementItemBlacklisted(itemId);
    }

    private static boolean shouldIncludeFluidRequirement(ResourceLocation fluidId) {
        return !Config.isRequirementModBlacklisted(fluidId.getNamespace())
                && !Config.isRequirementFluidBlacklisted(fluidId);
    }

    private static void addCreateRecipeOutputs(Map<ResourceLocation, Item> recipeItems, Map<ResourceLocation, Fluid> recipeFluids, Object recipe) {
        if (!recipe.getClass().getName().startsWith("com.simibubi.create.")) {
            return;
        }

        try {
            addCreateProcessingOutputs(recipeItems, recipeFluids, recipe);
            addCreateSequencedAssemblyOutputs(recipeItems, recipeFluids, recipe);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Xavitia.LOGGER.debug("Unable to inspect Create recipe {}", recipe, exception);
        }
    }

    private static void addCreateProcessingOutputs(Map<ResourceLocation, Item> recipeItems, Map<ResourceLocation, Fluid> recipeFluids, Object recipe) throws ReflectiveOperationException {
        Object itemResults = invokeNoArgIfPresent(recipe, "getRollableResultsAsItemStacks");
        if (itemResults instanceof Iterable<?> stacks) {
            for (Object stack : stacks) {
                if (stack instanceof ItemStack itemStack) {
                    addRecipeOutputItem(recipeItems, itemStack);
                }
            }
        }

        Object fluidResults = invokeNoArgIfPresent(recipe, "getFluidResults");
        if (fluidResults instanceof Iterable<?> stacks) {
            for (Object stack : stacks) {
                if (stack instanceof FluidStack fluidStack) {
                    addRecipeFluid(recipeFluids, fluidStack.getFluid());
                }
            }
        }
    }

    private static void addCreateSequencedAssemblyOutputs(Map<ResourceLocation, Item> recipeItems, Map<ResourceLocation, Fluid> recipeFluids, Object recipe) throws ReflectiveOperationException {
        if (!"com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe".equals(recipe.getClass().getName())) {
            return;
        }

        Field resultPoolField = recipe.getClass().getField("resultPool");
        Object resultPool = resultPoolField.get(recipe);
        addCreateProcessingOutputStacks(recipeItems, resultPool);

        Object transitionalItem = invokeNoArgIfPresent(recipe, "getTransitionalItem");
        if (transitionalItem instanceof ItemStack stack) {
            addRecipeOutputItem(recipeItems, stack);
        }

        Object sequence = invokeNoArgIfPresent(recipe, "getSequence");
        if (sequence instanceof Iterable<?> sequencedRecipes) {
            for (Object sequencedRecipe : sequencedRecipes) {
                Object nestedRecipe = invokeNoArgIfPresent(sequencedRecipe, "getRecipe");
                if (nestedRecipe != null) {
                    addCreateProcessingOutputs(recipeItems, recipeFluids, nestedRecipe);
                }
            }
        }
    }

    private static void addCreateProcessingOutputStacks(Map<ResourceLocation, Item> recipeItems, Object outputs) throws ReflectiveOperationException {
        if (!(outputs instanceof Iterable<?> outputEntries)) {
            return;
        }

        for (Object output : outputEntries) {
            Method getStack = output.getClass().getMethod("getStack");
            Object stack = getStack.invoke(output);
            if (stack instanceof ItemStack itemStack) {
                addRecipeOutputItem(recipeItems, itemStack);
            }
        }
    }

    private static Object invokeNoArgIfPresent(Object target, String methodName) throws ReflectiveOperationException {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
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

    public record Snapshot(List<ResourceLocation> itemIds, List<ResourceLocation> fluidIds, boolean debugStoneOnly, String blacklistSignature) {
    }
}
