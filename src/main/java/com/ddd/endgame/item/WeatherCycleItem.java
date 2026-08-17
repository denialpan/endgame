package com.ddd.endgame.item;

import com.ddd.endgame.item.models.WeatherControllerItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class WeatherCycleItem extends Item {
    private static final int WEATHER_DURATION = 6000;
    private static final int COOLDOWN_TICKS = 30;

    public WeatherCycleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.xevitia.weather_cycler.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        boolean thundering = serverLevel.getLevelData().isThundering();
        boolean raining = serverLevel.getLevelData().isRaining();
        if (thundering) {
            serverLevel.setWeatherParameters(WEATHER_DURATION, 0, false, false);
            player.displayClientMessage(Component.translatable("message.xevitia.weather.clear"), true);
        } else if (raining) {
            serverLevel.setWeatherParameters(0, WEATHER_DURATION, true, true);
            player.displayClientMessage(Component.translatable("message.xevitia.weather.thunder"), true);
        } else {
            serverLevel.setWeatherParameters(0, WEATHER_DURATION, true, false);
            player.displayClientMessage(Component.translatable("message.xevitia.weather.rain"), true);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return WeatherControllerItemRenderer.INSTANCE;
            }
        });
    }
}
