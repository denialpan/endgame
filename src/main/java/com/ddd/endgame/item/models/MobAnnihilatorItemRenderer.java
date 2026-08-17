package com.ddd.endgame.item.models;

import com.ddd.endgame.Xevitia;
import net.minecraft.resources.ResourceLocation;

public class MobAnnihilatorItemRenderer extends GeneratedStencilItemRenderer {
    public static final MobAnnihilatorItemRenderer INSTANCE = new MobAnnihilatorItemRenderer();

    private MobAnnihilatorItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "textures/item/galaxy_mob_annihilator.png"),
                MobAnnihilatorModel::originalModel,
                "Unable to load mob annihilator texture masks"
        );
    }
}
