package com.ddd.endgame.compat;

import java.lang.reflect.Method;

final class GnetumCompat {
    private static final String STENCIL_RENDER_REASON = "xavitia skybox item uses stencil state";
    private static boolean checked;
    private static Method disableCachingForCurrentElementMethod;

    private GnetumCompat() {
    }

    static void disableCachingForCurrentElement() {
        initializeIfNeeded();
        if (disableCachingForCurrentElementMethod == null) {
            return;
        }

        try {
            disableCachingForCurrentElementMethod.invoke(null, STENCIL_RENDER_REASON);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void initializeIfNeeded() {
        if (checked) {
            return;
        }

        checked = true;
        try {
            Class<?> gnetumClass = Class.forName("me.decce.gnetum.Gnetum");
            disableCachingForCurrentElementMethod = gnetumClass.getMethod("disableCachingForCurrentElement", String.class);
        } catch (ReflectiveOperationException ignored) {
            disableCachingForCurrentElementMethod = null;
        }
    }
}
