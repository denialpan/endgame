package com.ddd.endgame.block;

import com.ddd.endgame.Xavitia;
import com.ddd.endgame.galaxy.GalaxyInstability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GalaxyDecorativeBlockEntity extends BlockEntity {
    private static final String GALAXY_INSTABILITY_TICKS_TAG = "GalaxyInstabilityTicks";
    private static final int GALAXY_TINT_RERENDER_INTERVAL_TICKS = 5;
    private int galaxyInstabilityTicks;

    public GalaxyDecorativeBlockEntity(BlockPos pos, BlockState blockState) {
        super(Xavitia.GALAXY_DECORATIVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GalaxyDecorativeBlockEntity blockEntity) {
        if (!state.is(Xavitia.GALAXY_BLOCK.get())) {
            return;
        }

        blockEntity.galaxyInstabilityTicks = Math.min(blockEntity.galaxyInstabilityTicks + 1, GalaxyInstability.galaxyBlockDetonationTicks());
        if (level.isClientSide && blockEntity.galaxyInstabilityTicks % GALAXY_TINT_RERENDER_INTERVAL_TICKS == 0) {
            level.sendBlockUpdated(pos, state, state, 8);
        }
        if (!level.isClientSide && blockEntity.galaxyInstabilityTicks >= GalaxyInstability.galaxyBlockDetonationTicks()) {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, Xavitia.GALAXY_INSTABILITY_EXPLOSION_RADIUS, false, Level.ExplosionInteraction.BLOCK);
        }
    }

    public void setGalaxyInstabilityTicks(int ticks) {
        this.galaxyInstabilityTicks = Math.max(0, Math.min(ticks, GalaxyInstability.galaxyBlockDetonationTicks()));
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public float galaxyTintGreenBlue() {
        return 1.0F - Math.min(1.0F, (float)this.galaxyInstabilityTicks / (float)GalaxyInstability.galaxyBlockDetonationTicks());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.galaxyInstabilityTicks = tag.getInt(GALAXY_INSTABILITY_TICKS_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(GALAXY_INSTABILITY_TICKS_TAG, this.galaxyInstabilityTicks);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }
}
