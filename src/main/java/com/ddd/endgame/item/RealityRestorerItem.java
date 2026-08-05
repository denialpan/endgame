package com.ddd.endgame.item;

import com.ddd.endgame.dddsendgame;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class RealityRestorerItem extends Item {
    private static final int COOLDOWN_TICKS = 100;
    private static final int TEMPORARY_CONTEXT_RADIUS = 1;

    public RealityRestorerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.dddsendgame.reality_restorer.tooltip").withStyle(ChatFormatting.GRAY));
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
            player.displayClientMessage(Component.translatable("message.dddsendgame.reality_restorer.warning").withStyle(ChatFormatting.RED), true);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResultHolder.consume(stack);
        }

        try {
            int changedBlocks = restoreChunk(serverLevel, chunkPos);
            player.displayClientMessage(Component.translatable("message.dddsendgame.reality_restorer.restored", chunkPos.x, chunkPos.z, changedBlocks), true);
        } catch (RuntimeException exception) {
            dddsendgame.LOGGER.warn("Failed to restore chunk {} with Reality Restorer", chunkPos, exception);
            player.displayClientMessage(Component.translatable("message.dddsendgame.reality_restorer.failed").withStyle(ChatFormatting.RED), true);
        }

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

    private static int restoreChunk(ServerLevel level, ChunkPos chunkPos) {
        ProtoChunk generatedChunk = generateRestoredChunk(level, chunkPos);

        LevelChunk liveChunk = level.getChunk(chunkPos.x, chunkPos.z);
        liveChunk.clearAllBlockEntities();
        removeChunkEntities(level, chunkPos);

        int changedBlocks = copyChunkSections(generatedChunk, liveChunk);

        liveChunk.setUnsaved(true);
        sendFullChunkRefresh(level, liveChunk);
        return changedBlocks;
    }

    private static ProtoChunk generateRestoredChunk(ServerLevel level, ChunkPos chunkPos) {
        Map<Long, ProtoChunk> generatedChunks = createTemporaryChunks(level, chunkPos);
        generateBaseWorldgen(level, generatedChunks, chunkPos);
        ProtoChunk generatedChunk = generatedChunks.get(chunkPos.toLong());
        generateFeaturesWithoutStructures(level, generatedChunk, generatedChunks);
        return generatedChunk;
    }

    private static Map<Long, ProtoChunk> createTemporaryChunks(ServerLevel level, ChunkPos center) {
        Map<Long, ProtoChunk> chunks = new HashMap<>();
        for (int chunkX = center.x - TEMPORARY_CONTEXT_RADIUS; chunkX <= center.x + TEMPORARY_CONTEXT_RADIUS; chunkX++) {
            for (int chunkZ = center.z - TEMPORARY_CONTEXT_RADIUS; chunkZ <= center.z + TEMPORARY_CONTEXT_RADIUS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                chunks.put(chunkPos.toLong(), new ProtoChunk(
                        chunkPos,
                        UpgradeData.EMPTY,
                        level,
                        level.registryAccess().registryOrThrow(Registries.BIOME),
                        null
                ));
            }
        }
        return chunks;
    }

    private static void generateBaseWorldgen(ServerLevel level, Map<Long, ProtoChunk> generatedChunks, ChunkPos center) {
        for (ProtoChunk chunk : generatedChunks.values()) {
            WorldGenRegion biomeRegion = createWorldGenRegion(level, chunk, ChunkStatus.BIOMES, generatedChunks);
            level.getChunkSource().getGenerator()
                    .createBiomes(
                            level.getChunkSource().randomState(),
                            Blender.of(biomeRegion),
                            noStructures(biomeRegion),
                            chunk
                    )
                    .join();
            chunk.setPersistedStatus(ChunkStatus.BIOMES);
        }

        for (ProtoChunk chunk : generatedChunks.values()) {
            WorldGenRegion noiseRegion = createWorldGenRegion(level, chunk, ChunkStatus.NOISE, generatedChunks);
            level.getChunkSource().getGenerator()
                    .fillFromNoise(
                            Blender.of(noiseRegion),
                            level.getChunkSource().randomState(),
                            noStructures(noiseRegion),
                            chunk
                    )
                    .join();
            chunk.setPersistedStatus(ChunkStatus.NOISE);
        }

        for (ProtoChunk chunk : generatedChunks.values()) {
            WorldGenRegion surfaceRegion = createWorldGenRegion(level, chunk, ChunkStatus.SURFACE, generatedChunks);
            level.getChunkSource().getGenerator().buildSurface(
                    surfaceRegion,
                    noStructures(surfaceRegion),
                    level.getChunkSource().randomState(),
                    chunk
            );
            chunk.setPersistedStatus(ChunkStatus.SURFACE);
        }

        for (ProtoChunk chunk : generatedChunks.values()) {
            WorldGenRegion carverRegion = createWorldGenRegion(level, chunk, ChunkStatus.CARVERS, generatedChunks);
            level.getChunkSource().getGenerator().applyCarvers(
                    carverRegion,
                    level.getSeed(),
                    level.getChunkSource().randomState(),
                    level.getBiomeManager(),
                    noStructures(carverRegion),
                    chunk,
                    GenerationStep.Carving.AIR
            );
            chunk.setPersistedStatus(ChunkStatus.CARVERS);
        }
    }

    private static void generateFeaturesWithoutStructures(ServerLevel level, ProtoChunk generatedChunk, Map<Long, ProtoChunk> generatedChunks) {
        Heightmap.primeHeightmaps(
                generatedChunk,
                EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE)
        );
        WorldGenRegion featureRegion = createWorldGenRegion(level, generatedChunk, ChunkStatus.SURFACE, generatedChunks);
        level.getChunkSource().getGenerator().applyBiomeDecoration(
                featureRegion,
                generatedChunk,
                noStructures(featureRegion)
        );
        Blender.generateBorderTicks(featureRegion, generatedChunk);
        generatedChunk.setPersistedStatus(ChunkStatus.FEATURES);
    }

    private static StructureManager noStructures(WorldGenRegion region) {
        return new NoStructuresManager(region);
    }

    private static WorldGenRegion createWorldGenRegion(ServerLevel level, ChunkAccess generatedChunk, ChunkStatus targetStatus, Map<Long, ProtoChunk> generatedChunks) {
        ChunkStep step = ChunkPyramid.GENERATION_PYRAMID.getStepTo(targetStatus);
        ChunkPos center = generatedChunk.getPos();
        int radius = step.directDependencies().getRadius();
        StaticCache2D<GenerationChunkHolder> cache = StaticCache2D.create(center.x, center.z, radius, (chunkX, chunkZ) -> new GenerationChunkHolder(new ChunkPos(chunkX, chunkZ)) {
            private final ChunkAccess emptyContextChunk = createEmptyContextChunk(level, chunkX, chunkZ);

            @Override
            public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
                ProtoChunk temporaryChunk = generatedChunks.get(ChunkPos.asLong(chunkX, chunkZ));
                if (temporaryChunk != null && temporaryChunk.getPersistedStatus().isOrAfter(status)) {
                    return temporaryChunk;
                }
                return emptyContextChunk;
            }

            @Override
            public ChunkStatus getPersistedStatus() {
                ProtoChunk temporaryChunk = generatedChunks.get(ChunkPos.asLong(chunkX, chunkZ));
                return temporaryChunk != null ? temporaryChunk.getPersistedStatus() : ChunkStatus.FULL;
            }

            @Override
            public int getTicketLevel() {
                return 0;
            }

            @Override
            public int getQueueLevel() {
                return 0;
            }
        });
        return new WorldGenRegion(level, cache, step, generatedChunk);
    }

    private static ChunkAccess createEmptyContextChunk(ServerLevel level, int chunkX, int chunkZ) {
        Holder<Biome> biome = level.getUncachedNoiseBiome((chunkX << 2) + 2, 0, (chunkZ << 2) + 2);
        return new EmptyLevelChunk(level, new ChunkPos(chunkX, chunkZ), biome);
    }

    private static int copyChunkSections(ProtoChunk generatedChunk, LevelChunk liveChunk) {
        int changedBlocks = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = liveChunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        LevelChunkSection[] generatedSections = generatedChunk.getSections();
        LevelChunkSection[] liveSections = liveChunk.getSections();

        for (int sectionIndex = 0; sectionIndex < liveSections.length; sectionIndex++) {
            LevelChunkSection generatedSection = generatedSections[sectionIndex];
            LevelChunkSection liveSection = liveSections[sectionIndex];
            int sectionY = liveChunk.getSectionYFromSectionIndex(sectionIndex);
            int minY = sectionY << 4;

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState generatedState = generatedSection.getBlockState(x, y, z);
                        if (liveSection.getBlockState(x, y, z) != generatedState) {
                            liveSection.setBlockState(x, y, z, generatedState, false);
                            mutablePos.set(minX + x, minY + y, minZ + z);
                            if (generatedState.hasBlockEntity()) {
                                liveChunk.setBlockState(mutablePos, generatedState, false);
                            } else {
                                liveChunk.removeBlockEntity(mutablePos);
                            }
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

    private static final class NoStructuresManager extends StructureManager {
        private NoStructuresManager(LevelAccessor level) {
            super(level, level.getServer().getWorldData().worldGenOptions().withStructures(false), null);
        }

        @Override
        public List<StructureStart> startsForStructure(ChunkPos chunkPos, Predicate<Structure> structurePredicate) {
            return Collections.emptyList();
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
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
}
