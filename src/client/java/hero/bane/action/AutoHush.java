package hero.bane.action;

import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.FriendUtil;
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

        String beforeArrow = legacy.substring(0, arrowIndex).strip();
        String afterArrow = legacy.substring(arrowIndex + 1).strip();

        String realNamePart = cleanName(beforeArrow);
        if (isSelfOrFriend(realNamePart)) return msg;

        String lower = legacy.toLowerCase();

        if (lower.contains("§#7a7a7a »")) {
            return buildHidden(beforeArrow, afterArrow, false);
        }

        if (lower.contains("§#1fa5ff »")) {
            String cleaned = TextUtil.stripFormatting(afterArrow).trim().toLowerCase();
            boolean isTrigger = AutoGG.isTrigger(cleaned) || cleaned.equals("ss");
            if (cfg.allowSS && isTrigger) return msg;
            return buildHidden(beforeArrow, afterArrow, true);
        }

        return msg;
    }

    public static void onMatchJoin() {
        var cfg = ClubtimizerConfig.getAutoHush();
        if (!cfg.enabled || Clubtimizer.client.player == null || cfg.joinMessage.isEmpty()) return;
        if (!matchJoin && !allowLobbyJoin) return;
        ChatUtil.delayedChat(cfg.joinMessage);
        matchJoin = false;
        allowLobbyJoin = false;
    }

    public static String cleanName(String raw) {
        if (raw == null) return "";

        String stripped = TextUtil.stripFormatting(raw).strip();
        if (stripped.isEmpty()) return stripped;

        char first = stripped.charAt(0);
        if (!Character.isLetterOrDigit(first)) {
            stripped = stripped.substring(1).strip();
        }

        int end = stripped.length();
        while (end > 0 && !Character.isLetterOrDigit(stripped.charAt(end - 1))) {
            end--;
        }
        return stripped.substring(0, end);
    }

    private static boolean isSelfOrFriend(String raw) {
        String name = cleanName(raw).toLowerCase();
        if (name.equals(Clubtimizer.playerName.toLowerCase())) return true;
        return FriendUtil.isFriend(name);
    }

    private static Text buildHidden(String beforeArrow, String afterArrow, boolean playerChat) {
        String prefix = playerChat ? beforeArrow + "§#1FA5FF » " : beforeArrow + "§#7A7A7A » ";
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
}
