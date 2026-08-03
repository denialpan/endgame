package com.ddd.endgame.compat;

import java.lang.reflect.Method;

public final class IrisCompat {
    private static final String PHOTON_SHADER_PACK_ID = "photon";
    private static boolean checked;
    private static Method getInstanceMethod;
    private static Method isShaderPackInUseMethod;
    private static Method isRenderingShadowPassMethod;
    private static Method getCurrentPackNameMethod;

    private IrisCompat() {
    }

    public static boolean isShaderPackInUse() {
        if (!checked) {
            initialize();
        }
        if (getInstanceMethod == null || isShaderPackInUseMethod == null) {
            return false;
        }

        try {
            Object api = getInstanceMethod.invoke(null);
            return api != null && Boolean.TRUE.equals(isShaderPackInUseMethod.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isPhotonShaderPackInUse() {
        if (!isShaderPackInUse() || getCurrentPackNameMethod == null) {
            return false;
        }

        try {
            Object packName = getCurrentPackNameMethod.invoke(null);
            return packName instanceof String name && name.toLowerCase().contains(PHOTON_SHADER_PACK_ID);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isRenderingShadowPass() {
        if (!checked) {
            initialize();
        }
        if (getInstanceMethod == null || isRenderingShadowPassMethod == null) {
            return false;
        }

        try {
            Object api = getInstanceMethod.invoke(null);
            return api != null && Boolean.TRUE.equals(isRenderingShadowPassMethod.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void initialize() {
        checked = true;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            getInstanceMethod = apiClass.getMethod("getInstance");
            isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
            isRenderingShadowPassMethod = apiClass.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException ignored) {
            getInstanceMethod = null;
            isShaderPackInUseMethod = null;
            isRenderingShadowPassMethod = null;
        }

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            getCurrentPackNameMethod = irisClass.getMethod("getCurrentPackName");
        } catch (ReflectiveOperationException ignored) {
            getCurrentPackNameMethod = null;
        }
    }
}
