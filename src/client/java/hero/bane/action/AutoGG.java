package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;

public class AutoGG {
    private static long reactionWindowEnd = 0;
    private static boolean unsentGG = true;

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;

        if (Clubtimizer.player != null) {
            double x = Clubtimizer.player.getX();
            double z = Clubtimizer.player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) {
                return;
            }
        }

        boolean messageWorks = text.contains("⚔ Match Complete")
                || (cfg.roundEnabled && text.contains("won the round") && text.contains("\uD83D\uDDE1"));

        if (messageWorks) {
            unsentGG = false;
            if (!cfg.reactionary && System.currentTimeMillis() > reactionWindowEnd) {
                ChatUtil.delayedChat(cfg.message);
            }
            reactionWindowEnd = System.currentTimeMillis() + 10000L;
        } else if (cfg.reactionary && !unsentGG) {
            assert Clubtimizer.client.player != null;

            int arrowIndex = text.indexOf('»');
            String beforeArrow = "";
            String afterArrow = "";

            if (arrowIndex >= 0) {
                beforeArrow = text.substring(0, arrowIndex);
                if (arrowIndex + 1 < text.length()) {
                    afterArrow = text.substring(arrowIndex + 1);
                }
            }

            String realNamePart = beforeArrow.chars()
                    .dropWhile(c -> !Character.isLetter(c))
                    .collect(StringBuilder::new,
                            StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString()
                    .strip();

            if (!realNamePart.equalsIgnoreCase(Clubtimizer.playerName)) {

                String cleaned = afterArrow.strip().toLowerCase().split("\\s+")[0];

                if (isTrigger(cleaned)) {
                    if (System.currentTimeMillis() < reactionWindowEnd) {
                        ChatUtil.delayedChat(cfg.message);
                        unsentGG = true;
                    }
                }
            }
        }
    }

    public static boolean isTrigger(String cleaned) {
        var cfg = ClubtimizerConfig.getAutoGG();
        if (cfg.enabled) {
            for (String t : cfg.triggers) {
                if (cleaned.equalsIgnoreCase(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void resetReactionWindowEnd() {
        reactionWindowEnd = System.currentTimeMillis();
    }
}
