package hero.bane.auto;

import hero.bane.Clubtimizer;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

public class Rematch {
    public static boolean triggered = false;

    private static final String CLICK_FINAL = "[Click to Rematch ";
    private static final String OPEN_FINAL = "Click to open chat with ";
    private static final Style REMATCH_COLOR = Style.EMPTY.withColor(Formatting.LIGHT_PURPLE);

    public static void handleMessage(String text) {
        if (!TextUtil.roundEnd(text, false) || triggered) return;
        triggered = true;

        var client = Clubtimizer.client;
        var player = client.player;
        if (player == null || client.world == null) return;

        MCPVPState state = MCPVPStateChanger.get();
        if (state != MCPVPState.RED && state != MCPVPState.BLUE) return;

        String opponent = parseOpponent(client);
        if (opponent == null) return;

        MutableText clickable = Text.literal("")
                .append(Text.literal(CLICK_FINAL).setStyle(REMATCH_COLOR))
                .append(TextUtil.rainbowGradient(opponent))
                .append(Text.literal("]").setStyle(REMATCH_COLOR));

        MutableText hovered = Text.literal("")
                .append(Text.literal(OPEN_FINAL).setStyle(REMATCH_COLOR))
                .append(TextUtil.rainbowGradient("/duel " + opponent));

        clickable.setStyle(
                Style.EMPTY
                        .withClickEvent(new ClickEvent.SuggestCommand("/duel " + opponent))
                        .withHoverEvent(new HoverEvent.ShowText(hovered))
        );
        ChatUtil.delayedSay(clickable);
    }

    private static String parseOpponent(MinecraftClient client) {
        List<String> lines = TextUtil.getOrderedTabList(client);
        if (lines.isEmpty()) return null;

        String self = Clubtimizer.playerName;

        for (String raw : lines) {
            String clean = TextUtil.stripFormatting(raw).trim();

            if (!clean.contains(self) && clean.contains("ms")) {
                String[] parts = clean.split(" ");
                return parts.length >= 2 ? parts[1] : null;
            }
        }
        return null;
    }
}