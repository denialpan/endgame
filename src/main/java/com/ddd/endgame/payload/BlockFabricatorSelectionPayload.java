package com.ddd.endgame.payload;

import com.ddd.endgame.Xevitia;
import com.ddd.endgame.item.RandomBlockPlacerItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BlockFabricatorSelectionPayload(int direction) implements CustomPacketPayload {
    public static final Type<BlockFabricatorSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "block_fabricator_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockFabricatorSelectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new BlockFabricatorSelectionPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BlockFabricatorSelectionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedFabricator(player);
        if (stack.isEmpty()) {
            return;
        }

        Item item = RandomBlockPlacerItem.cycleSelectedItem(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.xavitia.random_block_placer.selected", item.getDescription()), true);
    }

    private static ItemStack selectedFabricator(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Xevitia.RANDOM_BLOCK_PLACER.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(Xevitia.RANDOM_BLOCK_PLACER.get()) ? offHand : ItemStack.EMPTY;
    }
}
