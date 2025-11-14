package hero.bane.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.Clubtimizer;
import hero.bane.auto.Requeue;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.TextUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static hero.bane.util.ChatUtil.say;

public class ClubtimizerCommand {

    public static void register() {
        ClubtimizerConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("h-pvpclub")
                                .then(buildConfig())
                                .then(buildRequeue())
                                .then(buildAutoHush())
                                .then(buildAutoGG())
                                .then(buildAutoCope())
                                .then(buildAutoResponse())
                                .then(buildLobby())
                                .then(buildStateGet())
                )
        );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildLobby() {
        return ClientCommandManager.literal("lobby")
                .then(ClientCommandManager.literal("hideplayers")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getLobby().hidePlayers,
                                ClubtimizerConfig::setLobbyHidePlayers,
                                "Hide Players in lobby"))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHidePlayers,
                                        "Hide Players in lobby")))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHidePlayers,
                                        "Hide Players in lobby"))))
                .then(ClientCommandManager.literal("hitbox")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getLobby().hitboxes,
                                ClubtimizerConfig::setLobbyHitboxes,
                                "Turn on hitboxes when connecting"))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHitboxes,
                                        "Turn on hitboxes when connecting")))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHitboxes,
                                        "Turn on hitboxes when connecting"))))
                .then(ClientCommandManager.literal("hidechat")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getLobby().hideChat,
                                ClubtimizerConfig::setLobbyHideChat,
                                "Hide Chat in lobby"))
                        .then(ClientCommandManager.literal("on")
                                .executes(ctx -> setToggle(true,
                                        ClubtimizerConfig::setLobbyHideChat,
                                        "Hide Chat in lobby")))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> setToggle(false,
                                        ClubtimizerConfig::setLobbyHideChat,
                                        "Hide Chat in lobby"))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildStateGet() {
        return ClientCommandManager.literal("stateGet")
                .executes(ctx -> {
                    if (Clubtimizer.player == null || Clubtimizer.client.world == null) return 0;
                    MCPVPState state = MCPVPStateChanger.get();
                    say(TextUtil.rainbowGradient("Current MCPVP State: " + state));
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildConfig() {
        return ClientCommandManager.literal("config")
                .then(ClientCommandManager.literal("open")
                        .executes(ctx -> {
                            var file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "clubtimizer.json");
                            try {
                                Util.getOperatingSystem().open(file);
                                say("Opened config file", 0x55FFFF);
                            } catch (Exception e) {
                                say("Failed to open config file: " + e.getMessage(), 0xFF5555);
                            }
                            return 1;
                        }))
                .then(ClientCommandManager.literal("save")
                        .executes(ctx -> {
                            hero.bane.config.ClubtimizerConfig.save();
                            say("Config saved", 0x55FFFF);
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
        return ClientCommandManager.literal("autohush")
                .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoHush().enabled, ClubtimizerConfig::setAutoHushEnabled, "AutoHush"))
                .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoHushEnabled, "AutoHush")))
                .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoHushEnabled, "AutoHush")))
                .then(ClientCommandManager.literal("ss")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoHush().allowSS, ClubtimizerConfig::setAutoHushSS, "SS messages"))
                        .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoHushSpecChat, "SS messages")))
                        .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoHushSS, "SS messages"))))
                .then(ClientCommandManager.literal("specChat")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoHush().specChat, ClubtimizerConfig::setAutoHushSpecChat, "Spectator messages"))
                        .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoHushSpecChat, "Spectator messages")))
                        .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoHushSpecChat, "Spectator messages"))))
                .then(ClientCommandManager.literal("setmsg")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::executeSetAutoHushMessage)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoGG() {
        return ClientCommandManager.literal("autogg")
                .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoGG().enabled, ClubtimizerConfig::setAutoGGEnabled, "AutoGG"))
                .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoGGEnabled, "AutoGG")))
                .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoGGEnabled, "AutoGG")))
                .then(ClientCommandManager.literal("round")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoGG().roundEnabled, ClubtimizerConfig::setAutoGGRound, "AutoGG Round mode"))
                        .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoGGRound, "AutoGG Round mode")))
                        .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoGGRound, "AutoGG Round mode"))))
                .then(ClientCommandManager.literal("reaction")
                        .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoGG().reactionary, ClubtimizerConfig::setAutoGGReactionary, "Reactionary mode"))
                        .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoGGReactionary, "Reactionary mode")))
                        .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoGGReactionary, "Reactionary mode"))))
                .then(ClientCommandManager.literal("setmsg")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::executeSetGGMessage)))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("trigger", StringArgumentType.word())
                                .executes(ClubtimizerCommand::executeAddGGTrigger)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("trigger", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    ClubtimizerConfig.getAutoGG().triggers.forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .executes(ClubtimizerCommand::executeRemoveGGTrigger)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoCope() {
        return ClientCommandManager.literal("autocope")
                .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoCope().enabled, ClubtimizerConfig::setAutoCopeEnabled, "AutoCope"))
                .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoCopeEnabled, "AutoCope")))
                .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoCopeEnabled, "AutoCope")))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::executeAddCopePhrase)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("msg", StringArgumentType.greedyString())
                                .executes(ClubtimizerCommand::executeRemoveCopePhrase)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoResponse() {
        return ClientCommandManager.literal("autoresponse")
                .executes(ctx -> toggle(() -> ClubtimizerConfig.getAutoResponse().enabled, ClubtimizerConfig::setAutoResponseEnabled, "AutoResponse"))
                .then(ClientCommandManager.literal("on").executes(ctx -> setToggle(true, ClubtimizerConfig::setAutoResponseEnabled, "AutoResponse")))
                .then(ClientCommandManager.literal("off").executes(ctx -> setToggle(false, ClubtimizerConfig::setAutoResponseEnabled, "AutoResponse")))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("triggers", StringArgumentType.greedyString())
                                .then(ClientCommandManager.argument("responses", StringArgumentType.greedyString())
                                        .executes(ClubtimizerCommand::executeAddAutoResponseRule))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("trigger", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> {
                                    ClubtimizerConfig.getAutoResponse().rules.keySet()
                                            .forEach(list -> b.suggest(String.join(",", list)));
                                    return b.buildFuture();
                                })
                                .executes(ClubtimizerCommand::executeRemoveAutoResponseRule)));
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

    private static int executeSetAutoHushMessage(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.setAutoHushMessage(msg);
        say("AutoHush message updated", 0x55FFFF);
        return 1;
    }

    private static int executeSetGGMessage(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.setAutoGGMessage(msg);
        say("GG message updated", 0x55FFFF);
        return 1;
    }

    private static int executeAddGGTrigger(CommandContext<FabricClientCommandSource> ctx) {
        String trigger = StringArgumentType.getString(ctx, "trigger");
        ClubtimizerConfig.addAutoGGTrigger(trigger);
        say("Added GG trigger: " + trigger, 0x55FFFF);
        return 1;
    }

    private static int executeRemoveGGTrigger(CommandContext<FabricClientCommandSource> ctx) {
        String trigger = StringArgumentType.getString(ctx, "trigger");
        ClubtimizerConfig.removeAutoGGTrigger(trigger);
        say("Removed GG trigger: " + trigger, 0x55FFFF);
        return 1;
    }

    private static int executeAddCopePhrase(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.addAutoCopePhrase(msg);
        say("Phrase added", 0x55FFFF);
        return 1;
    }

    private static int executeRemoveCopePhrase(CommandContext<FabricClientCommandSource> ctx) {
        String msg = StringArgumentType.getString(ctx, "msg");
        ClubtimizerConfig.removeAutoCopePhrase(msg);
        say("Phrase removed", 0x55FFFF);
        return 1;
    }

    private static int executeAddAutoResponseRule(CommandContext<FabricClientCommandSource> ctx) {
        String triggerArg = StringArgumentType.getString(ctx, "triggers");
        String responseArg = StringArgumentType.getString(ctx, "responses");
        List<String> triggers = List.of(triggerArg.split("[,;]"));
        List<String> responses = List.of(responseArg.split("[,;]"));
        ClubtimizerConfig.addAutoResponseRule(triggers, responses);
        say("Added AutoResponse rule: " + triggers + " → " + responses, 0x55FFFF);
        return 1;
    }

    private static int executeRemoveAutoResponseRule(CommandContext<FabricClientCommandSource> ctx) {
        String trigger = StringArgumentType.getString(ctx, "trigger");
        ClubtimizerConfig.removeAutoResponseRule(trigger);
        say("Removed AutoResponse rule containing: " + trigger, 0x55FFFF);
        return 1;
    }
}
