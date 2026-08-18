package com.ddd.endgame.payload;

import com.ddd.endgame.Xavitia;
import com.ddd.endgame.item.GalaxyMultitoolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GalaxyMultitoolSelectionPayload(int direction) implements CustomPacketPayload {
    public static final Type<GalaxyMultitoolSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "galaxy_multitool_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GalaxyMultitoolSelectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new GalaxyMultitoolSelectionPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GalaxyMultitoolSelectionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedMultitool(player);
        if (stack.isEmpty()) {
            return;
        }

        GalaxyMultitoolItem.cycleSelectedTool(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.xavitia.galaxy_multitool.selected", GalaxyMultitoolItem.selectedToolName(stack)), true);
    }

    private static ItemStack selectedMultitool(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Xavitia.GALAXY_MULTITOOL.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(Xavitia.GALAXY_MULTITOOL.get()) ? offHand : ItemStack.EMPTY;
    }
}
