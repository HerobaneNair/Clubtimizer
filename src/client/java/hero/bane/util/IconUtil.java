package hero.bane.util;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

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

    private static final String[] VALID_SUFFIXES = {
            " H",
            " L"
    };

    public static Text remapIcons(Text original) {
        MutableText out = Text.empty();

        final String full = original.getString();

        original.visit((style, string) -> {
            if (string.isEmpty()) return java.util.Optional.empty();

            int[] cps = string.codePoints().toArray();

            int globalIndex = 0;

            for (int cp : cps) {
                String ch = new String(Character.toChars(cp));

                int idx = full.indexOf(ch, globalIndex);
                if (idx == -1) idx = globalIndex;

                globalIndex = idx + ch.length();

                if (MAP.containsKey(ch)) {
                    boolean shouldReplace = false;

                    for (String suffix : VALID_SUFFIXES) {
                        int index = idx + ch.length() + suffix.length();
                        if (index <= full.length()) {
                            String after = full.substring(idx + ch.length(),
                                    index);
                            if (after.equals(suffix)) {
                                shouldReplace = true;
                                break;
                            }
                        }
                    }

                    if (shouldReplace) {
                        char replacement = MAP.get(ch);
                        out.append(Text.literal(String.valueOf(replacement))
                                .setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
                        continue;
                    }
                }

                out.append(Text.literal(ch).setStyle(style));
            }

            return java.util.Optional.empty();
        }, Style.EMPTY);

        return out;
    }
}