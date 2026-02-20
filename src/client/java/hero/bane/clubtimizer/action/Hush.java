package hero.bane.clubtimizer.action;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PlayerUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class Hush {
    public static boolean matchJoin = true;
    public static boolean allowLobbyJoin = false;

    public static Component replaceMessage(Component msg) {
        if (!MCPVPStateChanger.inGame() || PlayerUtil.inSpawnArea()) return msg;

        var cfg1 = ClubtimizerConfig.getAutoHush();
        var cfg2 = ClubtimizerConfig.getSpecChat();
        if (!cfg1.hushed && (cfg2.mode == ClubtimizerConfig.specChatMode.off)) return msg;

        String legacy = TextUtil.toLegacyString(msg);
        int arrowIndex = legacy.indexOf('»');
        if (arrowIndex < 0) return msg;

        String beforeArrow = legacy.substring(0, arrowIndex).strip();
        String afterArrow = legacy.substring(arrowIndex + 1).strip();

        if (isAutoGGTrigger(afterArrow)) return msg;

        String realNamePart = cleanName(beforeArrow);
        if (isSelfOrFriend(realNamePart)) return msg;

        String lower = legacy.toLowerCase();

        if (lower.contains("§#1fa5ff »")) {
            if (cfg1.hushed) {
                return buildHidden(beforeArrow, afterArrow, true);
            } else {
                return msg;
            }
        }

        if (lower.contains("§#7a7a7a »")) {
            return switch (cfg2.mode) {
                case ClubtimizerConfig.specChatMode.on -> (cfg1.hushed ? buildHidden(beforeArrow, afterArrow, false) : msg);
                case ClubtimizerConfig.specChatMode.compress -> buildHidden(beforeArrow, afterArrow, false);
                case ClubtimizerConfig.specChatMode.off -> Component.literal("\uD83D\uDC41");
            };
        }

        return msg;
    }

    public static void onMatchJoin() {
        var cfg = ClubtimizerConfig.getAutoHush();
        if (!cfg.hushed || Clubtimizer.client.player == null || cfg.joinMessage.isEmpty() || cfg.joinMessage.equals("."))
            return;
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
        return PlayerUtil.isFriend(name);
    }

    private static boolean isAutoGGTrigger(String afterArrow) {
        if (afterArrow == null) return false;

        String cleaned = TextUtil.stripFormatting(afterArrow)
                .strip()
                .toLowerCase();

        if (cleaned.isEmpty()) return false;

        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx > 0) cleaned = cleaned.substring(0, spaceIdx);

        return GG.isTrigger(cleaned);
    }

    private static Component buildHidden(String beforeArrow, String afterArrow, boolean playerChat) {
        String prefix = playerChat ? beforeArrow + "§#1FA5FF » " : beforeArrow + "§#7A7A7A » ";

        MutableComponent base = (MutableComponent) TextUtil.fromLegacy(prefix);

        String hoverText = TextUtil.stripFormatting(afterArrow).trim();
        int len = afterArrow.length();
        String maskColor = playerChat ? "§7§m" : "§8§m";
        MutableComponent hiddenPart = (MutableComponent) TextUtil.fromLegacy(maskColor + " ".repeat(len));

        if (!hoverText.isEmpty()) {
            hiddenPart.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
            Clubtimizer.LOGGER.info("Message: {}", hoverText);
        }

        base.append(hiddenPart);
        return base;
    }
}