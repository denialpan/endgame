package com.ddd.endgame.item.models;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class GeneratedStencilItemModel extends BakedModelWrapper<BakedModel> {
    private static final Map<String, BakedModel> ORIGINAL_MODELS = new HashMap<>();
    private final String id;

    public GeneratedStencilItemModel(BakedModel originalModel, String id) {
        super(originalModel);
        this.id = id;
        ORIGINAL_MODELS.put(id, originalModel);
    }

    public static BakedModel originalModel(String id) {
        return ORIGINAL_MODELS.get(id);
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
