package hero.bane.clubtimizer.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hero.bane.clubtimizer.Clubtimizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public final class ClubtimizerConfig {

    private static final File file =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static Config data = new Config();

    public static final class Config {
        String requeue = "14";

        AutoHush autoHush = new AutoHush();
        AutoGG autoGG = new AutoGG();
        AutoCope autoCope = new AutoCope();
        AutoResponse autoResponse = new AutoResponse();
        Lobby lobby = new Lobby();
    }

    public static final class AutoHush {
        public boolean enabled = false;
        public boolean allowSS = true;
        public boolean specChat = true;
        public String joinMessage =
                "Hi, I have chat disabled, don't want to talk, just want to fight";
    }

    public static final class AutoGG {
        public boolean enabled = true;
        public boolean reactionary = true;
        public boolean roundEnabled = true;
        public String message = "gg";
        public List<String> triggers = new ArrayList<>(List.of(
                "g","gg","ggs","ggg","gs","gggg","ggwp","wp"
        ));
    }

    public static final class AutoCope {
        public boolean enabled = false;
        public List<String> phrases = new ArrayList<>(List.of(
                "I ghosted",
                "You're so lucky",
                "This wouldn't have happened if HerobaneNair didn't hack my computer"
        ));
    }

    public static final class AutoResponse {
        public boolean enabled = true;
        public Map<Integer, AutoResponseRule> rules = new LinkedHashMap<>();
    }

    public static final class Lobby {
        public boolean hidePlayers = false;
        public boolean hideChat = false;
        public boolean hidePublicParties = false;
        public boolean hitboxes = false;
        public boolean warning = true;
    }

    public static class AutoResponseRule {
        public List<String> from = new ArrayList<>();
        public List<String> to = new ArrayList<>();
    }

    public static void load() {

        if (!file.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {

            Config loaded = gson.fromJson(reader, Config.class);

            if (loaded == null) {
                say("Config malformed. Resetting.", 0xFF0000);
                save();
                return;
            }

            data = loaded;

            validateAndMigrate();

        } catch (Exception e) {
            say("Config unreadable. Resetting.", 0xFF0000);
            data = new Config();
            save();
        }
    }

    public static boolean save() {

        try (FileWriter writer = new FileWriter(file, false)) {

            gson.toJson(data, writer);
            writer.flush();
            return true;

        } catch (Exception e) {
            say("Failed Saving Config", 0xFF5555);
            Clubtimizer.LOGGER.error("Save error", e);
            return false;
        }
    }

    private static void validateAndMigrate() {

        boolean rewrite = false;

        if (!isRequeueOrderOk(data.requeue)) {
            String previous = data.requeue;
            data.requeue = "14";
            say("Requeue malformed (" + previous + "). Resetting to 14.", 0xFF0000);
            rewrite = true;
        }

        if (data.autoGG.triggers == null || data.autoGG.triggers.isEmpty()) {
            say("AutoGG triggers missing or empty. Restoring defaults.", 0xFF0000);

            data.autoGG.triggers = new ArrayList<>(List.of(
                    "g","gg","ggs","ggg","gs","gggg","ggwp","wp"
            ));
            rewrite = true;
        }

        if (data.autoCope.phrases == null) {
            say("AutoCope phrases missing. Restoring empty list.", 0xFF0000);

            data.autoCope.phrases = new ArrayList<>();
            rewrite = true;
        }

        if (data.autoResponse.rules == null) {
            say("AutoResponse rules missing. Restoring empty rule set.", 0xFF0000);

            data.autoResponse.rules = new LinkedHashMap<>();
            rewrite = true;
        }

        if (rewrite) save();
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

    public static String getRequeueOrder() {
        return data.requeue == null ? "" : data.requeue;
    }

    public static void setRequeueOrder(String sequence) {
        data.requeue = sequence == null ? "" : sequence;
        save();
    }

    public static Lobby getLobby() { return data.lobby; }

    public static void setLobbyHidePlayers(boolean b) { data.lobby.hidePlayers = b; save(); }
    public static void setLobbyHideChat(boolean b) { data.lobby.hideChat = b; save(); }
    public static void setLobbyHidePublicParties(boolean b) { data.lobby.hidePublicParties = b; save(); }
    public static void setLobbyHitboxes(boolean b) { data.lobby.hitboxes = b; save(); }
    public static void setLobbyWarning(boolean b) { data.lobby.warning = b; save(); }

    public static AutoHush getAutoHush() { return data.autoHush; }

    public static void setAutoHushEnabled(boolean b) { data.autoHush.enabled = b; save(); }
    public static void setAutoHushSS(boolean b) { data.autoHush.allowSS = b; save(); }
    public static void setAutoHushSpecChat(boolean b) { data.autoHush.specChat = b; save(); }
    public static void setAutoHushMessage(String s) { data.autoHush.joinMessage = s; save(); }

    public static AutoGG getAutoGG() { return data.autoGG; }

    public static void setAutoGGEnabled(boolean b) { data.autoGG.enabled = b; save(); }
    public static void setAutoGGReactionary(boolean b) { data.autoGG.reactionary = b; save(); }
    public static void setAutoGGRound(boolean b) { data.autoGG.roundEnabled = b; save(); }
    public static void setAutoGGMessage(String s) { data.autoGG.message = s; save(); }

    public static void addAutoGGTrigger(String s) {
        data.autoGG.triggers.add(s.toLowerCase());
        save();
    }

    public static void removeAutoGGTrigger(String s) {
        data.autoGG.triggers.remove(s.toLowerCase());
        save();
    }

    public static AutoCope getAutoCope() { return data.autoCope; }

    public static void setAutoCopeEnabled(boolean b) { data.autoCope.enabled = b; save(); }

    public static void addAutoCopePhrase(String s) {
        data.autoCope.phrases.add(s);
        save();
    }

    public static void removeAutoCopePhrase(String s) {
        data.autoCope.phrases.remove(s);
        save();
    }

    public static AutoResponse getAutoResponse() { return data.autoResponse; }

    public static void setAutoResponseEnabled(boolean b) {
        data.autoResponse.enabled = b;
        save();
    }

    public static void addAutoResponseRule(String fromRaw, String toRaw) {

        int index = data.autoResponse.rules.keySet()
                .stream().mapToInt(i -> i).max().orElse(0) + 1;

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
            data.autoResponse.rules.put(index, rule);

        save();
    }

    public static void deleteAutoResponseRule(int index) {
        data.autoResponse.rules.remove(index);
        save();
    }

    public static void addAutoResponseValue(int index, String value, boolean toSide) {
        AutoResponseRule rule =
                data.autoResponse.rules.computeIfAbsent(index, i -> new AutoResponseRule());

        if (toSide) rule.to.add(value);
        else rule.from.add(value);

        save();
    }

    public static void removeAutoResponseValue(int index, String value) {

        AutoResponseRule rule = data.autoResponse.rules.get(index);
        if (rule == null) return;

        rule.from.remove(value);
        rule.to.remove(value);

        if (rule.from.isEmpty() || rule.to.isEmpty())
            data.autoResponse.rules.remove(index);
        save();
    }
}