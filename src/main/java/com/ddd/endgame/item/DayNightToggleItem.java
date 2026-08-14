package com.ddd.endgame.item;

import com.ddd.endgame.item.models.DayControllerItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerLevel;
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

public class DayNightToggleItem extends Item {
    private static final long DAY_LENGTH = 24000L;
    private static final long NIGHT_START = 13000L;
    private static final int COOLDOWN_TICKS = 30;

    public DayNightToggleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.day_night_toggle.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
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

        long dayTime = serverLevel.getDayTime();
        long cycleTime = Math.floorMod(dayTime, DAY_LENGTH);
        long delta = cycleTime < NIGHT_START ? NIGHT_START - cycleTime : DAY_LENGTH - cycleTime;
        long targetTime = dayTime + delta;
        serverLevel.setDayTime(targetTime);

        if (Math.floorMod(targetTime, DAY_LENGTH) == NIGHT_START) {
            player.displayClientMessage(Component.translatable("message.dddsendgame.time.night"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.dddsendgame.time.day"), true);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return DayControllerItemRenderer.INSTANCE;
            }
        });
    }
}
