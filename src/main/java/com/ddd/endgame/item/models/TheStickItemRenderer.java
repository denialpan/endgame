package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class TheStickItemRenderer extends GeneratedStencilItemRenderer {
    public static final TheStickItemRenderer INSTANCE = new TheStickItemRenderer();

    private TheStickItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/the_stick.png"),
                TheStickModel::originalModel,
                "Unable to load endgame test stick texture masks"
        );
    }
}
