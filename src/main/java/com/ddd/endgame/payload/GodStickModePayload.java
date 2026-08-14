package com.ddd.endgame.payload;

import com.ddd.endgame.dddsendgame;
import com.ddd.endgame.item.GodStickItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GodStickModePayload(int direction) implements CustomPacketPayload {
    public static final Type<GodStickModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "god_stick_mode")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GodStickModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new GodStickModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GodStickModePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedStick(player);
        if (stack.isEmpty()) {
            return;
        }

        GodStickItem.Mode mode = GodStickItem.cycleMode(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.dddsendgame.god_stick.mode", mode.displayName()), true);
    }

    private static ItemStack selectedStick(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(dddsendgame.GOD_STICK.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(dddsendgame.GOD_STICK.get()) ? offHand : ItemStack.EMPTY;
    }
}
