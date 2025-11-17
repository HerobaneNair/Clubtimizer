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
        if (!reflectionEnabled || !MCPVPStateChanger.inGame()) return;

        long now = System.currentTimeMillis();
        if (now < reactionWindowEnd) return;

        boolean match =
                text.contains("⚔ Match Complete") ||
                        (text.contains("won the round") && !text.contains("»"));

        if (match) invokeReset(now);
    }

    public static void resetCounter() {
        if (!reflectionEnabled) return;
        long now = System.currentTimeMillis();
        if (now < reactionWindowEnd) return;
        invokeReset(now);
    }

    private static void invokeReset(long now) {
        try {
            resetMethod.invoke(null);
        } catch (Throwable ignored) {}
        reactionWindowEnd = now + 2000L;
    }
}
