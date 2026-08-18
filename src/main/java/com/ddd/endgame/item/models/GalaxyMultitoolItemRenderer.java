package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import com.ddd.endgame.item.GalaxyMultitoolItem;
import com.ddd.endgame.item.GalaxyToolItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GalaxyMultitoolItemRenderer extends GeneratedStencilItemRenderer {
    public static final GalaxyMultitoolItemRenderer INSTANCE = new GalaxyMultitoolItemRenderer();
    private static final List<ModelResourceLocation> TOOL_MODELS = List.of(
            model("galaxy_multitool_pickaxe"),
            model("galaxy_multitool_axe"),
            model("galaxy_multitool_hoe"),
            model("galaxy_multitool_shovel"),
            model("galaxy_multitool_sword")
    );
    private static final List<ResourceLocation> TOOL_TEXTURES = List.of(
            texture("galaxy_multitool_pickaxe"),
            texture("galaxy_multitool_axe"),
            texture("galaxy_multitool_hoe"),
            texture("galaxy_multitool_shovel"),
            texture("galaxy_multitool_sword")
    );

    private GalaxyMultitoolItemRenderer() {
        super(
                stack -> TOOL_TEXTURES.get(toolIndex(stack)),
                GalaxyMultitoolItemRenderer::selectedModel,
                "Unable to load galaxy multitool texture masks"
        );
    }

    public static List<ModelResourceLocation> modelLocations() {
        return TOOL_MODELS;
    }

    private static BakedModel selectedModel(ItemStack stack) {
        return Minecraft.getInstance().getModelManager().getModel(TOOL_MODELS.get(toolIndex(stack)));
    }

    private static int toolIndex(ItemStack stack) {
        if (stack.getItem() instanceof GalaxyToolItem toolItem) {
            return toolItem.toolIndex();
        }
        return GalaxyMultitoolItem.selectedToolIndex(stack);
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "item/" + path));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/" + path + ".png");
    }
}
