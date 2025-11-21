package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;

public class AutoGG {
    private static long reactionWindowEnd = 0;
    private static boolean unsentGG = true;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;

        var player = Clubtimizer.player;
        if (player != null) {
            double x = player.getX();
            double z = player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) return;
        }

        long now = System.currentTimeMillis();
        boolean messageWorks = TextUtil.roundEnd(text, cfg.roundEnabled);

        if (messageWorks) {
            unsentGG = false;
            if (!cfg.reactionary && now > reactionWindowEnd) ChatUtil.delayedChat(cfg.message);
            reactionWindowEnd = now + 10000L;
            return;
        }

        if (!cfg.reactionary || unsentGG) return;

        int arrowIndex = text.indexOf('»');
        if (arrowIndex < 0) return;

        String beforeArrow = text.substring(0, arrowIndex);
        String afterArrow = arrowIndex + 1 < text.length() ? text.substring(arrowIndex + 1) : "";

        String realNamePart = beforeArrow.chars()
                .dropWhile(c -> !Character.isLetter(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();

        if (realNamePart.equalsIgnoreCase(Clubtimizer.playerName)) return;

        String cleaned = afterArrow.strip().toLowerCase();
        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) cleaned = cleaned.substring(0, spaceIdx);

        if (isTrigger(cleaned) && now < reactionWindowEnd) {
            ChatUtil.chat(cfg.message);
            unsentGG = true;
        }
    }

    public static boolean isTrigger(String cleaned) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (!cfg.enabled) return false;
        for (String t : cfg.triggers) {
            if (cleaned.equalsIgnoreCase(t)) return true;
        }
        return false;
    }

    public static void resetReactionWindowEnd() {
        reactionWindowEnd = System.currentTimeMillis();
    }
}