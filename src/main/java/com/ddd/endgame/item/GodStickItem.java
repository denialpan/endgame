package com.ddd.endgame.item;

import com.ddd.endgame.item.models.GodStickItemRenderer;
import com.ddd.endgame.dddsendgame;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class GodStickItem extends Item {
    private static final String MODE_TAG = "EndgameStickMode";

    public GodStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.god_stick.mode", mode(stack).displayName()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        return switch (mode(stack)) {
            case DAY_CONTROLLER -> dddsendgame.DAY_NIGHT_TOGGLE.get().use(level, player, usedHand);
            case WEATHER_CONTROLLER -> dddsendgame.WEATHER_CYCLER.get().use(level, player, usedHand);
            case MOB_ANNIHILATOR -> dddsendgame.ENTITY_PURGE_CORE.get().use(level, player, usedHand);
            case REALITY_SHIFTER -> dddsendgame.REALITY_RESTORER.get().use(level, player, usedHand);
            case NOCLIP -> dddsendgame.SPECTATOR_PHASE_CORE.get().use(level, player, usedHand);
            case CHUNK_ANNIHILATOR -> dddsendgame.CHUNK_ANNIHILATOR.get().use(level, player, usedHand);
            case BLOCK_FABRICATOR, DEBUG_STICK -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (mode(context.getItemInHand()) == Mode.BLOCK_FABRICATOR) {
            return dddsendgame.RANDOM_BLOCK_PLACER.get().useOn(context);
        }
        if (mode(context.getItemInHand()) == Mode.DEBUG_STICK) {
            return Items.DEBUG_STICK.useOn(context);
        }
        return super.useOn(context);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(this) && mode(stack) == Mode.DEBUG_STICK) {
            return Items.DEBUG_STICK.canAttackBlock(state, level, pos, player);
        }
        return super.canAttackBlock(state, level, pos, player);
    }

    public static Mode mode(ItemStack stack) {
        int index = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_TAG);
        return Mode.byIndex(index);
    }

    public static Mode cycleMode(ItemStack stack, int direction) {
        Mode next = mode(stack).cycle(direction);
        setMode(stack, next);
        return next;
    }

    private static void setMode(ItemStack stack, Mode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_TAG, mode.ordinal()));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return GodStickItemRenderer.INSTANCE;
            }
        });
    }

    public enum Mode {
        DAY_CONTROLLER("item.dddsendgame.day_night_toggle"),
        WEATHER_CONTROLLER("item.dddsendgame.weather_cycler"),
        MOB_ANNIHILATOR("item.dddsendgame.entity_purge_core"),
        BLOCK_FABRICATOR("item.dddsendgame.random_block_placer"),
        REALITY_SHIFTER("item.dddsendgame.reality_restorer"),
        NOCLIP("item.dddsendgame.spectator_phase_core"),
        CHUNK_ANNIHILATOR("item.dddsendgame.chunk_annihilator"),
        DEBUG_STICK("item.minecraft.debug_stick");

        private static final Mode[] VALUES = values();
        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component displayName() {
            return Component.translatable(this.translationKey);
        }

        private Mode cycle(int direction) {
            int step = direction >= 0 ? 1 : -1;
            return byIndex(Mth.positiveModulo(this.ordinal() + step, VALUES.length));
        }

        private static Mode byIndex(int index) {
            return VALUES[Mth.positiveModulo(index, VALUES.length)];
        }
    }
}
