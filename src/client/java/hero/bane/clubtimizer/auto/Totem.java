package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.mixin.accessor.TotemCounterAccessor;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.multiplayer.ClientLevel;

public class Totem {
    private static long reactionWindowEnd = 0;
    public static boolean totemCounterLoaded = false;

    public static void handleMessage(String text) {
        if (!totemCounterLoaded || !(MCPVPStateChanger.inGame() || MCPVPStateChanger.get() == MCPVPState.SPECTATING))
            return;

        ClientLevel clientLevel = Clubtimizer.client.level;
        if (clientLevel == null) return;
        long now = Clubtimizer.client.level.getGameTime();
        if (now < reactionWindowEnd) return;

        if (TextUtil.roundEnd(text, true)) {
            resetPops();
            reactionWindowEnd = now + 40L;
        }
    }

    public static void resetPops() {
        TotemCounterAccessor.getPops().clear();
        reactionWindowEnd = 0;
    }
}
