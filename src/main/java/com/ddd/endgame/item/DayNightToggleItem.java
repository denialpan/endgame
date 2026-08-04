package com.ddd.endgame.item;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DayNightToggleItem extends Item {
    private static final long DAY_LENGTH = 24000L;
    private static final long TOGGLE_INTERVAL = 12000L;
    private static final int COOLDOWN_TICKS = 20;

    public DayNightToggleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.day_night_toggle.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        long dayTime = serverLevel.getDayTime();
        long remainder = Math.floorMod(dayTime, TOGGLE_INTERVAL);
        long delta = remainder == 0L ? TOGGLE_INTERVAL : TOGGLE_INTERVAL - remainder;
        long targetTime = dayTime + delta;
        serverLevel.setDayTime(targetTime);

        if (Math.floorMod(targetTime, DAY_LENGTH) == TOGGLE_INTERVAL) {
            player.displayClientMessage(Component.translatable("message.dddsendgame.time.night"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.dddsendgame.time.day"), true);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }
}
