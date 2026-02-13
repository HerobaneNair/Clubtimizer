package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.command.ClubtimizerConfig.AutoResponseRule;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;

import java.util.Random;

public class Response {

    private static long reactionWindowEnd;
    private static final Random RANDOM = new Random();

    public static void handleMessage(String text) {
        var cfg = ClubtimizerConfig.getAutoResponse();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return;

        long now = System.currentTimeMillis();
        if (now <= reactionWindowEnd) return;

        int arrow = text.indexOf('»');
        if (arrow < 0) return;

        String sender = Hush.cleanName(
                text.substring(0, arrow).strip()
        ).toLowerCase();

        if (sender.equals(Clubtimizer.playerName.toLowerCase())) return;

        String message = text.substring(arrow + 1).trim().toLowerCase();
        if (message.isEmpty()) return;

        for (AutoResponseRule rule : cfg.rules.values()) {
            for (String trigger : rule.from) {
                if (message.contains(trigger)) {
                    if (rule.to.isEmpty()) return;

                    reactionWindowEnd = now + 1000L;

                    String response = rule.to.get(RANDOM.nextInt(rule.to.size()));
                    ChatUtil.chat(response);
                    ChatUtil.delayedSay(response, 0x55FFFF, 50);
                    return;
                }
            }
        }
    }
}