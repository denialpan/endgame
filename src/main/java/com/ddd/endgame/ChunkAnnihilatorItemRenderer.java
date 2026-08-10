package com.ddd.endgame;

import net.minecraft.resources.ResourceLocation;

public class ChunkAnnihilatorItemRenderer extends GeneratedStencilItemRenderer {
    public static final ChunkAnnihilatorItemRenderer INSTANCE = new ChunkAnnihilatorItemRenderer();

    private ChunkAnnihilatorItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/item/galaxy_chunk_annihilator.png"),
                ChunkAnnihilatorModel::originalModel,
                "Unable to load chunk annihilator texture masks"
        );
    }
}
