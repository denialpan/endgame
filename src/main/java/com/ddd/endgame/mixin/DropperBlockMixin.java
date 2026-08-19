package com.ddd.endgame.mixin;

import com.ddd.endgame.compat.ItemFabricatorVanillaAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropperBlock.class)
public class DropperBlockMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void xavitia$dispenseFabricatorOutput(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo callbackInfo) {
        if (ItemFabricatorVanillaAutomation.dispenseFromDropper(level, state, pos)) {
            callbackInfo.cancel();
        }
    }
}
