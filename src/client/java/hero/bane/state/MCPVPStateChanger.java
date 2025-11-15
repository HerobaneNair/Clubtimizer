package hero.bane.state;

import hero.bane.Clubtimizer;
import hero.bane.action.AutoGG;
import hero.bane.action.AutoHush;
import hero.bane.auto.Rematch;
import hero.bane.auto.TotemResetter;
import hero.bane.mixin.accessor.PlayerListHudAccessor;
import hero.bane.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public class MCPVPStateChanger {

    private static final Logger LOGGER = Clubtimizer.LOGGER;
    private static volatile MCPVPState current = MCPVPState.NONE;

    private static final MCPVPState[] IN_GAME_VALUES =
            java.util.Arrays.copyOf(MCPVPState.values(), 9);
    private static final boolean[] IN_GAME_MAP = new boolean[MCPVPState.values().length];

    static {
        for (MCPVPState s : IN_GAME_VALUES) {
            IN_GAME_MAP[s.ordinal()] = true;
        }
    }

    private static boolean inGame = false;
    private static boolean inLobby = false;

    public static MCPVPState get() {
        return current;
    }

    private static void setState(MCPVPState newState) {
        if (newState != current) {
            if (newState == MCPVPState.RED || newState == MCPVPState.BLUE) {
                AutoHush.allowLobbyJoin = false;
                AutoHush.onMatchJoin();
            }
            current = newState;
        }
    }

    public static void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        ClientPlayNetworkHandler net = client.getNetworkHandler();
        var server = client.getCurrentServerEntry();

        boolean valid =
                player != null &&
                        world != null &&
                        net != null &&
                        server != null &&
                        TextUtil.fastContains(server.address, "mcpvp.club");

        if (!valid) {
            current = MCPVPState.NONE;
            inGame = false;
            inLobby = false;
            return;
        }

        String actionbar = TextUtil.getActionbarText(client);
        List<String> tab = TextUtil.getOrderedTabList(client);
        List<String> scoreboardLines = TextUtil.getScoreboardLines(client);
        String playerName = player.getName().getString();
        boolean spectator = player.isSpectator();

        boolean matched =
                checkLimbo(net) ||
                        checkSpectating(actionbar) ||
                        checkLoadingIn(player.getBlockPos()) ||
                        checkFFA(tab, spectator) ||
                        checkPicking(scoreboardLines) ||
                        checkDuel(tab, scoreboardLines, playerName) ||
                        checkTeamFight(tab, scoreboardLines, playerName, spectator) ||
                        checkLobby(client, actionbar);

        if (!matched) {
            LOGGER.error("Failed to obtain mcpvp state");
        }

        inGame = IN_GAME_MAP[current.ordinal()];
        inLobby = (current == MCPVPState.LOBBY || current == MCPVPState.IN_QUEUE);
    }

    private static boolean checkLimbo(ClientPlayNetworkHandler net) {
        String brand = Objects.requireNonNull(net).getBrand();
        if (brand != null && TextUtil.fastContains(brand, "Limbo")) {
            setState(MCPVPState.LIMBO);
            return true;
        }
        return false;
    }

    private static boolean checkSpectating(String actionbar) {
        if (actionbar == null) return false;
        String lower = actionbar.toLowerCase();
        if (TextUtil.fastContains(lower, "currently spectating")) {
            setState(MCPVPState.SPECTATING);
            return true;
        }
        return false;
    }

    private static boolean checkLoadingIn(BlockPos pos) {
        if (((pos.getX() + 10000.0) < 2.5) &&
                ((pos.getY() - 255.0) < 2.5) &&
                ((pos.getZ() + 10000.0) < 2.5)) {
            setState(MCPVPState.LOADING_IN);
            return true;
        }
        return false;
    }

    private static boolean checkFFA(List<String> tab, boolean spectator) {
        for (String line : tab) {
            if (TextUtil.fastContains(line, "§6🗡")) {
                setState(spectator ? MCPVPState.FFA_DEAD : MCPVPState.FFA);
                return true;
            }
        }
        return false;
    }

    private static boolean checkPicking(List<String> scoreboardLines) {
        String flag = "⚑ " + Clubtimizer.playerName;
        for (String line : scoreboardLines) {
            if (TextUtil.fastContains(line, flag) && TextUtil.fastContains(line, "§f")) {
                setState(MCPVPState.PICKING_TEAM);
                return true;
            }
        }
        return false;
    }

    private static boolean checkDuel(List<String> tab, List<String> scoreboardLines, String playerName) {
        if (tab.isEmpty() || !TextUtil.fastContains(tab.getFirst(), "Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));
        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize > 2) return false;

        String flag = "⚑ " + playerName;

        for (String line : scoreboardLines) {
            if (!TextUtil.fastContains(line, flag)) continue;

            if (TextUtil.fastContains(line, "§#1FA5FF")) {
                setState(MCPVPState.BLUE);
                return true;
            } else if (TextUtil.fastContains(line, "§c")) {
                setState(MCPVPState.RED);
                return true;
            } else {
                LOGGER.info(line);
            }
        }
        return false;
    }

    private static boolean checkTeamFight(List<String> tab, List<String> scoreboardLines, String playerName, boolean spectator) {
        if (tab.isEmpty() || !TextUtil.fastContains(tab.getFirst(), "Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));
        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize <= 2) return false;

        String flag = "⚑ " + playerName;

        for (String line : scoreboardLines) {
            if (!TextUtil.fastContains(line, flag)) continue;

            if (TextUtil.fastContains(line, "§#1FA5FF")) {
                setState(spectator ? MCPVPState.TEAMFIGHT_BLUE_DEAD : MCPVPState.TEAMFIGHT_BLUE);
                return true;
            } else if (TextUtil.fastContains(line, "§c")) {
                setState(spectator ? MCPVPState.TEAMFIGHT_RED_DEAD : MCPVPState.TEAMFIGHT_RED);
                return true;
            }
        }
        return false;
    }

    private static boolean checkLobby(MinecraftClient client, String actionbar) {
        PlayerListHud hud = client.inGameHud.getPlayerListHud();
        Text footer = ((PlayerListHudAccessor) hud).getFooter();
        if (footer == null) return false;

        String f = TextUtil.toLegacyString(footer);
        if (!TextUtil.fastContains(f, "Displaying:")) return false;

        if (actionbar != null && TextUtil.fastContains(actionbar, "Queued for")) {
            setState(MCPVPState.IN_QUEUE);
            return true;
        }

        if (!inLobby()) onLobby();
        setState(MCPVPState.LOBBY);
        return true;
    }

    public static boolean inGame() {
        return inGame;
    }

    public static boolean inLobby() {
        return inLobby;
    }

    private static void onLobby() {
        AutoHush.matchJoin = true;
        AutoHush.allowLobbyJoin = true;
        Rematch.triggered = false;
        AutoGG.resetReactionWindowEnd();
        TotemResetter.resetCounter();
    }
}
