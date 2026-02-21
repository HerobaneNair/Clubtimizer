package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PlayerUtil;
import hero.bane.clubtimizer.util.TextUtil;

public class GG {
    private static long reactionWindow = 0;
    private static boolean sentGG = true;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (!cfg.enabled || !MCPVPStateChanger.inGame() || Clubtimizer.client.level == null) return;
        if (PlayerUtil.inSpawnArea()) return;

        long now = Clubtimizer.client.level.getGameTime();
        boolean messageWorks = TextUtil.roundEnd(text, cfg.roundEnabled);

        if (messageWorks) {
            sentGG = false;
            if (!cfg.reactionary && now > reactionWindow) ChatUtil.delayedChat(cfg.message);
            reactionWindow = now + 200L;
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

        if (isTrigger(cleaned) && now < reactionWindow) {
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

    public static void resetReactionWindow() {
        if (Clubtimizer.client.level == null) return;
        reactionWindow = Clubtimizer.client.level.getGameTime();
    }

    public static void setReactionWindow() {
        if (Clubtimizer.client.level == null) return;
        reactionWindow = Clubtimizer.client.level.getGameTime() + 200L;
        sentGG = true;
    }
}