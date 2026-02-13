package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.TextUtil;

public class GG {
    private static long reactionWindowEnd = 0;
    private static boolean sentGG = true;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;

        if (inSpawn()) return;

        long now = System.currentTimeMillis();
        boolean messageWorks = TextUtil.roundEnd(text, cfg.roundEnabled);

        if (messageWorks) {
            sentGG = false;
            if (!cfg.reactionary && now > reactionWindowEnd) ChatUtil.delayedChat(cfg.message);
            reactionWindowEnd = now + 10000L;
            return;
        }

        if (!cfg.reactionary || sentGG) return;

        int arrowIndex = text.indexOf('»');
        if (arrowIndex < 0) return;

        String beforeArrow = text.substring(0, arrowIndex);
        String realNamePart = beforeArrow.chars()
                .dropWhile(c -> !Character.isLetter(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();
        if (realNamePart.equalsIgnoreCase(Clubtimizer.playerName)) return;

        String afterArrow = arrowIndex + 1 < text.length() ? text.substring(arrowIndex + 1) : "";

        String cleaned = afterArrow.strip().toLowerCase();
        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) cleaned = cleaned.substring(0, spaceIdx);

        if (isTrigger(cleaned) && now < reactionWindowEnd) {
            ChatUtil.chat(cfg.message);
            sentGG = true;
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

    public static boolean inSpawn() {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return false;
        var clientPlayer = Clubtimizer.player;
        if (clientPlayer != null) {
            double x = clientPlayer.getX(), z = clientPlayer.getZ();
            return ((x > -300) && (x < 300) && (z > -300) && (z < 300));
        }
        return true;
    }
}