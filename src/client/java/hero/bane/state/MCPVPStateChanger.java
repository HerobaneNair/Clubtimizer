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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MCPVPStateChanger {

    private static final Logger LOGGER = Clubtimizer.LOGGER;
    private static volatile MCPVPState current = MCPVPState.NONE;
    private static final MCPVPState[] IN_GAME_VALUES =
            java.util.Arrays.copyOf(MCPVPState.values(), 8);
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
        ClientPlayNetworkHandler net = client.getNetworkHandler();

        boolean valid =
                client.player != null &&
                        client.world != null &&
                        net != null &&
                        isOnMCPVP(client);
        if (!valid) {
            current = MCPVPState.NONE;
        } else {
            boolean matched =
                    checkLimbo(client) ||
                            checkSpectating(client) ||
                            checkLoadingIn(client) ||
                            checkFFA(client) ||
                            checkPicking(client) ||
                            checkDuel(client) ||
                            checkTeamFight(client) ||
                            checkLobby(client);

            if (!matched) {
                Clubtimizer.LOGGER.error("Failed to obtain mcpvp state");
            }
        }
        inGame = Arrays.asList(IN_GAME_VALUES).contains(current);
        inLobby = (current.equals(MCPVPState.LOBBY) || current.equals(MCPVPState.IN_QUEUE));
    }

    private static boolean isOnMCPVP(MinecraftClient client) {
        return client.getCurrentServerEntry() != null &&
                client.getCurrentServerEntry().address.contains("mcpvp.club");
    }

    private static boolean checkLimbo(MinecraftClient client) {
        String brand = Objects.requireNonNull(client.getNetworkHandler()).getBrand();
        if (brand != null && brand.contains("Limbo")) {
            setState(MCPVPState.LIMBO);
            return true;
        }
        return false;
    }

    private static boolean checkSpectating(MinecraftClient client) {
        String actionbar = TextUtil.getActionbarText(client);
        if (actionbar != null && actionbar.toLowerCase().contains("currently spectating")) {
            setState(MCPVPState.SPECTATING);
            return true;
        }
        return false;
    }

    private static boolean checkLoadingIn(MinecraftClient client) {
        assert client.player != null;
        BlockPos pos = client.player.getBlockPos();
        if (((pos.getX() + 10000.0) < 2.5) && ((pos.getY() - 255.0) < 2.5) && ((pos.getZ() + 10000.0) < 2.5)) {
            setState(MCPVPState.LOADING_IN);
            return true;
        }
        return false;
    }

    private static boolean checkFFA(MinecraftClient client) {
        for (String line : TextUtil.getOrderedTabList(client)) {
            if (line.contains("§6🗡")) {
                assert client.player != null;
                setState(client.player.isSpectator() ? MCPVPState.FFA_DEAD : MCPVPState.FFA);
                return true;
            }
        }
        return false;
    }

    private static boolean checkPicking(MinecraftClient client) {
        for (String line : TextUtil.getScoreboardLines(client)) {
            if (line.contains("⚑ " + Clubtimizer.playerName) && line.contains("§f")) {
                setState(MCPVPState.PICKING_TEAM);
                return true;
            }
        }
        return false;
    }

    private static boolean checkDuel(MinecraftClient client) {
        List<String> tab = TextUtil.getOrderedTabList(client);
        if (tab.isEmpty() || !tab.getFirst().contains("Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));

        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize > 2) return false;

        assert client.player != null;
        String playerName = client.player.getName().getString();

        for (String line : TextUtil.getScoreboardLines(client)) {
            if (line.contains("⚑ " + playerName)) {
                if (line.contains("§#1FA5FF")) {
                    setState(MCPVPState.BLUE);
                    return true;
                } else if (line.contains("§c")) {
                    setState(MCPVPState.RED);
                    return true;
                } else LOGGER.info(line);
            }
        }
        return false;
    }

    private static boolean checkTeamFight(MinecraftClient client) {
        List<String> tab = TextUtil.getOrderedTabList(client);
        if (tab.isEmpty() || !tab.getFirst().contains("Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));

        int skulls = TextUtil.countSkulls(tab);
        if (skulls + duelSize <= 2) return false;

        assert client.player != null;
        String playerName = client.player.getName().getString();
        boolean spectator = client.player.isSpectator();

        List<String> lines = TextUtil.getScoreboardLines(client);
        for (String line : lines) {
            if (!line.contains("⚑ " + playerName)) continue;

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

    private static boolean checkLobby(MinecraftClient client) {
        PlayerListHud hud = client.inGameHud.getPlayerListHud();
        Text footer = ((PlayerListHudAccessor) hud).getFooter();
        if (footer == null) return false;

        String f = TextUtil.toLegacyString(footer);
        boolean isLobby = f.contains("Displaying:");
        if (!isLobby) return false;

        String actionbar = TextUtil.getActionbarText(client);
        if (actionbar != null && actionbar.contains("Queued for")) {
            setState(MCPVPState.IN_QUEUE);
            return true;
        }

        if (!MCPVPStateChanger.inLobby()) onLobby();
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