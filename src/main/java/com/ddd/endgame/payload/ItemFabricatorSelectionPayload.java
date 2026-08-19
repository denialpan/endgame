package com.ddd.endgame.payload;

import com.ddd.endgame.Xavitia;
import com.ddd.endgame.item.ItemFabricatorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ItemFabricatorSelectionPayload(int direction) implements CustomPacketPayload {
    public static final Type<ItemFabricatorSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "item_fabricator_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFabricatorSelectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.direction()),
            buffer -> new ItemFabricatorSelectionPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemFabricatorSelectionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack stack = selectedFabricator(player);
        if (stack.isEmpty()) {
            return;
        }

        Item item = ItemFabricatorItem.cycleSelectedItem(stack, payload.direction());
        player.displayClientMessage(Component.translatable("message.xavitia.item_fabricator.selected", item.getDescription()), true);
    }

    private static ItemStack selectedFabricator(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Xavitia.ITEMFABRICATOR.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        return offHand.is(Xavitia.ITEMFABRICATOR.get()) ? offHand : ItemStack.EMPTY;
    }
}
