package com.ddd.endgame.item.models;

import com.ddd.endgame.dddsendgame;
import net.minecraft.resources.ResourceLocation;

public class EndgameTestStickItemRenderer extends GeneratedStencilItemRenderer {
    public static final EndgameTestStickItemRenderer INSTANCE = new EndgameTestStickItemRenderer();

    private EndgameTestStickItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_creative_stick.png"),
                EndgameTestStickModel::originalModel,
                "Unable to load endgame test stick texture masks"
        );
    }
}
