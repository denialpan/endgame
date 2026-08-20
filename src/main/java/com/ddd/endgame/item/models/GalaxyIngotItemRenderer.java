package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class GalaxyIngotItemRenderer extends GeneratedStencilItemRenderer {
    public static final GalaxyIngotItemRenderer INSTANCE = new GalaxyIngotItemRenderer();
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_ingot.png");

    private GalaxyIngotItemRenderer() {
        super(
                stack -> DEFAULT_TEXTURE,
                stack -> GeneratedStencilItemModel.originalModel("galaxy_ingot"),
                "Unable to load galaxy ingot texture masks"
        );
    }
}
