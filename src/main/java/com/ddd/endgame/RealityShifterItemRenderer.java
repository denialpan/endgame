package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;

public class RealityShifterItemRenderer extends GeneratedStencilItemRenderer {
    public static final RealityShifterItemRenderer INSTANCE = new RealityShifterItemRenderer();

    private RealityShifterItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_reality_shifter.png"),
                RealityShifterModel::originalModel,
                "Unable to load reality shifter texture masks"
        );
    }
}
