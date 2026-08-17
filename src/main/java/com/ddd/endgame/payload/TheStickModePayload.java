package com.ddd.endgame.payload;

import com.ddd.endgame.Xevitia;
import com.ddd.endgame.item.TheStickItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TheStickModePayload(int direction) implements CustomPacketPayload {
    public static final Type<TheStickModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "the_stick_mode")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TheStickModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new TheStickModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TheStickModePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedStick(player);
        if (stack.isEmpty()) {
            return;
        }

        TheStickItem.Mode mode = TheStickItem.cycleMode(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.xevitia.the_stick.mode", mode.displayName()), true);
    }

    private static ItemStack selectedStick(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Xevitia.THE_STICK.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(Xevitia.THE_STICK.get()) ? offHand : ItemStack.EMPTY;
    }
}
