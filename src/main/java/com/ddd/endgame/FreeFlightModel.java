package com.ddd.endgame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class FreeFlightModel extends BakedModelWrapper<BakedModel> {
    private static BakedModel originalModel;

    public FreeFlightModel(BakedModel originalModel) {
        super(originalModel);
        FreeFlightModel.originalModel = originalModel;
    }

    public static BakedModel originalModel() {
        return originalModel;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return this;
    }
}
