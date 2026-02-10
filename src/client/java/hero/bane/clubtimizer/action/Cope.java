package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.TextUtil;

import java.util.concurrent.ThreadLocalRandom;

public class Cope {
    private static long lastCopeTime = 0;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoCope();
        if (!cfg.enabled) return;
        if (Clubtimizer.client.player == null) return;
        if (!MCPVPStateChanger.inGame()) return;

        long now = System.currentTimeMillis();
        if (now <= lastCopeTime + 1000) return;

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
    }
}
