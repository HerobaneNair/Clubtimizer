package hero.bane.auto;

import hero.bane.Clubtimizer;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;
import net.minecraft.util.Hand;

public class PartyMaker {
    private static final String NOT_IN_PARTY = "ℹ You are not currently in a party";
    private static final String CREATED_PARTY = "---\nCreated party - resend your invite\n---";

    public static void handleMessage(String text) {
        if (!text.contains(NOT_IN_PARTY)) return;
        rightClickHorn();
        var client = Clubtimizer.client;
        ChatUtil.delayedSay(CREATED_PARTY, 0xFFAA00, TextUtil.getDynamicDelay(client, 2) * 50L);
    }

    private static void rightClickHorn() {
        var client = Clubtimizer.client;
        var player = client.player;
        var im = client.interactionManager;
        if (player == null || im == null) return;
        player.getInventory().setSelectedSlot(1);
        im.interactItem(player, Hand.MAIN_HAND);
    }
}