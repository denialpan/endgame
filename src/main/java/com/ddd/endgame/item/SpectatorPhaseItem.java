package com.ddd.endgame.item;

import com.ddd.endgame.dddsendgame;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SpectatorPhaseItem extends Item {
    private static final int COOLDOWN_TICKS = 20;

    public SpectatorPhaseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.spectator_phase_core.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        dddsendgame.startSpectatorPhase(serverPlayer);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }
}
