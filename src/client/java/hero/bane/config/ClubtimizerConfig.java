package hero.bane.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hero.bane.Clubtimizer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClubtimizerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");
    private static ConfigData data = new ConfigData("");

    public static void load() {
        if (!FILE.exists()) {
            data = new ConfigData("");
            save();
            return;
        }

        try (FileReader reader = new FileReader(FILE)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                data = loaded;
            } else {
                Clubtimizer.LOGGER.warn("Config was empty or invalid, keeping existing values");
            }
        } catch (Exception e) {
            Clubtimizer.LOGGER.error("Error reading config, keeping previous values", e);
        }

        if (data.autoHush == null) data.autoHush = new AutoHushConfig();
        if (data.autoGG == null) data.autoGG = new AutoGGConfig();
        if (data.autoCope == null) data.autoCope = new AutoCopeConfig();
        if (data.autoResponse == null) data.autoResponse = new AutoResponseConfig();
        if (data.lobby == null) data.lobby = new LobbyConfig();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            String json = GSON.toJson(data)
                    .replace("\\n", "");
            writer.write(json);
        } catch (IOException e) {
            Clubtimizer.LOGGER.error("Error saving config", e);
        }
    }

    public static class ConfigData {
        String requeue;
        AutoHushConfig autoHush = new AutoHushConfig();
        AutoGGConfig autoGG = new AutoGGConfig();
        AutoCopeConfig autoCope = new AutoCopeConfig();
        AutoResponseConfig autoResponse = new AutoResponseConfig();
        LobbyConfig lobby = new LobbyConfig();

        ConfigData(String requeue) {
            this.requeue = requeue;
        }
    }

    public static class LobbyConfig {
        public boolean hidePlayers = false;
        public boolean hideChat = false;
        public boolean hitboxes = false;
    }

    public static class AutoHushConfig {
        public boolean enabled = false;
        public boolean allowSS = true;
        public boolean specChat = true;
        public String joinMessage = "Hi, I have chat disabled, don't want to talk, just want to fight";
    }

    public static class AutoGGConfig {
        public boolean enabled = true;
        public boolean reactionary = true;
        public boolean roundEnabled = false;
        public String message = "gg";
        public List<String> triggers = new ArrayList<>(List.of("g", "gg", "ggs", "ggg", "gs", "gggg"));
    }

    public static class AutoCopeConfig {
        public boolean enabled = false;
        public List<String> phrases = new ArrayList<>(List.of("I ghosted", "You're so lucky", "This wouldn't have happened if HerobaneNair didn't hack my computer"));
    }

    public static class AutoResponseConfig {
        public boolean enabled = true;
        public Map<String, List<String>> rules = new LinkedHashMap<>(Map.of("ghosted,ghost,gohst", List.of("idc", "autoghost moment"), "ur bad", List.of("ur mom is", "erm you got lucky punk", "nuh uh")));
    }

    public static LobbyConfig getLobby() {
        return data.lobby;
    }

    public static void setLobbyHidePlayers(boolean enabled) {
        data.lobby.hidePlayers = enabled;
        save();
    }

    public static void setLobbyHideChat(boolean enabled) {
        data.lobby.hideChat = enabled;
        save();
    }

    public static void setLobbyHitboxes(boolean enabled) {
        data.lobby.hitboxes = enabled;
        save();
    }

    public static void setRequeueOrder(String sequence) {
        data.requeue = sequence;
        save();
    }

    public static String getRequeueOrder() {
        return data.requeue != null ? data.requeue : "";
    }

    public static AutoHushConfig getAutoHush() {
        return data.autoHush;
    }

    public static void setAutoHushEnabled(boolean enabled) {
        data.autoHush.enabled = enabled;
        save();
    }

    public static void setAutoHushSS(boolean enabled) {
        data.autoHush.allowSS = enabled;
        save();
    }

    public static void setAutoHushSpecChat(boolean enabled) {
        data.autoHush.specChat = enabled;
        save();
    }

    public static void setAutoHushMessage(String msg) {
        data.autoHush.joinMessage = msg;
        save();
    }

    public static AutoGGConfig getAutoGG() {
        return data.autoGG;
    }

    public static void setAutoGGEnabled(boolean enabled) {
        data.autoGG.enabled = enabled;
        save();
    }

    public static void setAutoGGReactionary(boolean r) {
        data.autoGG.reactionary = r;
        save();
    }

    public static void setAutoGGRound(boolean enabled) {
        data.autoGG.roundEnabled = enabled;
        save();
    }

    public static void setAutoGGMessage(String m) {
        data.autoGG.message = m;
        save();
    }

    public static void addAutoGGTrigger(String trigger) {
        data.autoGG.triggers.add(trigger.toLowerCase());
        save();
    }

    public static void removeAutoGGTrigger(String trigger) {
        data.autoGG.triggers.remove(trigger.toLowerCase());
        save();
    }

    public static AutoCopeConfig getAutoCope() {
        return data.autoCope;
    }

    public static void setAutoCopeEnabled(boolean enabled) {
        data.autoCope.enabled = enabled;
        save();
    }

    public static void addAutoCopePhrase(String phrase) {
        data.autoCope.phrases.add(phrase);
        save();
    }

    public static void removeAutoCopePhrase(String phrase) {
        data.autoCope.phrases.remove(phrase);
        save();
    }

    public static AutoResponseConfig getAutoResponse() {
        return data.autoResponse;
    }

    public static void setAutoResponseEnabled(boolean enabled) {
        data.autoResponse.enabled = enabled;
        save();
    }

    public static void addAutoResponseRule(List<String> triggers, List<String> responses) {
        String key = String.join(",", triggers);
        data.autoResponse.rules.put(key, responses);
        save();
    }

    public static void removeAutoResponseRule(String trigger) {
        var it = data.autoResponse.rules.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getKey().contains(trigger)) {
                it.remove();
                save();
                return;
            }
        }
    }
}