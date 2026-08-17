package com.ddd.endgame.item.models;

import com.ddd.endgame.Xevitia;
import net.minecraft.resources.ResourceLocation;

public class NoclipItemRenderer extends GeneratedStencilItemRenderer {
    public static final NoclipItemRenderer INSTANCE = new NoclipItemRenderer();

    private NoclipItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "textures/item/galaxy_noclip.png"),
                NoclipModel::originalModel,
                "Unable to load noclip texture masks"
        );
    }
}
