package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PingUtil;
import net.minecraft.world.InteractionHand;

import java.util.Set;

public class PartyMaker {

    private static final String NOT_IN_PARTY = "ℹ You are not currently in a party";
    private static final String CREATED_PARTY = "\nCreated party - resend your invite\n";
    public static final Set<String> PARTY_COMMAND_TAB_COMPLETES =
            Set.of(
                    "accept", "chat", "demote", "disband", "invite", "join",
                    "kick", "leave", "maxsize", "promote", "prune", "transfer"
            );
    public static String lastPartyCommand = "";

    public static void handleMessage(String text) {
        if (!text.contains(NOT_IN_PARTY)) return;

        rightClickHorn();

        var client = Clubtimizer.client;
        int dynamicDelay = PingUtil.getDynamicDelay(client, 2);

        ChatUtil.delayedSay(CREATED_PARTY, 0xFFAA00, false, dynamicDelay * 50L);
        ChatUtil.delayedChat(lastPartyCommand, dynamicDelay * 50L);
    }

    private static void rightClickHorn() {
        var client = Clubtimizer.client;
        var player = client.player;
        var gameMode = client.gameMode;

        if (player == null || gameMode == null) return;

        player.getInventory().setSelectedSlot(1);

        gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }
}
