package com.ddd.endgame.compat;

import java.lang.reflect.Method;
import java.util.Optional;

public final class IrisCompat {
    private static final String PHOTON_SHADER_PACK_ID = "photon";
    private static boolean checked;
    private static Method getInstanceMethod;
    private static Method isShaderPackInUseMethod;
    private static Method isRenderingShadowPassMethod;
    private static Method getCurrentPackMethod;
    private static Method getCurrentPackNameMethod;
    private static Method getPipelineManagerMethod;
    private static Method getPipelineNullableMethod;
    private static Class<?> shaderRenderingPipelineClass;

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
            if (api == null || !Boolean.TRUE.equals(isShaderPackInUseMethod.invoke(api))) {
                return false;
            }

            Boolean shaderPipelineActive = isShaderPipelineActive();
            if (shaderPipelineActive != null) {
                return shaderPipelineActive;
            }

            if (getCurrentPackMethod == null) {
                return true;
            }
            Object currentPack = getCurrentPackMethod.invoke(null);
            return !(currentPack instanceof Optional<?> optional) || optional.isPresent();
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
            getCurrentPackMethod = irisClass.getMethod("getCurrentPack");
            getCurrentPackNameMethod = irisClass.getMethod("getCurrentPackName");
            getPipelineManagerMethod = irisClass.getMethod("getPipelineManager");
            Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            getPipelineNullableMethod = pipelineManagerClass.getMethod("getPipelineNullable");
            shaderRenderingPipelineClass = Class.forName("net.irisshaders.iris.pipeline.ShaderRenderingPipeline");
        } catch (ReflectiveOperationException ignored) {
            getCurrentPackMethod = null;
            getCurrentPackNameMethod = null;
            getPipelineManagerMethod = null;
            getPipelineNullableMethod = null;
            shaderRenderingPipelineClass = null;
        }
    }

    private static Boolean isShaderPipelineActive() {
        if (getPipelineManagerMethod == null || getPipelineNullableMethod == null || shaderRenderingPipelineClass == null) {
            return null;
        }

        try {
            Object pipelineManager = getPipelineManagerMethod.invoke(null);
            if (pipelineManager == null) {
                return null;
            }

            Object pipeline = getPipelineNullableMethod.invoke(pipelineManager);
            if (pipeline == null) {
                return null;
            }
            return shaderRenderingPipelineClass.isInstance(pipeline);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
