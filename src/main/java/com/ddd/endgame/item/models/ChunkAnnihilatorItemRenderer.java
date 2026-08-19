package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class ChunkAnnihilatorItemRenderer extends GeneratedStencilItemRenderer {
    public static final ChunkAnnihilatorItemRenderer INSTANCE = new ChunkAnnihilatorItemRenderer();

    private ChunkAnnihilatorItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_chunk_annihilator.png"),
                () -> GeneratedStencilItemModel.originalModel("chunk_annihilator"),
                "Unable to load chunk annihilator texture masks"
        );
    }
}
