package com.ddd.endgame.compat;

import java.lang.reflect.Method;

final class ImmediatelyFastCompat {
    private static boolean checked;
    private static Method flushHudBuffersMethod;

    private ImmediatelyFastCompat() {
    }

    static void flushHudBuffers() {
        initializeIfNeeded();
        if (flushHudBuffersMethod == null) {
            return;
        }

        try {
            flushHudBuffersMethod.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void initializeIfNeeded() {
        if (checked) {
            return;
        }

        checked = true;
        try {
            Class<?> batchingBuffersClass = Class.forName("net.raphimc.immediatelyfast.feature.batching.BatchingBuffers");
            flushHudBuffersMethod = batchingBuffersClass.getMethod("tryForceDrawHudBuffers");
        } catch (ReflectiveOperationException ignored) {
            flushHudBuffersMethod = null;
        }
    }
}
