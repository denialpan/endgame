package com.ddd.endgame.galaxy;

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

    public static MultiBufferSource wrap(MultiBufferSource buffer, float redMultiplier, float greenMultiplier, float blueMultiplier) {
        if (redMultiplier >= 0.999F && greenMultiplier >= 0.999F && blueMultiplier >= 0.999F) {
            return buffer;
        }
        return renderType -> new RgbTintingVertexConsumer(buffer.getBuffer(renderType), redMultiplier, greenMultiplier, blueMultiplier);
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

    private static class RgbTintingVertexConsumer extends VertexConsumerWrapper {
        private final float redMultiplier;
        private final float greenMultiplier;
        private final float blueMultiplier;

        private RgbTintingVertexConsumer(VertexConsumer parent, float redMultiplier, float greenMultiplier, float blueMultiplier) {
            super(parent);
            this.redMultiplier = redMultiplier;
            this.greenMultiplier = greenMultiplier;
            this.blueMultiplier = blueMultiplier;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return super.setColor(
                    Mth.clamp(Math.round(red * this.redMultiplier), 0, 255),
                    Mth.clamp(Math.round(green * this.greenMultiplier), 0, 255),
                    Mth.clamp(Math.round(blue * this.blueMultiplier), 0, 255),
                    alpha
            );
        }
    }
}
