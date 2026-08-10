package com.ddd.endgame;

import com.ddd.endgame.item.EndgameTestStickItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EndgameTestStickModePayload(int direction) implements CustomPacketPayload {
    public static final Type<EndgameTestStickModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "endgame_test_stick_mode")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, EndgameTestStickModePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new EndgameTestStickModePayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EndgameTestStickModePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedStick(player);
        if (stack.isEmpty()) {
            return;
        }

        EndgameTestStickItem.Mode mode = EndgameTestStickItem.cycleMode(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.dddsendgame.endgame_test_stick.mode", mode.displayName()), true);
    }

    private static ItemStack selectedStick(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(dddsendgame.ENDGAME_TEST_STICK.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(dddsendgame.ENDGAME_TEST_STICK.get()) ? offHand : ItemStack.EMPTY;
    }
}
