package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;

public class FreeFlightItemRenderer extends GeneratedStencilItemRenderer {
    public static final FreeFlightItemRenderer INSTANCE = new FreeFlightItemRenderer();

    private FreeFlightItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_free_flight.png"),
                FreeFlightModel::originalModel,
                "Unable to load free flight texture masks"
        );
    }
}
