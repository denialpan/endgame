package com.ddd.endgame.client;

import com.ddd.endgame.block.GalaxyCompressorBlockEntity;
import com.ddd.endgame.payload.GalaxyCompressorRequirementDeltaPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class GalaxyCompressorDeltaClientHandler {
    private GalaxyCompressorDeltaClientHandler() {
    }

    public static void handle(GalaxyCompressorRequirementDeltaPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(payload.pos());
        if (blockEntity instanceof GalaxyCompressorBlockEntity compressor) {
            compressor.applyRequirementDelta(payload.id(), payload.fluid(), payload.remaining());
        }
    }
}
