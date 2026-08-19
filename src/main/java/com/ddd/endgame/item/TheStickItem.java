package com.ddd.endgame.item;

import com.ddd.endgame.Config;
import com.ddd.endgame.item.models.TheStickItemRenderer;
import com.ddd.endgame.Xavitia;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class TheStickItem extends Item {
    public TheStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.xavitia.the_stick.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static boolean grantsServerCommandPermissions(Entity entity) {
        if (!Config.THE_STICK_GRANTS_SERVER_COMMANDS.getAsBoolean()) {
            return false;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        return player.getMainHandItem().is(Xavitia.THE_STICK.get()) || player.getOffhandItem().is(Xavitia.THE_STICK.get());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return TheStickItemRenderer.INSTANCE;
            }
        });
    }
}
