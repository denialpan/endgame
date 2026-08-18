package com.ddd.endgame.item;

import com.ddd.endgame.Config;
import com.ddd.endgame.item.models.MobAnnihilatorItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class EntityPurgeItem extends Item {
    private static final int COOLDOWN_TICKS = 30;

    public EntityPurgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.xavitia.entity_purge_core.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
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

        int radius = Config.ENTITY_PURGE_RADIUS.get();
        boolean killsPlayers = Config.ENTITY_PURGE_KILLS_PLAYERS.getAsBoolean();
        AABB bounds = player.getBoundingBox().inflate(radius);
        List<Entity> targets = serverLevel.getEntities(player, bounds, entity -> isPurgeTarget(entity, killsPlayers));
        for (Entity target : targets) {
            purgeTarget(serverLevel, player, target);
        }

        player.displayClientMessage(Component.translatable("message.xavitia.entity_purge", targets.size(), radius), true);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private static boolean isPurgeTarget(Entity entity, boolean killsPlayers) {
        return entity.isAlive()
                && !(entity instanceof ItemEntity)
                && (killsPlayers || !(entity instanceof Player));
    }

    private static void purgeTarget(ServerLevel level, Player player, Entity target) {
        if (target instanceof LivingEntity livingEntity) {
            DamageSource damageSource = level.damageSources().playerAttack(player);
            livingEntity.setLastHurtByPlayer(player);
            livingEntity.hurt(damageSource, Float.MAX_VALUE);
            return;
        }

        target.remove(Entity.RemovalReason.KILLED);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MobAnnihilatorItemRenderer.INSTANCE;
            }
        });
    }
}
