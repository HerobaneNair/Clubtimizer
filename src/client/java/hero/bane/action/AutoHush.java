package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.TextUtil;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class AutoHush {
    public static boolean matchJoin = true;
    public static boolean allowLobbyJoin = false;

    public static Text replaceMessage(Text msg) {
        var cfg = ClubtimizerConfig.getAutoHush();
        if (!cfg.enabled || !MCPVPStateChanger.inGame()) return msg;

        var player = Clubtimizer.player;
        if (player != null) {
            double x = player.getX(), z = player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) return msg;
        }

        String legacy = TextUtil.toLegacyString(msg);
        int arrowIndex = legacy.indexOf('»');
        if (arrowIndex < 0) return msg;

        String beforeArrow = legacy.substring(0, arrowIndex);
        if (TextUtil.fastContains(beforeArrow,Clubtimizer.playerName)) return msg;

        String afterArrow = legacy.substring(arrowIndex + 1).strip();

        String realNamePart = beforeArrow.chars()
                .dropWhile(c -> !Character.isLetter(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();

        if (realNamePart.equalsIgnoreCase(Clubtimizer.playerName)) return msg;

        String lower = legacy.toLowerCase();

        if (TextUtil.fastContains(lower,"§#7a7a7a »")) {
            return buildHidden(beforeArrow, afterArrow, false);
        }

        if (TextUtil.fastContains(lower,"§#1fa5ff »")) {
            String cleaned = TextUtil.stripFormatting(afterArrow).trim().toLowerCase();
            boolean isTrigger = AutoGG.isTrigger(cleaned) || cleaned.equals("ss");
            if (cfg.allowSS && isTrigger) return msg;
            return buildHidden(beforeArrow, afterArrow, true);
        }

        return msg;
    }

    private static Text buildHidden(String beforeArrow, String afterArrow, boolean playerChat) {
        String prefix = playerChat ? beforeArrow + "§#1FA5FF» " : beforeArrow + "§#7A7A7A» ";
        MutableText base = (MutableText) TextUtil.fromLegacy(prefix);

        String hoverText = TextUtil.stripFormatting(afterArrow).trim();
        int len = afterArrow.length();
        String maskColor = playerChat ? "§7§m" : "§8§m";
        MutableText hiddenPart = (MutableText) TextUtil.fromLegacy(maskColor + " ".repeat(len));

        if (!hoverText.isEmpty()) {
            hiddenPart.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverText))));
            Clubtimizer.LOGGER.info("Message: {}", hoverText);
        }

        base.append(hiddenPart);
        return base;
    }

    public static void onMatchJoin() {
        var cfg = ClubtimizerConfig.getAutoHush();
        if (!cfg.enabled || Clubtimizer.client.player == null || cfg.joinMessage.isEmpty()) return;
        if (!matchJoin && !allowLobbyJoin) return;
        hero.bane.util.ChatUtil.delayedChat(cfg.joinMessage);
        matchJoin = false;
        allowLobbyJoin = false;
    }
}
