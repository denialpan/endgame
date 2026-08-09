package com.ddd.endgame;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

public class WeatherControllerModel extends BakedModelWrapper<BakedModel> {
    private static BakedModel inventoryModel;
    private static BakedModel handModel;

    public WeatherControllerModel(BakedModel inventoryModel, BakedModel handModel) {
        super(inventoryModel);
        WeatherControllerModel.inventoryModel = inventoryModel;
        WeatherControllerModel.handModel = handModel;
    }

    public static BakedModel inventoryModel() {
        return inventoryModel;
    }

    public static BakedModel handModel() {
        return handModel;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel model = cameraTransformType == ItemDisplayContext.GUI ? inventoryModel : handModel;
        if (model != null) {
            model.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        }
        return this;
    }
}
