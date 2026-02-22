package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public class Totem {
    private static long reactionWindowEnd = 0;
    private static boolean reflectionEnabled = false;
    public static boolean saidTotemFarmInChat = false;
    private static Method resetMethod = null;
    private static Method getPopsMethod = null;


    public static void initReflection() {
        if (!FabricLoader.getInstance().isModLoaded("totemcounter")) {
            reflectionEnabled = false;
            resetMethod = null;
            return;
        }
        try {
            Class<?> cls = Class.forName("net.uku3lig.totemcounter.TotemCounter");
            resetMethod = cls.getMethod("resetPopCounter");
            //            getPopsMethod = cls.getMethod("getCount", Player.class);
            reflectionEnabled = true;
        } catch (Throwable ignored) {
            reflectionEnabled = false;
            resetMethod = null;
        }
    }

    public static void handleMessage(String text) {
        if (!reflectionEnabled || !(MCPVPStateChanger.inGame() || MCPVPStateChanger.get() == MCPVPState.SPECTATING))
            return;

        long now = System.currentTimeMillis();
        if (now < reactionWindowEnd) return;

        if (TextUtil.roundEnd(text, true)) {
            saidTotemFarmInChat = false;
            invokeReset(now);
        }
    }

//    public static void handleTick(Minecraft minecraft) {
//        if (!reflectionEnabled || saidTotemFarmInChat) return;
//        if (minecraft.level == null || minecraft.player == null) return;
//
//        long tick = minecraft.level.getGameTime();
//        if ((tick % 20) != 0) return;
//
//        Player self = minecraft.player;
//
//        Player nearest = minecraft.level.players().stream()
//                .filter(p -> p != self)
//                .filter(p -> !p.isRemoved())
//                .min(Comparator.comparingDouble(a -> a.distanceToSqr(self)))
//                .orElse(null);
//
//        if (nearest == null) return;
//
//        int pops = invokeGetCount(nearest);
//
//        if (pops > 14) {
//            ChatUtil.say("Popped opponent 15 times [14+death]");
//            saidTotemFarmInChat = true;
//        }
//    }

    public static void resetCounter() {
        if (!reflectionEnabled) return;
        long now = System.currentTimeMillis();
        if (now < reactionWindowEnd) return;
        invokeReset(now);
    }

    private static void invokeReset(long now) {
        try {
            resetMethod.invoke(null);
        } catch (Throwable ignored) {
        }
        reactionWindowEnd = now + 2000L;
    }

//    private static int invokeGetCount(Player player) {
//        try {
//            return (int) getPopsMethod.invoke(null, player);
//        } catch (Throwable ignored) {
//            return 0;
//        }
//    }
}
