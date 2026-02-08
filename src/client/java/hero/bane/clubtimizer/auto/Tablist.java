package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.mixin.accessor.PlayerListEntryAccessor;
import hero.bane.clubtimizer.util.PingUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Tablist {

    public static boolean noBetterPing = true;

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

    public static void process(List<PlayerListEntry> entries, long tick) {
        List<String> daggerNames = new ArrayList<>();

        for (PlayerListEntry entry : entries) {
            Text disp = entry.getDisplayName();
            if (disp == null) continue;

            String raw = disp.getString();
            if (raw.contains("ms")) {
                int ping = PingUtil.parseTablistPing(raw);
                if (ping >= 0) {
                    ((PlayerListEntryAccessor) entry).setLatency(ping);
                    String legacy = TextUtil.toLegacyString(disp);
                    legacy = removeMs(legacy);
                    entry.setDisplayName(TextUtil.fromLegacy(legacy));
                    disp = entry.getDisplayName();
                    if (disp == null) continue;
                }
            }

            String legacyFull = TextUtil.toLegacyString(disp);
            if (legacyFull.contains("🗡")) {
                String stripped = TextUtil.stripFormatting(legacyFull).trim();
                String[] parts = stripped.split(" ");
                if (parts.length >= 2) daggerNames.add(parts[1]);
            }
        }

        Spectator.updateAlivelist(daggerNames, tick);
    }
}
