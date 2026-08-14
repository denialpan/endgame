package com.ddd.endgame.item.models;

import com.ddd.endgame.dddsendgame;
import net.minecraft.resources.ResourceLocation;

public class GodStickItemRenderer extends GeneratedStencilItemRenderer {
    public static final GodStickItemRenderer INSTANCE = new GodStickItemRenderer();

    private GodStickItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_creative_stick.png"),
                GodStickModel::originalModel,
                "Unable to load endgame test stick texture masks"
        );
    }
}
