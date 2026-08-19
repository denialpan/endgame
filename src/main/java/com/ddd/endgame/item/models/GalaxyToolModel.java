package com.ddd.endgame.item.models;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class GalaxyToolModel extends BakedModelWrapper<BakedModel> {
    private static final BakedModel[] ORIGINAL_MODELS = new BakedModel[5];
    private final int toolIndex;

    public GalaxyToolModel(BakedModel originalModel, int toolIndex) {
        super(originalModel);
        this.toolIndex = toolIndex;
        ORIGINAL_MODELS[toolIndex] = originalModel;
    }

    public static BakedModel originalModel(int toolIndex) {
        return toolIndex >= 0 && toolIndex < ORIGINAL_MODELS.length ? ORIGINAL_MODELS[toolIndex] : null;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return this;
    }
}
