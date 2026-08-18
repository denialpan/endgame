package com.ddd.endgame.mixin;

import com.ddd.endgame.Xavitia;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void xavitia$pushFabricatorOutput(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo callbackInfo) {
        if (Xavitia.handleFabricatorHopperTick(level, pos, state, blockEntity)) {
            callbackInfo.cancel();
        }
    }
}
