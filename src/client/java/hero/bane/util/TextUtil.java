package hero.bane.util;

import hero.bane.Clubtimizer;
import hero.bane.mixin.accessor.InGameHudAccessor;
import hero.bane.mixin.accessor.PlayerListHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.regex.Pattern;

public class TextUtil {

    private static int rainbowIndex = 0;

    public static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§(?:#[0-9A-F]{6}|[0-9A-FK-OR])");

    public static List<String> getOrderedTabList(MinecraftClient client) {
        if (client.inGameHud == null) return Collections.emptyList();
        PlayerListHud hud = client.inGameHud.getPlayerListHud();
        List<PlayerListEntry> entries = ((PlayerListHudAccessor) hud).invokeCollectPlayerEntries();
        int size = entries.size();
        if (size == 0) return Collections.emptyList();

        List<String> lines = new ArrayList<>(size);

        for (PlayerListEntry e : entries) {
            if (e == null || e.getProfile() == null) continue;
            Text display = e.getDisplayName();
            if (display == null) continue;

            String line = toLegacyString(display);
            if (isBlank(line)) continue;

            lines.add(line);
        }

        return lines;
    }

    private static boolean isBlank(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int parseDuelSize(String text) {
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static int countSkulls(List<String> tab) {
        int count = 0;
        for (String line : tab) {
            String stripped = stripFormatting(line);
            if (stripped.trim().isEmpty()) break;
            if (stripped.contains("☠")) count++;
        }
        Clubtimizer.temp1 = count;
        return count;
    }

    public static List<String> getScoreboardLines(MinecraftClient client) {
        assert client.world != null;
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return Collections.emptyList();

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());

        List<String> lines = new ArrayList<>();
        for (ScoreboardEntry e : entries) {
            if (e == null || e.hidden()) continue;
            String owner = e.owner();
            Team team = scoreboard.getScoreHolderTeam(owner);
            String visible = buildVisible(owner, team);
            if (!visible.trim().isEmpty()) lines.add(visible);
        }
        return lines;
    }

    public static String getActionbarText(MinecraftClient client) {
        if (client.inGameHud == null) return null;
        InGameHudAccessor hud = (InGameHudAccessor) client.inGameHud;
        Text msg = hud.getOverlayMessage();
        if (msg == null || hud.getOverlayRemaining() <= 0) return null;
        return toLegacyString(msg);
    }

    public static int getDynamicDelay(MinecraftClient client, int tickBuffer) {
        int ping = PingUtil.parsePingFromScoreboard(client);
        ping = ping == 0 ? 25 : ping;
        int calculated = (int) Math.ceil(ping / 25.0) + tickBuffer;
        if (calculated < 1) calculated = 1;
        return calculated;
    }

    public static String buildVisible(String owner, Team team) {
        if (team == null) return owner;
        StringBuilder visible = new StringBuilder();
        if (team.getPrefix() != null) visible.append(toLegacyString(team.getPrefix()));
        visible.append(owner);
        if (team.getSuffix() != null) visible.append(toLegacyString(team.getSuffix()));
        return visible.toString();
    }

    public static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder();

        text.visit((style, string) -> {
            if (string.isEmpty()) return Optional.empty();

            TextColor color = style.getColor();
            if (color != null) {
                String name = color.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith("#")) {
                    sb.append("§#").append(name.substring(1).toUpperCase(Locale.ROOT));
                } else {
                    Formatting formatting = Formatting.byName(name);
                    if (formatting != null && formatting.isColor()) {
                        sb.append("§").append(formatting.getCode());
                    } else {
                        sb.append("§#").append(String.format("%06X", color.getRgb())); //%06X is the next 6 colors
                    }
                }
            }

            if (style.isBold()) sb.append("§l");
            if (style.isItalic()) sb.append("§o");
            if (style.isUnderlined()) sb.append("§n");
            if (style.isStrikethrough()) sb.append("§m");
            if (style.isObfuscated()) sb.append("§k");

            sb.append(string);
            return Optional.empty();
        }, Style.EMPTY);

        return sb.toString();
    }

    public static Text fromLegacy(String legacy) {
        MutableText root = Text.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        int i = 0;
        while (i < legacy.length()) {
            char c = legacy.charAt(i);

            if (c == '§') {
                if (!buffer.isEmpty()) {
                    root.append(Text.literal(buffer.toString()).setStyle(currentStyle));
                    buffer.setLength(0);
                }
                if (i + 1 < legacy.length() && legacy.charAt(i + 1) == '#' && i + 7 < legacy.length()) {
                    String hex = legacy.substring(i + 2, i + 8);
                    try {
                        int color = Integer.parseInt(hex, 16);
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(color));
                    } catch (Exception ignored) {
                    }
                    i += 8;
                    continue;
                }
                if (i + 1 < legacy.length()) {
                    char code = Character.toLowerCase(legacy.charAt(i + 1));
                    Formatting formatting = Formatting.byCode(code);

                    if (formatting != null) {
                        if (formatting.isColor()) {
                            currentStyle = Style.EMPTY.withColor(TextColor.fromFormatting(formatting));
                        } else if (formatting == Formatting.BOLD) {
                            currentStyle = currentStyle.withBold(true);
                        } else if (formatting == Formatting.ITALIC) {
                            currentStyle = currentStyle.withItalic(true);
                        } else if (formatting == Formatting.UNDERLINE) {
                            currentStyle = currentStyle.withUnderline(true);
                        } else if (formatting == Formatting.STRIKETHROUGH) {
                            currentStyle = currentStyle.withStrikethrough(true);
                        } else if (formatting == Formatting.OBFUSCATED) {
                            currentStyle = currentStyle.withObfuscated(true);
                        } else if (formatting == Formatting.RESET) {
                            currentStyle = Style.EMPTY;
                        }
                        i += 2;
                        continue;
                    }
                }
            }

            buffer.append(c);
            i++;
        }
        if (!buffer.isEmpty()) {
            root.append(Text.literal(buffer.toString()).setStyle(currentStyle));
        }

        return root;
    }

    public static String stripFormatting(String input) {
        return COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static boolean containsAny(String text, String... parts) {
        int tLen = text.length();
        for (String p : parts) {
            if (p == null) continue;
            int pLen = p.length();
            if (pLen == 0 || pLen > tLen) continue;
            if (fastContains(text, p)) return true;
        }
        return false;
    }

    public static int nextRainbowColor() {
        float hue = (rainbowIndex % 360) / 360f;
        rainbowIndex = (rainbowIndex + 31) % 360;
        return java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF;
    }

    public static MutableText rainbowGradient(String input) {
        MutableText out = Text.empty();
        int base = nextRainbowColor();
        float[] hsb = java.awt.Color.RGBtoHSB((base >> 16) & 0xFF, (base >> 8) & 0xFF, base & 0xFF, null);
        float hue = hsb[0];
        float step = 5f / 360f;
        boolean started = false;
        for (int i = 0, len = input.length(); i < len; i++) {
            char ch = input.charAt(i);
            if (!started) {
                if (Character.isWhitespace(ch)) {
                    out.append(Text.literal(Character.toString(ch)));
                    continue;
                }
                started = true;
            }
            if (!Character.isWhitespace(ch)) {
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.5f, 1f) & 0xFFFFFF;
                out.append(Text.literal(Character.toString(ch)).styled(s -> s.withColor(rgb)));
                hue += step;
                if (hue > 1f) hue -= 1f;
            } else {
                out.append(Text.literal(" "));
            }
        }
        return out;
    }

    public static boolean fastContains(String text, String pattern) {
        int tLen = text.length();
        int pLen = pattern.length();

        if (pLen == 0) return true;
        if (pLen > tLen) return false;

        if (pLen <= 8) return tinySearch(text, pattern, tLen, pLen);

        return bmhSearch(text, pattern, tLen, pLen);
    }

    private static boolean tinySearch(String t, String p, int tLen, int pLen) {
        char first = p.charAt(0);
        int max = tLen - pLen;

        for (int i = 0; i <= max; i++) {
            if (t.charAt(i) == first) {
                int j = 1;
                while (j < pLen && t.charAt(i + j) == p.charAt(j)) {
                    j++;
                }
                if (j == pLen) return true;
            }
        }
        return false;
    }

    private static boolean bmhSearch(String text, String pattern, int tLen, int pLen) {
        int[] skip = buildSkipArray(pattern, pLen);

        int i = 0;
        int last = pLen - 1;

        while (i <= tLen - pLen) {

            int j = last;
            while (text.charAt(i + j) == pattern.charAt(j)) {
                if (j == 0) return true;
                j--;
            }

            i += skip[text.charAt(i + last) & 0xFF];
        }
        return false;
    }

    private static int[] buildSkipArray(String pattern, int pLen) {
        int[] skip = new int[256];
        int last = pLen - 1;

        Arrays.fill(skip, pLen);

        for (int i = 0; i < last; i++) {
            skip[pattern.charAt(i) & 0xFF] = last - i;
        }
        return skip;
    }
}