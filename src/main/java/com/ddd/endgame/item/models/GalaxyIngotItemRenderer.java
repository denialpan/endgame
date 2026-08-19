package com.ddd.endgame.item.models;

import com.ddd.endgame.galaxy.GalaxyInstabilityVisuals;
import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class GalaxyIngotItemRenderer extends GeneratedStencilItemRenderer {
    public static final GalaxyIngotItemRenderer INSTANCE = new GalaxyIngotItemRenderer();

    private GalaxyIngotItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_ingot.png"),
                () -> GeneratedStencilItemModel.originalModel("galaxy_ingot"),
                "Unable to load galaxy ingot texture masks",
                GalaxyInstabilityVisuals::tintGreenBlue
        );
    }
}
