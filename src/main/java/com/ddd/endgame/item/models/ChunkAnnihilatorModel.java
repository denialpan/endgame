package com.ddd.endgame.item.models;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class ChunkAnnihilatorModel extends BakedModelWrapper<BakedModel> {
    private static BakedModel originalModel;

    public ChunkAnnihilatorModel(BakedModel originalModel) {
        super(originalModel);
        ChunkAnnihilatorModel.originalModel = originalModel;
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
