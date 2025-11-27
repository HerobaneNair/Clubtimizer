package hero.bane.util;

import hero.bane.Clubtimizer;
import hero.bane.mixin.accessor.InGameHudAccessor;
import hero.bane.mixin.accessor.PlayerListHudAccessor;
import net.minecraft.client.MinecraftClient;
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
        var hud = client.inGameHud;
        if (hud == null) return Collections.emptyList();

        List<PlayerListEntry> entries = ((PlayerListHudAccessor) hud.getPlayerListHud()).invokeCollectPlayerEntries();
        if (entries.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>(entries.size());
        for (PlayerListEntry e : entries) {
            Text d = e.getDisplayName();
            if (d == null) continue;
            String s = toLegacyString(d);
            if (!s.trim().isEmpty()) out.add(s);
        }
        return out;
    }

    public static int parseDuelSize(String text) {
        int num = 0;
        boolean found = false;
        for (int i = 0, len = text.length(); i < len; i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                found = true;
                num = num * 10 + (c - '0');
            }
        }
        return found ? num : -1;
    }

    public static int countSkulls(List<String> tab) {
        int count = 0;
        for (String line : tab) {
            String stripped = stripFormatting(line).trim();
            if (stripped.isEmpty()) break;
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

    public static String buildVisible(String owner, Team team) {
        if (team == null) return owner;
        StringBuilder visible = new StringBuilder();
        if (team.getPrefix() != null) visible.append(toLegacyString(team.getPrefix()));
        visible.append(owner);
        if (team.getSuffix() != null) visible.append(toLegacyString(team.getSuffix()));
        return visible.toString();
    }

    public static String toLegacyString(Text text) {
        StringBuilder sb = new StringBuilder(64);

        text.visit((style, string) -> {
            if (string.isEmpty()) return Optional.empty();

            var color = style.getColor();
            if (color != null) {
                String name = color.getName();
                if (!name.isEmpty() && name.charAt(0) == '#') {
                    sb.append("§#").append(name.substring(1).toUpperCase(Locale.ROOT));
                } else {
                    Formatting f = Formatting.byName(name);
                    if (f != null && f.isColor()) {
                        sb.append('§').append(f.getCode());
                    } else {
                        sb.append("§#").append(String.format("%06X", color.getRgb()));
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
        for (String p : parts) {
            if (p != null && text.contains(p)) return true;
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

    public static boolean roundEnd(String text, boolean roundCheck) {
        text = text.trim();
        if (text.contains("⚔ Match Complete")) return true;
        if (text.equals("Draw!")) return true;
        if (!roundCheck) return false;
        return (text.contains("won the round") && !text.contains("»"));
    }
}