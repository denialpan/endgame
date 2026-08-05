package com.ddd.endgame.item;

import com.ddd.endgame.GalaxyIngotItemRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class GalaxyIngotItem extends Item {
    public GalaxyIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return GalaxyIngotItemRenderer.INSTANCE;
            }
        });
    }
}
