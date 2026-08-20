package com.ddd.endgame.item.models;

import com.ddd.endgame.Xavitia;
import net.minecraft.resources.ResourceLocation;

public class WeatherControllerItemRenderer extends GeneratedStencilItemRenderer {
    public static final WeatherControllerItemRenderer INSTANCE = new WeatherControllerItemRenderer();

    private WeatherControllerItemRenderer() {
        super(
                ResourceLocation.fromNamespaceAndPath(Xavitia.MODID, "textures/item/galaxy_weather_controller.png"),
                () -> GeneratedStencilItemModel.originalModel("weather_cycler"),
                "Unable to load weather controller texture masks"
        );
    }
}
