package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class ChunkDestroyerItemRenderer extends GeneratedStencilItemRenderer {
    public static final ChunkDestroyerItemRenderer INSTANCE = new ChunkDestroyerItemRenderer();

    private ChunkDestroyerItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_chunk_destroyer.png"),
                () -> GeneratedStencilItemModel.originalModel("chunk_destroyer"),
                "Unable to load chunk destroyer texture masks"
        );
    }
}
