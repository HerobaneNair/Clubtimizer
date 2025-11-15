package hero.bane.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingUtil {
    private static final Pattern PING_PATTERN = Pattern.compile("(\\d{1,4})\\s*ms");

    public static int parsePing(String text) {
        if (text == null) return -1;
        Matcher matcher = PING_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    public static int parsePingFromScoreboard(MinecraftClient client) {
        List<String> lines = TextUtil.getScoreboardLines(client);
        for (String line : lines) {
            if (line.contains("ms")) {
                int start = line.indexOf('(');
                int end = line.indexOf("ms");
                if (start != -1 && end != -1 && end > start) {
                    try {
                        String numStr = line.substring(start + 1, end).trim();
                        return Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }

    public static int getPingColor(int ping) {
        if (ping <= 0)
            return 0x0083BF;
        if (ping <= 60)
            return interpolate(0x0083BF, 0x00BF00, computeOffset(0, 60, ping));
        if (ping <= 120)
            return interpolate(0x00BF00, 0xBFBF00, computeOffset(60, 120, ping));
        if (ping <= 150)
            return interpolate(0xBFBF00, 0xBF0000, computeOffset(120, 150, ping));
        return 0xBF0000;
    }

    private static float computeOffset(int start, int end, int value) {
        float offset = (value - start) / (float) (end - start);
        return MathHelper.clamp(offset, 0.0F, 1.0F);
    }

    private static int interpolate(int startColor, int endColor, float offset) {
        int sr = (startColor >> 16) & 0xFF;
        int sg = (startColor >> 8) & 0xFF;
        int sb = startColor & 0xFF;

        int er = (endColor >> 16) & 0xFF;
        int eg = (endColor >> 8) & 0xFF;
        int eb = endColor & 0xFF;

        int r = (int) (sr + (er - sr) * offset);
        int g = (int) (sg + (eg - sg) * offset);
        int b = (int) (sb + (eb - sb) * offset);

        return (r << 16) | (g << 8) | b;
    }
}