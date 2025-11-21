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

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final File FILE =
            new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");

    private static ConfigData data = new ConfigData("");

    public static void load() {
        if (!FILE.exists()) {
            data = new ConfigData("");
            save();
            return;
        }

        try (FileReader reader = new FileReader(FILE)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) data = loaded;
            else Clubtimizer.LOGGER.warn("Config invalid, keeping defaults");
        } catch (Exception e) {
            Clubtimizer.LOGGER.error("Error loading config", e);
        }

        if (data.autoHush == null) data.autoHush = new AutoHushConfig();
        if (data.autoGG == null) data.autoGG = new AutoGGConfig();
        if (data.autoCope == null) data.autoCope = new AutoCopeConfig();
        if (data.autoResponse == null) data.autoResponse = new AutoResponseConfig();
        if (data.lobby == null) data.lobby = new LobbyConfig();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(GSON.toJson(data));
        } catch (IOException e) {
            Clubtimizer.LOGGER.error("Error saving config", e);
        }
    }

    private static void saveField(Runnable r) {
        r.run();
        save();
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
        public boolean hidePlayers;
        public boolean hideChat;
        public boolean hitboxes;
    }

    public static class AutoHushConfig {
        public boolean enabled;
        public boolean allowSS = true;
        public boolean specChat = true;
        public String joinMessage =
                "Hi, I have chat disabled, don't want to talk, just want to fight";
    }

    public static class AutoGGConfig {
        public boolean enabled = true;
        public boolean reactionary = true;
        public boolean roundEnabled;
        public String message = "gg";
        public List<String> triggers =
                new ArrayList<>(List.of(
                        "g",
                        "gg",
                        "ggs",
                        "ggg",
                        "gs",
                        "gggg",
                        "ggwp",
                        "wp"
                ));
    }

    public static class AutoCopeConfig {
        public boolean enabled;
        public List<String> phrases =
                new ArrayList<>(List.of(
                        "I ghosted",
                        "You're so lucky",
                        "This wouldn't have happened if HerobaneNair didn't hack my computer"
                ));
    }

    public static class AutoResponseConfig {
        public boolean enabled = true;
        public Map<String, List<String>> rules =
                new LinkedHashMap<>(Map.of(
                        "ghosted,ghost,gohst",
                        List.of("idc", "autoghost moment"),
                        "ur bad",
                        List.of("ur mom is", "erm you got lucky punk", "nuh uh")
                ));
    }

    public static LobbyConfig getLobby() {
        return data.lobby;
    }

    public static void setLobbyHidePlayers(boolean enabled) {
        saveField(() -> data.lobby.hidePlayers = enabled);
    }

    public static void setLobbyHideChat(boolean enabled) {
        saveField(() -> data.lobby.hideChat = enabled);
    }

    public static void setLobbyHitboxes(boolean enabled) {
        saveField(() -> data.lobby.hitboxes = enabled);
    }

    public static void setRequeueOrder(String sequence) {
        saveField(() -> data.requeue = sequence);
    }

    public static String getRequeueOrder() {
        return data.requeue == null ? "" : data.requeue;
    }

    public static AutoHushConfig getAutoHush() {
        return data.autoHush;
    }

    public static void setAutoHushEnabled(boolean enabled) {
        saveField(() -> data.autoHush.enabled = enabled);
    }

    public static void setAutoHushSS(boolean enabled) {
        saveField(() -> data.autoHush.allowSS = enabled);
    }

    public static void setAutoHushSpecChat(boolean enabled) {
        saveField(() -> data.autoHush.specChat = enabled);
    }

    public static void setAutoHushMessage(String msg) {
        saveField(() -> data.autoHush.joinMessage = msg);
    }

    public static AutoGGConfig getAutoGG() {
        return data.autoGG;
    }

    public static void setAutoGGEnabled(boolean enabled) {
        saveField(() -> data.autoGG.enabled = enabled);
    }

    public static void setAutoGGReactionary(boolean r) {
        saveField(() -> data.autoGG.reactionary = r);
    }

    public static void setAutoGGRound(boolean enabled) {
        saveField(() -> data.autoGG.roundEnabled = enabled);
    }

    public static void setAutoGGMessage(String m) {
        saveField(() -> data.autoGG.message = m);
    }

    public static void addAutoGGTrigger(String trigger) {
        String lower = trigger.toLowerCase();
        saveField(() -> data.autoGG.triggers.add(lower));
    }

    public static void removeAutoGGTrigger(String trigger) {
        String lower = trigger.toLowerCase();
        saveField(() -> data.autoGG.triggers.remove(lower));
    }

    public static AutoCopeConfig getAutoCope() {
        return data.autoCope;
    }

    public static void setAutoCopeEnabled(boolean enabled) {
        saveField(() -> data.autoCope.enabled = enabled);
    }

    public static void addAutoCopePhrase(String phrase) {
        saveField(() -> data.autoCope.phrases.add(phrase));
    }

    public static void removeAutoCopePhrase(String phrase) {
        saveField(() -> data.autoCope.phrases.remove(phrase));
    }

    public static AutoResponseConfig getAutoResponse() {
        return data.autoResponse;
    }

    public static void setAutoResponseEnabled(boolean enabled) {
        saveField(() -> data.autoResponse.enabled = enabled);
    }

    public static void addAutoResponseRule(List<String> triggers, List<String> responses) {
        String key = String.join(",", triggers);
        saveField(() -> data.autoResponse.rules.put(key, responses));
    }

    public static void removeAutoResponseRule(String trigger) {
        saveField(() -> data.autoResponse.rules.entrySet().removeIf(e -> e.getKey().contains(trigger)));
    }
}