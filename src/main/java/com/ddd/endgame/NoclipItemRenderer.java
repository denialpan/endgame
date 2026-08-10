package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;

public class NoclipItemRenderer extends GeneratedStencilItemRenderer {
    public static final NoclipItemRenderer INSTANCE = new NoclipItemRenderer();

    private NoclipItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_noclip.png"),
                NoclipModel::originalModel,
                "Unable to load noclip texture masks"
        );
    }
}
