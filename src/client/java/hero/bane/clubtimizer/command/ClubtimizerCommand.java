package hero.bane.clubtimizer.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.auto.Requeue;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.TextUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public class ClubtimizerCommand {
    private static final String NEW_AR_RULE_HINT = " from (only inputs, use add to add outputs, separate with |'s) ";

    public static void register() {
        ClubtimizerConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("h-club")
                                .requires(source ->
                                        Clubtimizer.ip.contains("mcpvp.club") //Seemed like a good add, hopefully doesn't break anything
                                )
                                .executes(ClubtimizerCommand::debugger)
                                .then(buildConfig())
                                .then(buildRequeue())
                                .then(buildAutoHush())
                                .then(buildAutoGG())
                                .then(buildAutoCope())
                                .then(buildAutoResponse())
                                .then(buildLobby())
                )
        );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildLobby() {
        return ClientCommandManager.literal("lobby")
                .then(ClientCommandManager.literal("hidePlayers")
                        .executes(ctx -> toggle(
                                () -> ClubtimizerConfig.getLobby().hidePlayers,
                                ClubtimizerConfig::setLobbyHidePlayers,
                                "Hide Players in lobby"
                        ))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHidePlayers,
                                        "Hide Players in lobby"
                                )))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHidePlayers,
                                        "Hide Players in lobby"
                                )))
                )
                .then(ClientCommandManager.literal("hitbox")
                        .executes(ctx -> toggle(
                                () -> ClubtimizerConfig.getLobby().hitboxes,
                                ClubtimizerConfig::setLobbyHitboxes,
                                "Turn on hitboxes when connecting"
                        ))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHitboxes,
                                        "Turn on hitboxes when connecting"
                                )))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHitboxes,
                                        "Turn on hitboxes when connecting"
                                )))
                )
                .then(ClientCommandManager.literal("warning")
                        .executes(ctx -> toggle(
                                () -> ClubtimizerConfig.getLobby().warning,
                                ClubtimizerConfig::setLobbyWarning,
                                "MCPVP warnings in lobby"
                        ))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyWarning,
                                        "MCPVP warnings in lobby"
                                )))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyWarning,
                                        "MCPVP warnings in lobby"
                                )))
                )
                .then(ClientCommandManager.literal("hideChat")
                        .executes(ctx -> toggle(
                                () -> ClubtimizerConfig.getLobby().hideChat,
                                ClubtimizerConfig::setLobbyHideChat,
                                "Hide Chat in lobby"
                        ))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHideChat,
                                        "Hide Chat in lobby"
                                )))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHideChat,
                                        "Hide Chat in lobby"
                                )))
                )
                .then(ClientCommandManager.literal("hidePublicParties")
                        .executes(ctx -> toggle(
                                () -> ClubtimizerConfig.getLobby().hidePublicParties,
                                ClubtimizerConfig::setLobbyHidePublicParties,
                                "Hide Public Party Messages in lobby"
                        ))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHidePublicParties,
                                        "Hide Party Messages in lobby"
                                )))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHidePublicParties,
                                        "Hide Party Messages in lobby"
                                )))
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildConfig() {
        return ClientCommandManager.literal("config")
                .then(ClientCommandManager.literal("open").executes(ctx -> {
                    File f = new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");
                    try {
                        Util.getPlatform().openFile(f);
                        say("Opened Config", 0x55FFFF);
                    } catch (Exception e) {
                        say("Failed to open config: " + e.getMessage(), 0xFF5555);
                        Clubtimizer.LOGGER.error("Failed to open config: ", e);
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("save").executes(ctx -> {
                    ClubtimizerConfig.load();
                    if (ClubtimizerConfig.save()) {
                        say("Saved Config", 0x55FF55);
                    }
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRequeue() {
        return ClientCommandManager.literal("requeue")
                .executes(ctx -> showList())
                .then(ClientCommandManager.argument("order", StringArgumentType.word())
                        .executes(Requeue::handleRequeue));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoHush() {
        return literalToggleGroupRoot(
                "autoHush",
                () -> ClubtimizerConfig.getAutoHush().enabled,
                ClubtimizerConfig::setAutoHushEnabled,
                "AutoHush")
                .then(toggleSub(
                        "ss",
                        () -> ClubtimizerConfig.getAutoHush().allowSS,
                        ClubtimizerConfig::setAutoHushSS,
                        "SS messages"
                ))
                .then(toggleSub(
                        "specChat",
                        () -> ClubtimizerConfig.getAutoHush().specChat,
                        ClubtimizerConfig::setAutoHushSpecChat,
                        "Spectator messages"
                ))
                .then(ClientCommandManager.literal("setMsg")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::setAutoHushMessage)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoGG() {
        return literalToggleGroupRoot(
                "autoGG",
                () -> ClubtimizerConfig.getAutoGG().enabled,
                ClubtimizerConfig::setAutoGGEnabled,
                "AutoGG")
                .then(toggleSub(
                        "round",
                        () -> ClubtimizerConfig.getAutoGG().roundEnabled,
                        ClubtimizerConfig::setAutoGGRound,
                        "AutoGG Round mode"
                ))
                .then(toggleSub(
                        "reaction",
                        () -> ClubtimizerConfig.getAutoGG().reactionary,
                        ClubtimizerConfig::setAutoGGReactionary,
                        "Reactionary mode"
                ))
                .then(ClientCommandManager.literal("setMsg")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::setGGMessage)))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("trigger", StringArgumentType.word())
                                .executes(ClubtimizerCommand::addGGTrigger)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("trigger", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    var triggers = ClubtimizerConfig.getAutoGG().triggers;
                                    if (triggers != null) {
                                        triggers.forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ClubtimizerCommand::removeGGTrigger)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoCope() {
        return literalToggleGroupRoot(
                "autoCope",
                () -> ClubtimizerConfig.getAutoCope().enabled,
                ClubtimizerConfig::setAutoCopeEnabled,
                "AutoCope")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::addCopePhrase)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::removeCopePhrase)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoResponse() {
        return literalToggleGroupRoot(
                "autoResponse",
                () -> ClubtimizerConfig.getAutoResponse().enabled,
                ClubtimizerConfig::setAutoResponseEnabled,
                "AutoResponse")
                .then(ClientCommandManager.literal("rule")
                        .then(ClientCommandManager.literal("show")
                                .then(ClientCommandManager.argument("index", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            ClubtimizerConfig.getAutoResponse().rules.keySet()
                                                    .forEach(i -> b.suggest(Integer.toString(i)));
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            int index = Integer.parseInt(
                                                    StringArgumentType.getString(ctx, "index")
                                            );
                                            var rule = ClubtimizerConfig.getAutoResponse().rules.get(index);
                                            if (rule == null) return 0;
                                            say("-----");
                                            say("Rule #" + index, 0xAAAAAA, false);
                                            say(String.join(" | ", rule.from), 0xFFCCAA, false);
                                            say("goes to", 0xAAAAAA, false);
                                            say(String.join(" | ", rule.to), 0xAAFFAA, false);
                                            say("-----");
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("new")
                                .then(ClientCommandManager.argument(NEW_AR_RULE_HINT, StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String from = StringArgumentType.getString(ctx, NEW_AR_RULE_HINT);
                                            int index = ClubtimizerConfig.getAutoResponse().rules
                                                    .keySet()
                                                    .stream()
                                                    .mapToInt(i -> i)
                                                    .max()
                                                    .orElse(0) + 1;

                                            ClubtimizerConfig.addAutoResponseRule(from, ".");
                                            String msg =
                                                    "§aAutoResponse rule created.\n" +
                                                            "§aAdd values with edit §e" + index + " §aadd value <msg>";

                                            say(TextUtil.fromLegacy(msg));

                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("delete")
                                .then(ClientCommandManager.argument("index", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            ClubtimizerConfig.getAutoResponse().rules.keySet()
                                                    .forEach(i -> b.suggest(Integer.toString(i)));
                                            return b.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            int index = Integer.parseInt(
                                                    StringArgumentType.getString(ctx, "index")
                                            );

                                            ClubtimizerConfig.deleteAutoResponseRule(index);
                                            say("Rule deleted", 0xFF5555);
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("edit")
                                .then(ClientCommandManager.argument("index", StringArgumentType.word())
                                        .suggests((ctx, b) -> {
                                            ClubtimizerConfig.getAutoResponse().rules.keySet()
                                                    .forEach(i -> b.suggest(Integer.toString(i)));
                                            return b.buildFuture();
                                        })
                                        .then(ClientCommandManager.literal("add")
                                                .then(ClientCommandManager.literal("input")
                                                        .then(ClientCommandManager.argument("entry", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    String raw = StringArgumentType.getString(ctx, "entry");

                                                                    for (String s : raw.split("\\|")) {
                                                                        String v = s.trim().toLowerCase();
                                                                        if (!v.isEmpty()) {
                                                                            ClubtimizerConfig.addAutoResponseValue(index, v, false);
                                                                        }
                                                                    }

                                                                    say("Rule updated", 0x55FFFF);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(ClientCommandManager.literal("output")
                                                        .then(ClientCommandManager.argument("entry", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    String raw = StringArgumentType.getString(ctx, "entry");

                                                                    for (String s : raw.split("\\|")) {
                                                                        String v = s.trim();
                                                                        if (!v.isEmpty()) {
                                                                            ClubtimizerConfig.addAutoResponseValue(index, v, true);
                                                                        }
                                                                    }

                                                                    say("Rule updated", 0x55FFFF);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                        .then(ClientCommandManager.literal("remove")

                                                .then(ClientCommandManager.literal("input")
                                                        .then(ClientCommandManager.argument("entry", StringArgumentType.word())
                                                                .suggests((ctx, b) -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    var rule = ClubtimizerConfig.getAutoResponse().rules.get(index);
                                                                    if (rule != null) rule.from.forEach(b::suggest);
                                                                    return b.buildFuture();
                                                                })
                                                                .executes(ctx -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    String entry = StringArgumentType
                                                                            .getString(ctx, "entry")
                                                                            .toLowerCase();

                                                                    ClubtimizerConfig.removeAutoResponseValue(index, entry);
                                                                    say("Rule updated", 0xFFAA00);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(ClientCommandManager.literal("output")
                                                        .then(ClientCommandManager.argument("entry", StringArgumentType.word())
                                                                .suggests((ctx, b) -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    var rule = ClubtimizerConfig.getAutoResponse().rules.get(index);
                                                                    if (rule != null) rule.to.forEach(b::suggest);
                                                                    return b.buildFuture();
                                                                })
                                                                .executes(ctx -> {
                                                                    int index = Integer.parseInt(
                                                                            StringArgumentType.getString(ctx, "index")
                                                                    );
                                                                    String entry = StringArgumentType.getString(ctx, "entry");
                                                                    ClubtimizerConfig.removeAutoResponseValue(index, entry);
                                                                    say("Rule updated", 0xFFAA00);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> literalToggleGroupRoot(
            String root, Supplier<Boolean> getter, Consumer<Boolean> setter, String label) {

        return ClientCommandManager.literal(root)
                .executes(ctx -> toggle(getter, setter, label))
                .then(ClientCommandManager.literal("on")
                        .executes(ctx -> setToggle(true, setter, label)))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx -> setToggle(false, setter, label)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleSub(
            String sub, Supplier<Boolean> getter, Consumer<Boolean> setter, String label) {

        return ClientCommandManager.literal(sub)
                .executes(ctx -> toggle(getter, setter, label))
                .then(ClientCommandManager.literal("on")
                        .executes(ctx -> setToggle(true, setter, label)))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx -> setToggle(false, setter, label)));
    }

    private static int toggle(Supplier<Boolean> getter, Consumer<Boolean> setter, String label) {
        boolean newState = !getter.get();
        setter.accept(newState);
        say(label + " " + (newState ? "enabled" : "disabled"), newState ? 0x55FF55 : 0xFF5555);
        return 1;
    }

    private static int setToggle(boolean newState, Consumer<Boolean> setter, String label) {
        setter.accept(newState);
        say(label + " " + (newState ? "enabled" : "disabled"), newState ? 0x55FF55 : 0xFF5555);
        return 1;
    }

    private static int showList() {
        say("Available gamemodes", 0x55FFFF, true);
        for (int i = 0; i < Requeue.GAMEMODES.length; i++) {
            say((i + 1) + ". " + Requeue.GAMEMODES[i], 0xFFFFFF, false);
        }
        say("/h-pvpclub requeue <order>\n(i.e. /h-pvpclub requeue 8136)", 0xAAAAAA, false);
        return 1;
    }

    private static int setAutoHushMessage(CommandContext<FabricClientCommandSource> ctx) {
        ClubtimizerConfig.setAutoHushMessage(StringArgumentType.getString(ctx, "msg"));
        say("AutoHush message updated", 0x55FFFF);
        return 1;
    }

    private static int setGGMessage(CommandContext<FabricClientCommandSource> ctx) {
        ClubtimizerConfig.setAutoGGMessage(StringArgumentType.getString(ctx, "msg"));
        say("GG message updated", 0x55FFFF);
        return 1;
    }

    private static int addGGTrigger(CommandContext<FabricClientCommandSource> ctx) {
        String trigger = StringArgumentType.getString(ctx, "trigger");
        ClubtimizerConfig.addAutoGGTrigger(trigger);
        say("Added GG trigger: " + trigger, 0x55FFFF);
        return 1;
    }

    private static int removeGGTrigger(CommandContext<FabricClientCommandSource> ctx) {
        String trigger = StringArgumentType.getString(ctx, "trigger");
        ClubtimizerConfig.removeAutoGGTrigger(trigger);
        say("Removed GG trigger: " + trigger, 0x55FFFF);
        return 1;
    }

    private static int addCopePhrase(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.addAutoCopePhrase(msg);
        say("Phrase added", 0x55FFFF);
        return 1;
    }

    private static int removeCopePhrase(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.removeAutoCopePhrase(msg);
        say("Phrase removed", 0x55FFFF);
        return 1;
    }

    private static int debugger(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Clubtimizer.player;
        if (player == null || Clubtimizer.client.level == null) return 0;

        MCPVPState state = MCPVPStateChanger.get();
        say(TextUtil.rainbowGradient("Current MCPVP State: " + state));

        String clubtimizerVersion = FabricLoader.getInstance()
                .getModContainer("clubtimizer")
                .get()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        say("Clubtimizer Version: " + clubtimizerVersion, 0xFFFFFF);

        return 1;
    }
}