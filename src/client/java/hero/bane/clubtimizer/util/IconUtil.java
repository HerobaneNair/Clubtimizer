package hero.bane.clubtimizer.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;

public class IconUtil {

    private static final Map<String, Character> MAP = Map.of(
            "❤", '\uE060',
            "✦", '\uE061',
            "🪓", '\uE062',
            "🔨", '\uE063',
            "☠", '\uE064',
            "⚗", '\uE065',
            "⛨", '\uE066',
            "🗡", '\uE067'
    );

    private static final String[] VALID_SUFFIXES = {" (R) H"," (R) L"," H", " L"};

    public static Component remapIcons(Component original) {
        MutableComponent out = Component.empty();
        final String full = original.getString();
        final int fullLen = full.length();
        final char[] buffer = new char[2];

        original.visit((style, string) -> {
            if (string.isEmpty()) return java.util.Optional.empty();

            int[] cps = string.codePoints().toArray();
            int pos = 0;

            for (int cp : cps) {
                int len = Character.toChars(cp, buffer, 0);
                String ch = new String(buffer, 0, len);

                int idx = full.indexOf(ch, pos);
                if (idx < 0) idx = pos;
                pos = idx + len;

                Character repl = MAP.get(ch);
                if (repl != null) {
                    boolean ok = false;
                    int base = idx + len;

                    for (String suf : VALID_SUFFIXES) {
                        int end = base + suf.length();
                        if (end <= fullLen && full.regionMatches(base, suf, 0, suf.length())) {
                            ok = true;
                            break;
                        }
                    }

                    if (ok) {
                        out.append(
                                Component.literal(String.valueOf(repl))
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))
                        );
                        continue;
                    }
                }

                out.append(Component.literal(ch).setStyle(style));
            }

            return java.util.Optional.empty();
        }, Style.EMPTY);

        return out;
    }
}
