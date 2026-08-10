package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;

public class DayControllerItemRenderer extends GeneratedStencilItemRenderer {
    public static final DayControllerItemRenderer INSTANCE = new DayControllerItemRenderer();

    private DayControllerItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_day_controller.png"),
                DayControllerModel::originalModel,
                "Unable to load day controller texture masks"
        );
    }
}
