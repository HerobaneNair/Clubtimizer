package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.mixin.accessor.PlayerListEntryAccessor;
import hero.bane.clubtimizer.util.PingUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class Tablist {

    public static String removeMs(String s) {
        if (!s.contains("ms")) return s;
        int idx = s.indexOf("ms");
        if (idx < 0) return s;

        int start = idx - 1;
        while (start >= 0 && Character.isDigit(s.charAt(start))) start--;
        start++;

        int after = idx + 2;
        if (after >= s.length()) return s.substring(0, start).trim();
        if (start == 0) return s.substring(after).trim();
        return (s.substring(0, start) + s.substring(after)).trim();
    }

    public static void process(List<PlayerInfo> entries, long tick) {
        List<String> aliveNames = new ArrayList<>();

        for (PlayerInfo entry : entries) {
            Component disp = entry.getTabListDisplayName();
            if (disp == null) continue;

            String raw = disp.toString();
            if (raw.contains("ms")) {
                int ping = PingUtil.parseTablistPing(raw);
                if (ping >= 0) {
                    ((PlayerListEntryAccessor) entry).setLatency(ping);

                    String legacy = TextUtil.toLegacyString(disp);
                    legacy = removeMs(legacy);

                    entry.setTabListDisplayName(TextUtil.fromLegacy(legacy));
                    disp = entry.getTabListDisplayName();
                    if (disp == null) continue;
                }
            }

            String legacyFull = TextUtil.toLegacyString(disp);
            if (legacyFull.contains("🗡")) {
                String stripped = TextUtil.stripFormatting(legacyFull).trim();
                String[] parts = stripped.split(" ");
                if (parts.length >= 2) aliveNames.add(parts[1]);
            }
        }

        Spectator.updateAlivelist(aliveNames, tick);
    }
}
