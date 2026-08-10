package com.ddd.endgame.item;

import com.ddd.endgame.EndgameTestStickItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class EndgameTestStickItem extends Item {
    public EndgameTestStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return EndgameTestStickItemRenderer.INSTANCE;
            }
        });
    }
}
