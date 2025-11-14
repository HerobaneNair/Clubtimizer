package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AutoResponse {
    private static long reactionWindowEnd = 0;
    private static final Random RANDOM = new Random();

    public static void handleMessage(String text, String text2) {
        var cfg = ClubtimizerConfig.getAutoResponse();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;
        if (System.currentTimeMillis() < reactionWindowEnd) return;

        if (Clubtimizer.player != null) {
            double x = Clubtimizer.player.getX();
            double z = Clubtimizer.player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) {
                return;
            }
        }

        String lower = text.toLowerCase();
        if (!lower.contains("§#1fa5ff »")) return;

        int arrowIndex = text2.indexOf('»');
        if (arrowIndex < 0 || arrowIndex + 1 >= text2.length()) return;

        String afterArrow = text2.substring(arrowIndex + 1).trim().toLowerCase();

        for (Map.Entry<String, List<String>> entry : cfg.rules.entrySet()) {
            for (String trigger : entry.getKey().split(",")) {
                String triggerLower = trigger.toLowerCase().trim();
                if (afterArrow.contains(triggerLower)) {
                    reactionWindowEnd = System.currentTimeMillis() + 1000L;
                    List<String> responses = entry.getValue();
                    if (!responses.isEmpty()) {
                        String response = responses.get(RANDOM.nextInt(responses.size()));
                        ChatUtil.chat(response);
                        ChatUtil.delayedSay(response, 0x55FFFF, 50);
                        return;
                    }
                }
            }
        }
    }
}
