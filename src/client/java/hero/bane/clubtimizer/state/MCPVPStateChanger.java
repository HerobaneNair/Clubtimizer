package hero.bane.clubtimizer.state;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.action.GG;
import hero.bane.clubtimizer.action.Hush;
import hero.bane.clubtimizer.auto.Rematch;
import hero.bane.clubtimizer.auto.Totem;
import hero.bane.clubtimizer.mixin.accessor.PlayerListHudAccessor;
import hero.bane.clubtimizer.util.PlayerUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MCPVPStateChanger {
    private static volatile MCPVPState current = MCPVPState.NONE;

    private static final MCPVPState[] IN_GAME_VALUES =
            Arrays.copyOf(MCPVPState.values(), 9);
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
        if (newState == current) return;
        if (IN_GAME_MAP[newState.ordinal()]) {
            Hush.allowLobbyJoin = false;
            Hush.onMatchJoin();
        }
        current = newState;
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
        String playerName = player.getName().getString();
        boolean spectator = player.isSpectator();

        {
            boolean matched =
                    checkLimbo(net) ||
                            checkSpectating(actionbar) ||
                            checkLoadingIn(player.blockPosition()) ||
                            checkFFA(tab, spectator) ||
                            checkColoredFight(tab, playerName, spectator) ||
                            checkLobby(client, actionbar);
        }

        inGame = IN_GAME_MAP[current.ordinal()];
        inLobby = (current == MCPVPState.LOBBY || current == MCPVPState.IN_QUEUE);
        inSpec = IN_SPEC_MAP[current.ordinal()];

//        if (current != MCPVPState.NONE) {
//            player.displayClientMessage(Component.literal("Enum: " + current), true);
//        }
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
        if (tab.size() < 2 || !tab.getFirst().contains("Duel")) return false;
        if (tab.get(2).contains("§6🗡")) {
            setState(spectator ? MCPVPState.FFA_DEAD : MCPVPState.FFA);
            return true;
        }
        return false;
    }

    private static boolean checkColoredFight(List<String> tab, String playerName, boolean spectator) {
        if (tab.isEmpty() || !tab.getFirst().contains("Duel")) return false;

        int duelSize = TextUtil.parseDuelSize(TextUtil.stripFormatting(tab.getFirst()));
        int skulls = TextUtil.countSkulls(tab);

        boolean isTeamFight = skulls + duelSize > 2;

        Minecraft client = Clubtimizer.client;
        var entries = ((PlayerListHudAccessor) client.gui.getTabList()).invokeGetPlayerInfos();

        int limit = Math.min(4, entries.size());

        for (int i = 0; i < limit; i++) {
            var info = entries.get(i);
            Component display = info.getTabListDisplayName();
            if (display == null) continue;

            String raw = display.getString();
            if (!raw.startsWith("🗡")) continue;

            String[] split = raw.split(" ");
            if (split.length < 2) continue;
            if (!Objects.equals(split[1], playerName)) continue;

            String legacy = TextUtil.toLegacyString(display);

            if (legacy.startsWith("§#1FA5FF🗡")) {

                if (!wasPreMatch()) return true;

                if (isTeamFight) {
                    setState(spectator
                            ? MCPVPState.TEAMFIGHT_BLUE_DEAD
                            : MCPVPState.TEAMFIGHT_BLUE);
                } else {
                    setState(MCPVPState.BLUE);
                }
                return true;
            }

            if (legacy.startsWith("§c🗡")) {

                if (!wasPreMatch()) return true;

                if (isTeamFight) {
                    setState(spectator
                            ? MCPVPState.TEAMFIGHT_RED_DEAD
                            : MCPVPState.TEAMFIGHT_RED);
                } else {
                    setState(MCPVPState.RED);
                }
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

    public static boolean inGame() {
        return inGame;
    }

    public static boolean inLobby() {
        return inLobby;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean inSpec() {
        return inSpec;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean wasPreMatch() {
        return current == MCPVPState.LOADING_IN ||
                current == MCPVPState.LOBBY ||
                current == MCPVPState.IN_QUEUE ||
                current == MCPVPState.PICKING_TEAM;
    }

    private static void onLobby() {
        Hush.matchJoin = true;
        Hush.allowLobbyJoin = true;
        Rematch.triggered = false;
        GG.resetReactionWindow();
        Totem.resetCounter();
        PlayerUtil.requestUpdate();
    }
}
