package com.ddd.endgame.payload;

import com.ddd.endgame.Xavitia;
import com.ddd.endgame.client.GalaxyCompressorDeltaClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GalaxyCompressorRequirementDeltaPayload(BlockPos pos, ResourceLocation id, boolean fluid, long remaining) implements CustomPacketPayload {
    public static final Type<GalaxyCompressorRequirementDeltaPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "galaxy_compressor_requirement_delta")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GalaxyCompressorRequirementDeltaPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.pos());
                buffer.writeResourceLocation(payload.id());
                buffer.writeBoolean(payload.fluid());
                buffer.writeVarLong(payload.remaining());
            },
            buffer -> new GalaxyCompressorRequirementDeltaPayload(
                    buffer.readBlockPos(),
                    buffer.readResourceLocation(),
                    buffer.readBoolean(),
                    buffer.readVarLong()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GalaxyCompressorRequirementDeltaPayload payload, IPayloadContext context) {
        GalaxyCompressorDeltaClientHandler.handle(payload);
    }
}
