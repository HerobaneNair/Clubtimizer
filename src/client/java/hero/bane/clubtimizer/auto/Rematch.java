package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

public class Rematch {

    public static boolean triggered = false;

    private static final String CLICK_FINAL = "Click to Rematch ";
    private static final String OPEN_FINAL = "Click to open chat with ";
    private static final Style REMATCH_COLOR =
            Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);

    public static void handleMessage(String text) {
        if (!TextUtil.roundEnd(text, false) || triggered) return;
        triggered = true;

        var client = Clubtimizer.client;
        var player = client.player;
        if (player == null || client.level == null) return;

        MCPVPState state = MCPVPStateChanger.get();
        if (state != MCPVPState.RED && state != MCPVPState.BLUE) return;

        String opponent = parseOpponent(client);
        if (opponent == null) return;

        MutableComponent clickable = Component.literal("")
                .append(Component.literal(CLICK_FINAL).setStyle(REMATCH_COLOR))
                .append(TextUtil.rainbowGradient(opponent));

        MutableComponent hovered = Component.literal("")
                .append(Component.literal(OPEN_FINAL).setStyle(REMATCH_COLOR))
                .append(TextUtil.rainbowGradient("/duel " + opponent));

        clickable.withStyle(style ->
                style
                        .withClickEvent(new ClickEvent.SuggestCommand(
                                "/duel " + opponent
                        ))
                        .withHoverEvent(new HoverEvent.ShowText(
                                hovered
                        ))
        );


        ChatUtil.delayedSay(clickable);
    }

    private static String parseOpponent(Minecraft client) {
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
