package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AutoResponse {
    private static long reactionWindowEnd = 0;
    private static final Random RANDOM = new Random();

    public static void handleMessage(String text, String text2) {
        var cfg = ClubtimizerConfig.getAutoResponse();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;

        long now = System.currentTimeMillis();
        if (now <= reactionWindowEnd) return;

        var player = Clubtimizer.player;
        if (player != null) {
            double x = player.getX(), z = player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) return;
        }

        String lower = text.toLowerCase();
        if (!lower.contains("§#1fa5ff »")) return;

        int arrowIndex = text2.indexOf('»');
        if (arrowIndex < 0) return;

        String beforeArrow = text2.substring(0, arrowIndex).strip();
        String cleanedName = AutoHush.cleanName(beforeArrow).toLowerCase();
        if (cleanedName.equals(Clubtimizer.playerName.toLowerCase())) return;

        String afterArrow = arrowIndex + 1 < text2.length()
                ? text2.substring(arrowIndex + 1).trim().toLowerCase()
                : "";
        if (afterArrow.isEmpty()) return;

        for (Map.Entry<String, List<String>> entry : cfg.rules.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) continue;

            String[] triggers = key.split(",");
            for (String trigger : triggers) {
                String t = trigger.trim().toLowerCase();
                if (t.isEmpty()) continue;

                if (afterArrow.contains(t)) {
                    reactionWindowEnd = now + 1000L;

                    List<String> responses = entry.getValue();
                    if (responses.isEmpty()) return;

                    String response = responses.get(RANDOM.nextInt(responses.size()));
                    ChatUtil.chat(response);
                    ChatUtil.delayedSay(response, 0x55FFFF, 50);
                    return;
                }
            }
        }
    }
}
