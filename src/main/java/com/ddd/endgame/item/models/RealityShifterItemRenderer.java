package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class RealityShifterItemRenderer extends GeneratedStencilItemRenderer {
    public static final RealityShifterItemRenderer INSTANCE = new RealityShifterItemRenderer();

    private RealityShifterItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_reality_shifter.png"),
                () -> GeneratedStencilItemModel.originalModel("reality_restorer"),
                "Unable to load reality shifter texture masks"
        );
    }
}
