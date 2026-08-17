package com.ddd.endgame.item.models;

import com.ddd.endgame.Xevitia;
import net.minecraft.resources.ResourceLocation;

public class ChunkAnnihilatorItemRenderer extends GeneratedStencilItemRenderer {
    public static final ChunkAnnihilatorItemRenderer INSTANCE = new ChunkAnnihilatorItemRenderer();

    private ChunkAnnihilatorItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "textures/item/galaxy_chunk_annihilator.png"),
                ChunkAnnihilatorModel::originalModel,
                "Unable to load chunk annihilator texture masks"
        );
    }
}
