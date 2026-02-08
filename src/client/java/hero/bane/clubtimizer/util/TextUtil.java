package hero.bane.clubtimizer.util;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.mixin.accessor.InGameHudAccessor;
import hero.bane.clubtimizer.mixin.accessor.PlayerListHudAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.*;

import java.util.*;
import java.util.regex.Pattern;

public class TextUtil {

    private static int rainbowIndex = 0;

    public static final Pattern COLOR_PATTERN =
            Pattern.compile("(?i)§(?:#[0-9A-F]{6}|[0-9A-FK-OR])");

    public static List<String> getOrderedTabList(Minecraft client) {
        var hud = client.gui;
        if (hud == null) return Collections.emptyList();

        List<PlayerInfo> entries =
                ((PlayerListHudAccessor) hud.getTabList()).invokeGetPlayerInfos();

        if (entries.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>(entries.size());
        for (PlayerInfo e : entries) {
            Component d = e.getTabListDisplayName();
            if (d == null) continue;
            String s = toLegacyString(d);
            if (!s.trim().isEmpty()) out.add(s);
        }
        return out;
    }

    public static int parseDuelSize(String text) {
        int num = 0;
        boolean found = false;
        for (int i = 0; i < text.length(); i++) {
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

    public static List<String> getScoreboardLines(Minecraft client) {
        if (client.level == null) return Collections.emptyList();

        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return Collections.emptyList();

        List<PlayerScoreEntry> entries =
                new ArrayList<>(scoreboard.listPlayerScores(objective));

        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

        List<String> lines = new ArrayList<>();
        for (PlayerScoreEntry e : entries) {
            if (e == null) continue;

            String owner = e.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(owner);
            String visible = buildVisible(owner, team);
            if (!visible.trim().isEmpty()) lines.add(visible);
        }
        return lines;
    }

    public static String getActionbarText(Minecraft client) {

        InGameHudAccessor hud = (InGameHudAccessor) client.gui;
        Component msg = hud.getOverlayMessageString();
        if (msg == null || hud.getOverlayMessageTime() <= 0) return null;

        return toLegacyString(msg);
    }

    public static String buildVisible(String owner, PlayerTeam team) {
        if (team == null) return owner;

        String visible = toLegacyString(team.getPlayerPrefix()) +
                owner +
                toLegacyString(team.getPlayerSuffix());

        return visible;
    }

    public static String toLegacyString(Component text) {
        StringBuilder sb = new StringBuilder(64);

        text.visit((style, string) -> {
            if (string.isEmpty()) return Optional.empty();

            TextColor color = style.getColor();
            if (color != null) {
                String name = color.serialize();
                if (name != null && name.startsWith("#")) {
                    sb.append("§#").append(name.substring(1).toUpperCase(Locale.ROOT));
                } else {
                    ChatFormatting f = ChatFormatting.getByName(name);
                    if (f != null && f.isColor()) {
                        sb.append('§').append(f.getChar());
                    } else {
                        sb.append("§#")
                                .append(String.format("%06X", color.getValue()));
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

    public static Component fromLegacy(String legacy) {
        MutableComponent root = Component.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        int i = 0;
        while (i < legacy.length()) {
            char c = legacy.charAt(i);

            if (c == '§') {
                if (!buffer.isEmpty()) {
                    root.append(Component.literal(buffer.toString()).setStyle(currentStyle));
                    buffer.setLength(0);
                }

                if (i + 1 < legacy.length() && legacy.charAt(i + 1) == '#' && i + 7 < legacy.length()) {
                    try {
                        int rgb = Integer.parseInt(legacy.substring(i + 2, i + 8), 16);
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
                    } catch (Exception ignored) {
                    }
                    i += 8;
                    continue;
                }

                if (i + 1 < legacy.length()) {
                    ChatFormatting f = ChatFormatting.getByCode(
                            Character.toLowerCase(legacy.charAt(i + 1)));

                    if (f != null) {
                        if (f.isColor()) {
                            currentStyle = Style.EMPTY.withColor(TextColor.fromLegacyFormat(f));
                        } else if (f == ChatFormatting.BOLD) {
                            currentStyle = currentStyle.withBold(true);
                        } else if (f == ChatFormatting.ITALIC) {
                            currentStyle = currentStyle.withItalic(true);
                        } else if (f == ChatFormatting.UNDERLINE) {
                            currentStyle = currentStyle.withUnderlined(true);
                        } else if (f == ChatFormatting.STRIKETHROUGH) {
                            currentStyle = currentStyle.withStrikethrough(true);
                        } else if (f == ChatFormatting.OBFUSCATED) {
                            currentStyle = currentStyle.withObfuscated(true);
                        } else if (f == ChatFormatting.RESET) {
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
            root.append(Component.literal(buffer.toString()).setStyle(currentStyle));
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

    public static MutableComponent rainbowGradient(String input) {
        MutableComponent out = Component.empty();

        int base = nextRainbowColor();
        float[] hsb = java.awt.Color.RGBtoHSB(
                (base >> 16) & 0xFF,
                (base >> 8) & 0xFF,
                base & 0xFF,
                null
        );

        float hue = hsb[0];
        float step = 5f / 360f;
        boolean started = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (!started && Character.isWhitespace(ch)) {
                out.append(Component.literal(String.valueOf(ch)));
                continue;
            }
            started = true;

            if (!Character.isWhitespace(ch)) {
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.5f, 1f) & 0xFFFFFF;
                out.append(Component.literal(String.valueOf(ch))
                        .withStyle(s -> s.withColor(rgb)));
                hue = (hue + step) % 1f;
            } else {
                out.append(Component.literal(" "));
            }
        }

        return out;
    }

    public static boolean roundEnd(String text, boolean roundCheck) {
        text = text.trim();
        if (text.equals("Draw!")) return true;
        if (text.contains("⚔ Match Complete")) return true;
        if (!roundCheck) return false;
        return text.contains("won the round") && !text.contains("»");
    }
}
