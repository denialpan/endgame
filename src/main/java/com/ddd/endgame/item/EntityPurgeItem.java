package com.ddd.endgame.item;

import com.ddd.endgame.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EntityPurgeItem extends Item {
    private static final int COOLDOWN_TICKS = 40;
    private static final int GUARANTEED_DROP_STACKS = 8;
    private static final int GUARANTEED_DROP_COUNT = 64;

    public EntityPurgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.entity_purge_core.tooltip").withStyle(ChatFormatting.GRAY));
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
        List<Entity> targets = serverLevel.getEntities(player, bounds, entity -> entity.isAlive() && (killsPlayers || !(entity instanceof Player)));
        for (Entity target : targets) {
            spawnGuaranteedDrops(serverLevel, player, target);
            target.remove(Entity.RemovalReason.KILLED);
        }

        player.displayClientMessage(Component.translatable("message.dddsendgame.entity_purge", targets.size(), radius), true);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private static void spawnGuaranteedDrops(ServerLevel level, Player player, Entity target) {
        ResourceKey<LootTable> lootTableKey = target.getType().getDefaultLootTable();
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
        DamageSource damageSource = level.damageSources().playerAttack(player);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, target)
                .withParameter(LootContextParams.ORIGIN, target.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.ENTITY);
        long lootSeed = target instanceof LivingEntity livingEntity ? livingEntity.getLootTableSeed() : 0L;

        lootTable.getRandomItems(params, lootSeed, drop -> spawnAmplifiedDrop(target, drop));
    }

    private static void spawnAmplifiedDrop(Entity target, ItemStack drop) {
        if (drop.isEmpty()) {
            return;
        }

        for (int i = 0; i < GUARANTEED_DROP_STACKS; i++) {
            ItemStack fullStack = drop.copy();
            fullStack.setCount(GUARANTEED_DROP_COUNT);
            target.spawnAtLocation(fullStack);
        }
    }
}
