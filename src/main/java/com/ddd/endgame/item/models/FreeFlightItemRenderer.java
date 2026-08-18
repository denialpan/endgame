package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class FreeFlightItemRenderer extends GeneratedStencilItemRenderer {
    public static final FreeFlightItemRenderer INSTANCE = new FreeFlightItemRenderer();

    private FreeFlightItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_free_flight.png"),
                FreeFlightModel::originalModel,
                "Unable to load free flight texture masks"
        );
    }
}
