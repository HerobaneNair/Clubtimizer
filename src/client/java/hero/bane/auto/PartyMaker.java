package hero.bane.auto;

import hero.bane.Clubtimizer;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public class PartyMaker {
    private static final String NOT_IN_PARTY = "ℹ You are not currently in a party";
    private static final String CREATED_PARTY = "---\nCreated party - resend your invite\n---";

    public static void handleMessage(String text) {
        if (!text.contains(NOT_IN_PARTY)) return;
        rightClickHorn();
        MinecraftClient client = Clubtimizer.client;
        ChatUtil.delayedSay(CREATED_PARTY, 0xFFAA00, TextUtil.getDynamicDelay(client, 2) * 50L);
    }

    private static void rightClickHorn() {
        MinecraftClient client = Clubtimizer.client;
        if (client.player == null || client.interactionManager == null) return;
        client.player.getInventory().setSelectedSlot(1);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }
}
