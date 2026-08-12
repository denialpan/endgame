package com.ddd.endgame.item;

import com.ddd.endgame.ChunkAnnihilatorItemRenderer;
import com.ddd.endgame.dddsendgame;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class ChunkAnnihilatorItem extends Item {
    private static final int COOLDOWN_TICKS = 30;

    public ChunkAnnihilatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.chunk_annihilator.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
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

        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        boolean containsEndgameBlocks = containsEndgameBlocks(serverLevel, chunkPos);
        if (containsEndgameBlocks && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.dddsendgame.chunk_annihilator.warning").withStyle(ChatFormatting.RED), true);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResultHolder.consume(stack);
        }

        int changedBlocks = annihilateChunk(serverLevel, chunkPos);
        player.displayClientMessage(Component.translatable("message.dddsendgame.chunk_annihilator.destroyed", chunkPos.x, chunkPos.z, changedBlocks), true);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    private static boolean containsEndgameBlocks(ServerLevel level, ChunkPos chunkPos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    mutablePos.set(minX + x, y, minZ + z);
                    ResourceLocation key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(mutablePos).getBlock());
                    if (dddsendgame.MODID.equals(key.getNamespace())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static int annihilateChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.clearAllBlockEntities();
        removeChunkEntities(level, chunkPos);

        int changedBlocks = clearChunkSections(chunk);
        chunk.setUnsaved(true);
        sendFullChunkRefresh(level, chunk);
        return changedBlocks;
    }

    private static int clearChunkSections(LevelChunk chunk) {
        int changedBlocks = 0;
        BlockState air = Blocks.AIR.defaultBlockState();

        for (LevelChunkSection section : chunk.getSections()) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z) != air) {
                            section.setBlockState(x, y, z, air, false);
                            changedBlocks++;
                        }
                    }
                }
            }
        }

        return changedBlocks;
    }

    private static void sendFullChunkRefresh(ServerLevel level, LevelChunk chunk) {
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk,
                level.getChunkSource().getLightEngine(),
                null,
                null
        );
        for (ServerPlayer player : level.players()) {
            player.connection.send(packet);
        }
    }

    private static void removeChunkEntities(ServerLevel level, ChunkPos chunkPos) {
        AABB bounds = new AABB(
                chunkPos.getMinBlockX(),
                level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1,
                level.getMaxBuildHeight(),
                chunkPos.getMaxBlockZ() + 1
        );
        List<Entity> entities = level.getEntities((Entity) null, bounds, entity -> !(entity instanceof ServerPlayer));
        for (Entity entity : entities) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ChunkAnnihilatorItemRenderer.INSTANCE;
            }
        });
    }
}
