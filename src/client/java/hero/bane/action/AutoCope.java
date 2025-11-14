package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;

import java.util.concurrent.ThreadLocalRandom;

public class AutoCope {
    private static long lastCopeTime = 0;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoCope();
        if (!cfg.enabled) return;
        if (Clubtimizer.client.player == null) return;
        if (!MCPVPStateChanger.inGame()) return;

        long now = System.currentTimeMillis();
        if (now - lastCopeTime < 1000) return;

        String name = Clubtimizer.playerName;
        if (TextUtil.containsAny(text,
                name + " blew themselves up",
                name + " died",
                name + " was blown up by",
                name + " was smashed by",
                name + " was slain by")) {
            lastCopeTime = now;
            sayRandomPhrase(cfg);
        }
    }

    private static void sayRandomPhrase(ClubtimizerConfig.AutoCopeConfig cfg) {
        if (cfg.phrases.isEmpty()) return;
        String msg = cfg.phrases.get(ThreadLocalRandom.current().nextInt(cfg.phrases.size()));
        ChatUtil.delayedChat(msg, 200);
        ChatUtil.delayedSay("[Clubtimizer a-c]: " + msg, 0xFFAA00, 200);
    }
}