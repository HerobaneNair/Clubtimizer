package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.TextUtil;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

public class TotemReset {
    private static long reactionWindowEnd = 0;
    private static boolean reflectionEnabled = false;
    private static Method resetMethod = null;

    public static void initReflection() {
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
        if (!reflectionEnabled || !(MCPVPStateChanger.inGame() || MCPVPStateChanger.get()== MCPVPState.SPECTATING)) return;

        long now = System.currentTimeMillis();
        if (now < reactionWindowEnd) return;

        if (TextUtil.roundEnd(text, true)) invokeReset(now);
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
