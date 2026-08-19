package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class MobAnnihilatorItemRenderer extends GeneratedStencilItemRenderer {
    public static final MobAnnihilatorItemRenderer INSTANCE = new MobAnnihilatorItemRenderer();

    private MobAnnihilatorItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_mob_annihilator.png"),
                () -> GeneratedStencilItemModel.originalModel("entity_purge_core"),
                "Unable to load mob annihilator texture masks"
        );
    }
}
