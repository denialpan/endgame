package com.ddd.endgame.mixin;

import com.ddd.endgame.item.TheStickItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true)
    private void dddsendgame$theStickPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        if (TheStickItem.grantsServerCommandPermissions(this.entity)) {
            cir.setReturnValue(true);
        }
    }
}
