package com.ddd.endgame;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

public final class GalaxyInstabilityTint {
    private GalaxyInstabilityTint() {
    }

    public static VertexConsumer wrap(VertexConsumer consumer, float greenBlue) {
        if (greenBlue >= 0.999F) {
            return consumer;
        }
        return new TintingVertexConsumer(consumer, greenBlue);
    }

    public static MultiBufferSource wrap(MultiBufferSource buffer, float greenBlue) {
        if (greenBlue >= 0.999F) {
            return buffer;
        }
        return renderType -> wrap(buffer.getBuffer(renderType), greenBlue);
    }

    private static class TintingVertexConsumer extends VertexConsumerWrapper {
        private final float greenBlue;

        private TintingVertexConsumer(VertexConsumer parent, float greenBlue) {
            super(parent);
            this.greenBlue = greenBlue;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return super.setColor(
                    red,
                    Mth.clamp(Math.round(green * this.greenBlue), 0, 255),
                    Mth.clamp(Math.round(blue * this.greenBlue), 0, 255),
                    alpha
            );
        }
    }
}
