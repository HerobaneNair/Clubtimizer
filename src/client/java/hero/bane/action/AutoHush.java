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
        if (Clubtimizer.player != null) {
            double x = Clubtimizer.player.getX();
            double z = Clubtimizer.player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) {
                return msg;
            }
        }

        String legacy = TextUtil.toLegacyString(msg);
        if (!legacy.contains("»")) return msg;

        int arrowIndex = legacy.indexOf('»');
        String beforeArrow = legacy.substring(0, arrowIndex);
        String afterArrow = legacy.substring(arrowIndex + 1).strip();
        if (beforeArrow.contains(Clubtimizer.playerName)) return msg;

        String realNamePart = beforeArrow.chars().dropWhile(c -> !Character.isLetter(c)).collect(
                StringBuilder::new,
                StringBuilder::appendCodePoint,
                StringBuilder::append
        ).toString().strip();

        if (realNamePart.equalsIgnoreCase(Clubtimizer.playerName)) return msg;

        String lower = legacy.toLowerCase();

        if (lower.contains("§#7a7a7a »")) {
            return buildHidden(beforeArrow, afterArrow, false);
        }

        if (lower.contains("§#1fa5ff »")) {
            String cleaned = TextUtil.stripFormatting(afterArrow).trim().toLowerCase();
            boolean isTrigger = AutoGG.isTrigger(cleaned) || cleaned.equals("ss");

            if (cfg.allowSS && isTrigger) {
                return msg;
            }

            return buildHidden(beforeArrow, afterArrow, true);
        }

        return msg;
    }

    private static Text buildHidden(String beforeArrow, String afterArrow, boolean playerChat) {
        String prefix = playerChat ? beforeArrow + "§#1FA5FF» " : beforeArrow + "§#7A7A7A» ";

        MutableText base = (MutableText) TextUtil.fromLegacy(prefix);

        String hoverText = TextUtil.stripFormatting(afterArrow).trim();
        MutableText hiddenPart = (MutableText) TextUtil.fromLegacy((playerChat ? "§7§m" : "§8§m") + " ".repeat(afterArrow.length()));

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
