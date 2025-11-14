package hero.bane.auto;

import hero.bane.state.MCPVPStateChanger;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

public class TotemResetter {
    private static long reactionWindowEnd = 0;
    private static boolean reflectionEnabled = false;
    private static Method resetMethod = null;

    public static void initializeReflection() {
        if (!FabricLoader.getInstance().isModLoaded("totemcounter")) {
            reflectionEnabled = false;
            resetMethod = null;
            return;
        }
        try {
            Class<?> cls = Class.forName("net.uku3lig.totemcounter.TotemCounter");
            resetMethod = cls.getMethod("resetPopCounter");
            reflectionEnabled = true;
        } catch (Throwable ignored) {
            reflectionEnabled = false;
            resetMethod = null;
        }
    }

    public static void handleMessage(String text) {
        if (!reflectionEnabled) return;
        if (!MCPVPStateChanger.inGame()) return;
        if (System.currentTimeMillis() < reactionWindowEnd) return;
        boolean messageWorks = text.contains("⚔ Match Complete") ||
                (text.contains("won the round") && !text.contains("»"));
        if (!messageWorks) return;
        invokeReset();
    }

    public static void resetCounter() {
        if (!reflectionEnabled) return;
        if (System.currentTimeMillis() < reactionWindowEnd) return;
        invokeReset();
    }

    private static void invokeReset() {
        try {
            resetMethod.invoke(null);
        } catch (Throwable ignored) {}
        reactionWindowEnd = System.currentTimeMillis() + 2000L;
    }
}
