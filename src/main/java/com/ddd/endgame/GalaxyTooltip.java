package com.ddd.endgame;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class GalaxyTooltip {
    private static final float CYCLE_MILLIS = 2400.0F;
    private static final float WAVE_STEP = 0.45F;
    private static final int PURPLE = 0xA020F0;
    private static final int WHITE = 0xFFFFFF;

    private GalaxyTooltip() {
    }

    public static Component rainbow(String text) {
        float phase = (Util.getMillis() % (long)CYCLE_MILLIS) / CYCLE_MILLIS;
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float blend = (float)(0.5D + 0.5D * Math.sin((phase * Math.PI * 4.0D) + i * WAVE_STEP));
            component.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(lerpColor(PURPLE, WHITE, blend)))));
        }
        return component;
    }

    private static int lerpColor(int from, int to, float amount) {
        int fromRed = from >> 16 & 0xFF;
        int fromGreen = from >> 8 & 0xFF;
        int fromBlue = from & 0xFF;
        int toRed = to >> 16 & 0xFF;
        int toGreen = to >> 8 & 0xFF;
        int toBlue = to & 0xFF;
        int red = Math.round(fromRed + (toRed - fromRed) * amount);
        int green = Math.round(fromGreen + (toGreen - fromGreen) * amount);
        int blue = Math.round(fromBlue + (toBlue - fromBlue) * amount);
        return red << 16 | green << 8 | blue;
    }
}
