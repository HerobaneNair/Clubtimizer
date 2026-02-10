package hero.bane.clubtimizer.state;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.action.GG;
import hero.bane.clubtimizer.action.Hush;
import hero.bane.clubtimizer.auto.Rematch;
import hero.bane.clubtimizer.auto.TotemReset;
import hero.bane.clubtimizer.mixin.accessor.PlayerListHudAccessor;
import hero.bane.clubtimizer.util.PlayerUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public class MCPVPStateChanger {

    private static final Logger LOGGER = Clubtimizer.LOGGER;
    private static volatile MCPVPState current = MCPVPState.NONE;

    private static final MCPVPState[] IN_GAME_VALUES =
            java.util.Arrays.copyOf(MCPVPState.values(), 9);
    private static final boolean[] IN_GAME_MAP = new boolean[MCPVPState.values().length];
    private static final boolean[] IN_SPEC_MAP = new boolean[MCPVPState.values().length];

    static {
        for (MCPVPState s : IN_GAME_VALUES) {
            IN_GAME_MAP[s.ordinal()] = true;
        }

        IN_SPEC_MAP[MCPVPState.SPECTATING.ordinal()] = true;
        IN_SPEC_MAP[MCPVPState.FFA_DEAD.ordinal()] = true;
        IN_SPEC_MAP[MCPVPState.TEAMFIGHT_BLUE_DEAD.ordinal()] = true;
        IN_SPEC_MAP[MCPVPState.TEAMFIGHT_RED_DEAD.ordinal()] = true;
    }

    private static boolean inGame = false;
    private static boolean inLobby = false;
    private static boolean inSpec = false;

    public static MCPVPState get() {
        return current;
    }

    private static void setState(MCPVPState newState) {
        if (newState != current) {
            if (newState == MCPVPState.RED || newState == MCPVPState.BLUE) {
                Hush.allowLobbyJoin = false;
                Hush.onMatchJoin();
            }
            current = newState;
        }
    }

    public static void update() {
        Minecraft client = Clubtimizer.client;
        var player = client.player;
        var world = client.level;
        ClientPacketListener net = client.getConnection();

        boolean valid =
                player != null &&
                        world != null &&
                        net != null &&
                        Clubtimizer.ip.contains("mcpvp.club");

        if (!valid) {
            current = MCPVPState.NONE;
            inGame = false;
            inLobby = false;
            inSpec = false;
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
                        checkLoadingIn(player.blockPosition()) ||
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
        inSpec = IN_SPEC_MAP[current.ordinal()];
    }

    private static boolean checkLimbo(ClientPacketListener net) {
        String brand = Objects.requireNonNull(net.getServerData()).name;
        if (brand.contains("Limbo")) {
            setState(MCPVPState.LIMBO);
            return true;
        }
        return false;
    }

    private static boolean checkSpectating(String actionbar) {
        if (actionbar == null) return false;
        if (actionbar.toLowerCase().contains("currently spectating")) {
            setState(MCPVPState.SPECTATING);
            return true;
        }
        return false;
    }

    private static boolean checkLoadingIn(BlockPos pos) {
        if ((pos.getX() + 10000.0 < 2.5) &&
                (pos.getY() - 255.0 < 2.5) &&
                (pos.getZ() + 10000.0 < 2.5)) {
            setState(MCPVPState.LOADING_IN);
            return true;
        }
        return false;
    }

    private static boolean checkFFA(List<String> tab, boolean spectator) {
        for (String line : tab) {
            if (line.contains("§6🗡")) {
                setState(spectator ? MCPVPState.FFA_DEAD : MCPVPState.FFA);
                return true;
            }
        }
        return false;
    }

    private static boolean checkPicking(List<String> scoreboardLines) {
        String flag = "⚑ " + Clubtimizer.playerName;
        for (String line : scoreboardLines) {
            if (line.contains(flag) && line.contains("§f")) {
                setState(MCPVPState.PICKING_TEAM);
                return true;
            }
        }
        return false;
    }

    private static boolean checkDuel(List<String> tab, List<String> scoreboardLines, String playerName) {
        if (tab.isEmpty() || !tab.getFirst().contains("Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));
        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize > 2) return false;

        String flag = "⚑ " + playerName;

        for (String line : scoreboardLines) {
            if (!line.contains(flag)) continue;

            if (line.contains("§#1FA5FF")) {
                setState(MCPVPState.BLUE);
                return true;
            } else if (line.contains("§c")) {
                setState(MCPVPState.RED);
                return true;
            }
        }
        return false;
    }

    private static boolean checkTeamFight(List<String> tab, List<String> scoreboardLines, String playerName, boolean spectator) {
        if (tab.isEmpty() || !tab.getFirst().contains("Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));
        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize <= 2) return false;

        String flag = "⚑ " + playerName;

        for (String line : scoreboardLines) {
            if (!line.contains(flag)) continue;

            if (line.contains("§#1FA5FF")) {
                setState(spectator ? MCPVPState.TEAMFIGHT_BLUE_DEAD : MCPVPState.TEAMFIGHT_BLUE);
                return true;
            } else if (line.contains("§c")) {
                setState(spectator ? MCPVPState.TEAMFIGHT_RED_DEAD : MCPVPState.TEAMFIGHT_RED);
                return true;
            }
        }
        return false;
    }

    private static boolean checkLobby(Minecraft client, String actionbar) {
        PlayerTabOverlay tabList = client.gui.getTabList();
        Component footer = ((PlayerListHudAccessor) tabList).getFooter();
        if (footer == null) return false;

        String f = TextUtil.toLegacyString(footer);
        if (!f.contains("Displaying:")) return false;

        if (actionbar != null && actionbar.contains("Queued for")) {
            setState(MCPVPState.IN_QUEUE);
            return true;
        }

        if (!inLobby()) onLobby();
        setState(MCPVPState.LOBBY);
        return true;
    }

    public static boolean inGame() { return inGame; }
    public static boolean inLobby() { return inLobby; }
    public static boolean inSpec() { return inSpec; }

    private static void onLobby() {
        Hush.matchJoin = true;
        Hush.allowLobbyJoin = true;
        Rematch.triggered = false;
        GG.resetReactionWindowEnd();
        TotemReset.resetCounter();
        PlayerUtil.requestUpdate();
    }
}
