package com.ddd.endgame.mixin;

import com.ddd.endgame.compat.CreateFabricatorInventoryCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = {
                "com.simibubi.create.foundation.item.SmartInventory",
                "com.simibubi.create.foundation.item.ItemHandlerWrapper",
                "com.simibubi.create.content.logistics.depot.DepotItemHandler",
                "com.simibubi.create.content.logistics.chute.ChuteItemHandler",
                "com.simibubi.create.content.logistics.packager.PackagerItemHandler",
                "com.simibubi.create.content.logistics.tunnel.BrassTunnelItemHandler",
                "com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment"
        },
        remap = false
)
public class CreateItemHandlerMixin {
    @Inject(method = "getStackInSlot", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void dddsendgame$getFabricatorOutputStack(int slot, CallbackInfoReturnable<ItemStack> callbackInfo) {
        callbackInfo.setReturnValue(CreateFabricatorInventoryCompat.visibleStack(callbackInfo.getReturnValue()));
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void dddsendgame$extractFabricatorOutput(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> callbackInfo) {
        ItemStack extracted = CreateFabricatorInventoryCompat.extractFromFabricator(this, slot, amount);
        if (!extracted.isEmpty()) {
            callbackInfo.setReturnValue(extracted);
        }
    }
}
