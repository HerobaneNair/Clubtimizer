package hero.bane.auto;

import hero.bane.Clubtimizer;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;

import java.util.List;

public class Rematch {
    public static boolean triggered = false;

    public static void handleMessage(String text) {
        if (!TextUtil.fastContains(text,"⚔ Match Complete")) return;
        if (triggered) return;
        triggered = true;

        var client = Clubtimizer.client;
        var player = client.player;
        if (player == null || client.world == null) return;

        MCPVPState state = MCPVPStateChanger.get();
        if (state != MCPVPState.RED && state != MCPVPState.BLUE) return;

        String opponentRaw = getOpponentFromTabList(client);
        if (opponentRaw == null || opponentRaw.isEmpty()) return;

        String[] parts = opponentRaw.split(" ");
        if (parts.length < 2) return;
        String opponent = parts[1];

        MutableText clickable = Text.literal("")
                .append(Text.literal("[ Click to Rematch").styled(s -> s.withColor(0x55FFFF)))
                .append(TextUtil.rainbowGradient(" " + opponent))
                .append(Text.literal(" ]").styled(s -> s.withColor(0x55FFFF)));

        clickable.setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.SuggestCommand("/duel " + opponent))
                .withHoverEvent(new HoverEvent.ShowText(
                        Text.literal("Click to open chat with \n'/duel " + opponent + "'")))
        );

        ChatUtil.delayedSay(clickable);
    }

    private static String getOpponentFromTabList(MinecraftClient client) {
        List<String> lines = TextUtil.getOrderedTabList(client);
        if (lines.isEmpty()) return null;

        String self = Clubtimizer.playerName.toLowerCase();
        for (String raw : lines) {
            String clean = TextUtil.stripFormatting(raw).trim();
            String lower = clean.toLowerCase();
            if (TextUtil.fastContains(lower,"ms") && !TextUtil.fastContains(lower,self)) return clean;
        }
        return null;
    }
}
