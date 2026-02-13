package hero.bane.clubtimizer.command;

import com.google.gson.*;
import hero.bane.clubtimizer.Clubtimizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.*;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public class ClubtimizerConfig {

    private static final File FILE =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");

    private static final Gson PRETTY = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static String requeue = "";

    private static boolean autoHush_enabled = false;
    private static boolean autoHush_allowSS = true;
    private static boolean autoHush_specChat = true;
    private static String autoHush_joinMessage =
            "Hi, I have chat disabled, don't want to talk, just want to fight";

    private static boolean autoGG_enabled = true;
    private static boolean autoGG_reactionary = true;
    private static boolean autoGG_roundEnabled = false;
    private static String autoGG_message = "gg";
    private static final List<String> autoGG_triggers = new ArrayList<>();

    private static boolean autoCope_enabled = false;
    private static final List<String> autoCope_phrases = new ArrayList<>();

    private static boolean autoResponse_enabled = true;
    private static final Map<Integer, AutoResponseRule> autoResponse_rules =
            new LinkedHashMap<>();

    private static boolean lobby_hidePlayers = false;
    private static boolean lobby_hideChat = false;
    private static boolean lobby_hitboxes = false;
    private static boolean lobby_warning = true;

    public static class AutoResponseRule {
        public List<String> from = new ArrayList<>();
        public List<String> to = new ArrayList<>();
    }

    public static void load() {

        boolean needsRewrite = false;

        if (!FILE.exists()) {
            say("File missing so resetting the whole thing", 0xFF0000);
            applyHardDefaults();
            save();
            return;
        }

        JsonObject root;

        try (FileReader reader = new FileReader(FILE)) {

            JsonElement parsed = JsonParser.parseReader(reader);

            if (!parsed.isJsonObject()) {
                say("Root malformed so resetting the whole thing", 0xFF0000);
                applyHardDefaults();
                save();
                return;
            }

            root = parsed.getAsJsonObject();

        } catch (Exception e) {
            say("JSON completely unreadable so resetting the whole thing", 0xFF0000);
            applyHardDefaults();
            save();
            return;
        }

        if (!root.has("requeue")) {
            say("Requeue missing so fixing to default of 14", 0xFF0000);
            requeue = "14";
            needsRewrite = true;
        } else {
            JsonElement el = root.get("requeue");
            if (el.isJsonPrimitive()) {
                String val = el.getAsString();
                if (isRequeueOrderOk(val)) {
                    requeue = val;
                } else {
                    say("Requeue malformed. Reverting back to " + requeue, 0xFF0000);
                }
            } else {
                say("Requeue wrong type. Reverting back to " + requeue, 0xFF0000);
            }
        }

        if (!root.has("autoHush") || !root.get("autoHush").isJsonObject()) {
            say("AutoHush tree missing so restoring defaults", 0xFF0000);
            autoHush_enabled = false;
            autoHush_allowSS = true;
            autoHush_specChat = true;
            autoHush_joinMessage =
                    "Hi, I have chat disabled, don't want to talk, just want to fight";
            needsRewrite = true;
        } else {
            JsonObject ah = root.getAsJsonObject("autoHush");

            if (!ah.has("enabled")) {
                autoHush_enabled = false;
                needsRewrite = true;
            } else {
                JsonElement el = ah.get("enabled");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoHush_enabled = el.getAsBoolean();
                else
                    say("AutoHush.enabled malformed. Reverting back to " + autoHush_enabled, 0xFF0000);
            }

            if (!ah.has("allowSS")) {
                autoHush_allowSS = true;
                needsRewrite = true;
            } else {
                JsonElement el = ah.get("allowSS");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoHush_allowSS = el.getAsBoolean();
                else
                    say("AutoHush.allowSS malformed. Reverting back to " + autoHush_allowSS, 0xFF0000);
            }

            if (!ah.has("specChat")) {
                autoHush_specChat = true;
                needsRewrite = true;
            } else {
                JsonElement el = ah.get("specChat");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoHush_specChat = el.getAsBoolean();
                else
                    say("AutoHush.specChat malformed. Reverting back to " + autoHush_specChat, 0xFF0000);
            }

            if (!ah.has("joinMessage")) {
                autoHush_joinMessage =
                        "Hi, I have chat disabled, don't want to talk, just want to fight";
                needsRewrite = true;
            } else if (ah.get("joinMessage").isJsonPrimitive()) {
                autoHush_joinMessage = ah.get("joinMessage").getAsString();
            }
        }

        if (!root.has("autoGG") || !root.get("autoGG").isJsonObject()) {
            say("AutoGG tree missing so restoring defaults", 0xFF0000);
            autoGG_enabled = true;
            autoGG_reactionary = true;
            autoGG_roundEnabled = false;
            autoGG_message = "gg";
            autoGG_triggers.clear();
            autoGG_triggers.addAll(List.of("g","gg","ggs","ggg","gs","gggg","ggwp","wp"));
            needsRewrite = true;
        } else {
            JsonObject ag = root.getAsJsonObject("autoGG");

            if (!ag.has("enabled")) {
                autoGG_enabled = true;
                needsRewrite = true;
            } else {
                JsonElement el = ag.get("enabled");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoGG_enabled = el.getAsBoolean();
                else
                    say("AutoGG.enabled malformed. Reverting back to " + autoGG_enabled, 0xFF0000);
            }

            if (!ag.has("reactionary")) {
                autoGG_reactionary = true;
                needsRewrite = true;
            } else {
                JsonElement el = ag.get("reactionary");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoGG_reactionary = el.getAsBoolean();
                else
                    say("AutoGG.reactionary malformed. Reverting back to " + autoGG_reactionary, 0xFF0000);
            }

            if (!ag.has("roundEnabled")) {
                autoGG_roundEnabled = false;
                needsRewrite = true;
            } else {
                JsonElement el = ag.get("roundEnabled");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoGG_roundEnabled = el.getAsBoolean();
                else
                    say("AutoGG.roundEnabled malformed. Reverting back to " + autoGG_roundEnabled, 0xFF0000);
            }

            if (!ag.has("message")) {
                autoGG_message = "gg";
                needsRewrite = true;
            } else if (ag.get("message").isJsonPrimitive()) {
                autoGG_message = ag.get("message").getAsString();
            }

            if (!ag.has("triggers") || !ag.get("triggers").isJsonArray()) {
                autoGG_triggers.clear();
                autoGG_triggers.addAll(List.of("g","gg","ggs","ggg","gs","gggg","ggwp","wp"));
                needsRewrite = true;
            } else {
                autoGG_triggers.clear();
                for (JsonElement e : ag.getAsJsonArray("triggers"))
                    if (e.isJsonPrimitive())
                        autoGG_triggers.add(e.getAsString());
            }
        }

        if (!root.has("autoCope") || !root.get("autoCope").isJsonObject()) {
            say("AutoCope tree missing so restoring defaults", 0xFF0000);
            autoCope_enabled = false;
            autoCope_phrases.clear();
            autoCope_phrases.addAll(List.of(
                    "I ghosted",
                    "You're so lucky",
                    "This wouldn't have happened if HerobaneNair didn't hack my computer"
            ));
            needsRewrite = true;
        } else {
            JsonObject ac = root.getAsJsonObject("autoCope");

            if (!ac.has("enabled")) {
                autoCope_enabled = false;
                needsRewrite = true;
            } else {
                JsonElement el = ac.get("enabled");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoCope_enabled = el.getAsBoolean();
                else
                    say("AutoCope.enabled malformed. Reverting back to " + autoCope_enabled, 0xFF0000);
            }

            if (!ac.has("phrases") || !ac.get("phrases").isJsonArray()) {
                autoCope_phrases.clear();
                autoCope_phrases.addAll(List.of(
                        "I ghosted",
                        "You're so lucky",
                        "This wouldn't have happened if HerobaneNair didn't hack my computer"
                ));
                needsRewrite = true;
            } else {
                autoCope_phrases.clear();
                for (JsonElement e : ac.getAsJsonArray("phrases"))
                    if (e.isJsonPrimitive())
                        autoCope_phrases.add(e.getAsString());
            }
        }

        if (!root.has("autoResponse") || !root.get("autoResponse").isJsonObject()) {
            say("AutoResponse tree missing so restoring defaults", 0xFF0000);
            autoResponse_enabled = true;
            autoResponse_rules.clear();
            needsRewrite = true;
        } else {
            JsonObject ar = root.getAsJsonObject("autoResponse");

            if (!ar.has("enabled")) {
                autoResponse_enabled = true;
                needsRewrite = true;
            } else {
                JsonElement el = ar.get("enabled");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    autoResponse_enabled = el.getAsBoolean();
                else
                    say("AutoResponse.enabled malformed. Reverting back to " + autoResponse_enabled, 0xFF0000);
            }

            if (!ar.has("rules") || !ar.get("rules").isJsonObject()) {
                autoResponse_rules.clear();
                needsRewrite = true;
            } else {
                Map<Integer, AutoResponseRule> tmp = new LinkedHashMap<>();
                JsonObject rulesObj = ar.getAsJsonObject("rules");

                for (Map.Entry<String, JsonElement> entry : rulesObj.entrySet()) {
                    try {
                        int key = Integer.parseInt(entry.getKey());
                        if (!entry.getValue().isJsonObject()) continue;

                        JsonObject ruleObj = entry.getValue().getAsJsonObject();
                        AutoResponseRule rule = new AutoResponseRule();

                        if (ruleObj.has("from") && ruleObj.get("from").isJsonArray())
                            for (JsonElement f : ruleObj.getAsJsonArray("from"))
                                if (f.isJsonPrimitive())
                                    rule.from.add(f.getAsString());

                        if (ruleObj.has("to") && ruleObj.get("to").isJsonArray())
                            for (JsonElement t : ruleObj.getAsJsonArray("to"))
                                if (t.isJsonPrimitive())
                                    rule.to.add(t.getAsString());

                        if (!rule.from.isEmpty() && !rule.to.isEmpty())
                            tmp.put(key, rule);

                    } catch (Exception ignored) {}
                }

                autoResponse_rules.clear();
                autoResponse_rules.putAll(tmp);
            }
        }

        if (!root.has("lobby") || !root.get("lobby").isJsonObject()) {
            say("Lobby tree missing so restoring defaults", 0xFF0000);
            lobby_hidePlayers = false;
            lobby_hideChat = false;
            lobby_hitboxes = false;
            lobby_warning = true;
            needsRewrite = true;
        } else {
            JsonObject lb = root.getAsJsonObject("lobby");

            if (!lb.has("hidePlayers")) {
                lobby_hidePlayers = false;
                needsRewrite = true;
            } else {
                JsonElement el = lb.get("hidePlayers");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    lobby_hidePlayers = el.getAsBoolean();
                else
                    say("Lobby.hidePlayers malformed. Reverting back to " + lobby_hidePlayers, 0xFF0000);
            }

            if (!lb.has("hideChat")) {
                lobby_hideChat = false;
                needsRewrite = true;
            } else {
                JsonElement el = lb.get("hideChat");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    lobby_hideChat = el.getAsBoolean();
                else
                    say("Lobby.hideChat malformed. Reverting back to " + lobby_hideChat, 0xFF0000);
            }

            if (!lb.has("hitboxes")) {
                lobby_hitboxes = false;
                needsRewrite = true;
            } else {
                JsonElement el = lb.get("hitboxes");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    lobby_hitboxes = el.getAsBoolean();
                else
                    say("Lobby.hitboxes malformed. Reverting back to " + lobby_hitboxes, 0xFF0000);
            }

            if (!lb.has("warning")) {
                lobby_warning = true;
                needsRewrite = true;
            } else {
                JsonElement el = lb.get("warning");
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                    lobby_warning = el.getAsBoolean();
                else
                    say("Lobby.warning malformed. Reverting back to " + lobby_warning, 0xFF0000);
            }
        }

        if (needsRewrite) {
            save();
        }
//        say("Loaded Config", 0x55FFFF);
    }

    public static boolean save() {
        JsonObject root = new JsonObject();

        root.addProperty("requeue", requeue);

        JsonObject autoHush = new JsonObject();
        autoHush.addProperty("enabled", autoHush_enabled);
        autoHush.addProperty("allowSS", autoHush_allowSS);
        autoHush.addProperty("specChat", autoHush_specChat);
        autoHush.addProperty("joinMessage", autoHush_joinMessage);
        root.add("autoHush", autoHush);

        JsonObject autoGG = new JsonObject();
        autoGG.addProperty("enabled", autoGG_enabled);
        autoGG.addProperty("reactionary", autoGG_reactionary);
        autoGG.addProperty("roundEnabled", autoGG_roundEnabled);
        autoGG.addProperty("message", autoGG_message);
        JsonArray triggers = new JsonArray();
        for (String t : autoGG_triggers) triggers.add(t);
        autoGG.add("triggers", triggers);
        root.add("autoGG", autoGG);

        JsonObject autoCope = new JsonObject();
        autoCope.addProperty("enabled", autoCope_enabled);
        JsonArray phrases = new JsonArray();
        for (String p : autoCope_phrases) phrases.add(p);
        autoCope.add("phrases", phrases);
        root.add("autoCope", autoCope);

        JsonObject autoResponse = new JsonObject();
        autoResponse.addProperty("enabled", autoResponse_enabled);
        JsonObject rules = new JsonObject();
        for (Map.Entry<Integer, AutoResponseRule> e : autoResponse_rules.entrySet()) {

            AutoResponseRule rule = e.getValue();

            if (rule.to.size() > 1) {
                rule.to.removeIf(s -> s.equals("."));
            }

            JsonObject ruleObj = new JsonObject();
            JsonArray from = new JsonArray();
            JsonArray to = new JsonArray();

            for (String s : rule.from) from.add(s);
            for (String s : rule.to) to.add(s);

            ruleObj.add("from", from);
            ruleObj.add("to", to);
            rules.add(String.valueOf(e.getKey()), ruleObj);
        }
        autoResponse.add("rules", rules);
        root.add("autoResponse", autoResponse);

        JsonObject lobby = new JsonObject();
        lobby.addProperty("hidePlayers", lobby_hidePlayers);
        lobby.addProperty("hideChat", lobby_hideChat);
        lobby.addProperty("hitboxes", lobby_hitboxes);
        lobby.addProperty("warning", lobby_warning);
        root.add("lobby", lobby);

        String json = PRETTY.toJson(root);

        try (FileWriter writer = new FileWriter(FILE, false)) {

            writer.write(json);
            writer.flush();

        } catch (Exception e) {
            say("Failed Saving Config", 0xFF5555);
            Clubtimizer.LOGGER.error("Save error", e);
            return false;
        }
        return true;
    }

    private static void applyHardDefaults() {

        requeue = "14";

        autoHush_enabled = false;
        autoHush_allowSS = true;
        autoHush_specChat = true;
        autoHush_joinMessage =
                "Hi, I have chat disabled, don't want to talk, just want to fight";

        autoGG_enabled = true;
        autoGG_reactionary = true;
        autoGG_roundEnabled = true;
        autoGG_message = "gg";

        autoGG_triggers.clear();
        autoGG_triggers.addAll(List.of(
                "g","gg","ggs","ggg","gs","gggg","ggwp","wp"
        ));

        autoCope_enabled = false;

        autoCope_phrases.clear();
        autoCope_phrases.addAll(List.of(
                "I ghosted",
                "You're so lucky",
                "This wouldn't have happened if HerobaneNair didn't hack my computer"
        ));

        autoResponse_enabled = true;

        autoResponse_rules.clear();
        AutoResponseRule ghostRule = new AutoResponseRule();
        ghostRule.from.addAll(List.of("ghosted","ghost","gohst"));
        ghostRule.to.addAll(List.of(
                "auto ghost moment",
                "ghosts aren't real",
                "ghosting in the big 2026"
        ));
        autoResponse_rules.put(1, ghostRule);
        AutoResponseRule luckyRule = new AutoResponseRule();
        luckyRule.from.addAll(List.of("lucky","luck","lcuk"));
        luckyRule.to.addAll(List.of(
                "0 percent luck 100 percent skill 200 percent concentrated power of will",
                "lucky to face someone as stinky as you punk"
        ));
        autoResponse_rules.put(2, luckyRule);

        lobby_hidePlayers = false;
        lobby_hideChat = false;
        lobby_hitboxes = false;
        lobby_warning = true;
    }

    public static class LobbyConfig {
        public boolean hidePlayers;
        public boolean hideChat;
        public boolean hitboxes;
        public boolean warning;
    }

    public static LobbyConfig getLobby() {
        LobbyConfig c = new LobbyConfig();
        c.hidePlayers = lobby_hidePlayers;
        c.hideChat = lobby_hideChat;
        c.hitboxes = lobby_hitboxes;
        c.warning = lobby_warning;
        return c;
    }

    public static void setLobbyHidePlayers(boolean b) { lobby_hidePlayers = b; save(); }
    public static void setLobbyHideChat(boolean b) { lobby_hideChat = b; save(); }
    public static void setLobbyHitboxes(boolean b) { lobby_hitboxes = b; save(); }
    public static void setLobbyWarning(boolean b) { lobby_warning = b; save(); }

    public static class AutoHushConfig {
        public boolean enabled;
        public boolean allowSS;
        public boolean specChat;
        public String joinMessage;
    }

    public static AutoHushConfig getAutoHush() {
        AutoHushConfig c = new AutoHushConfig();
        c.enabled = autoHush_enabled;
        c.allowSS = autoHush_allowSS;
        c.specChat = autoHush_specChat;
        c.joinMessage = autoHush_joinMessage;
        return c;
    }

    public static void setAutoHushEnabled(boolean b) { autoHush_enabled = b; save(); }
    public static void setAutoHushSS(boolean b) { autoHush_allowSS = b; save(); }
    public static void setAutoHushSpecChat(boolean b) { autoHush_specChat = b; save(); }
    public static void setAutoHushMessage(String s) { autoHush_joinMessage = s; save(); }

    public static class AutoGGConfig {
        public boolean enabled;
        public boolean reactionary;
        public boolean roundEnabled;
        public String message;
        public List<String> triggers;
    }

    public static AutoGGConfig getAutoGG() {
        AutoGGConfig c = new AutoGGConfig();
        c.enabled = autoGG_enabled;
        c.reactionary = autoGG_reactionary;
        c.roundEnabled = autoGG_roundEnabled;
        c.message = autoGG_message;
        c.triggers = autoGG_triggers;
        return c;
    }

    public static void setAutoGGEnabled(boolean b) { autoGG_enabled = b; save(); }
    public static void setAutoGGReactionary(boolean b) { autoGG_reactionary = b; save(); }
    public static void setAutoGGRound(boolean b) { autoGG_roundEnabled = b; save(); }
    public static void setAutoGGMessage(String s) { autoGG_message = s; save(); }
    public static void addAutoGGTrigger(String s) { autoGG_triggers.add(s.toLowerCase()); save(); }
    public static void removeAutoGGTrigger(String s) { autoGG_triggers.remove(s.toLowerCase()); save(); }

    public static class AutoCopeConfig {
        public boolean enabled;
        public List<String> phrases;
    }

    public static AutoCopeConfig getAutoCope() {
        AutoCopeConfig c = new AutoCopeConfig();
        c.enabled = autoCope_enabled;
        c.phrases = autoCope_phrases;
        return c;
    }

    public static void setAutoCopeEnabled(boolean b) { autoCope_enabled = b; save(); }
    public static void addAutoCopePhrase(String s) { autoCope_phrases.add(s); save(); }
    public static void removeAutoCopePhrase(String s) { autoCope_phrases.remove(s); save(); }

    public static class AutoResponseConfig {
        public boolean enabled;
        public Map<Integer, AutoResponseRule> rules;
    }

    public static AutoResponseConfig getAutoResponse() {
        AutoResponseConfig c = new AutoResponseConfig();
        c.enabled = autoResponse_enabled;
        c.rules = autoResponse_rules;
        return c;
    }

    public static void setAutoResponseEnabled(boolean b) { autoResponse_enabled = b; save(); }

    public static void addAutoResponseRule(String fromRaw, String toRaw) {
        int index = autoResponse_rules.keySet().stream().mapToInt(i -> i).max().orElse(0) + 1;
        AutoResponseRule rule = new AutoResponseRule();
        for (String s : fromRaw.split("\\|")) {
            String v = s.trim().toLowerCase();
            if (!v.isEmpty()) rule.from.add(v);
        }
        for (String s : toRaw.split("\\|")) {
            String v = s.trim();
            if (!v.isEmpty()) rule.to.add(v);
        }
        if (!rule.from.isEmpty() && !rule.to.isEmpty())
            autoResponse_rules.put(index, rule);
        save();
    }

    public static String getRequeueOrder() {
        return requeue == null ? "" : requeue;
    }

    public static void setRequeueOrder(String sequence) {
        if (sequence == null) sequence = "";
        requeue = sequence;
        save();
    }

    private static boolean isRequeueOrderOk(String input) {

        if (input == null) return false;

        int len = input.length();
        if (len == 0 || len > 8) return false;

        boolean[] seen = new boolean[8];

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);

            if (c < '1' || c > '8') return false;

            int idx = c - '1';
            if (seen[idx]) return false;

            seen[idx] = true;
        }

        return true;
    }

    public static void addAutoResponseValue(int index, String value, boolean toSide) {
        AutoResponseRule rule =
                autoResponse_rules.computeIfAbsent(index, i -> new AutoResponseRule());
        if (toSide) rule.to.add(value);
        else rule.from.add(value);
        save();
    }

    public static void removeAutoResponseValue(int index, String value) {
        AutoResponseRule rule = autoResponse_rules.get(index);
        if (rule == null) return;
        rule.from.remove(value);
        rule.to.remove(value);
        if (rule.from.isEmpty() || rule.to.isEmpty())
            autoResponse_rules.remove(index);
        save();
    }

    public static void deleteAutoResponseRule(int index) {
        autoResponse_rules.remove(index);
        save();
    }
}
