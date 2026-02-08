package hero.bane.clubtimizer.util;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.action.AutoGG;
import hero.bane.clubtimizer.auto.Requeue;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerUtil {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static int lastRequestId = -1;

    private static final Set<String> FRIENDS = new HashSet<>();
    private static final String TARGET = "/friend remove ";

    private static final double[][] NPC_COORDS = {
            {15.5, 105.5, -57.5},
            {-24.5, 106.0, -42.5},
            {-59.5, 104.9, -15.5}
    };

    private static int nextRequestId() {
        int id = NEXT_ID.getAndIncrement();
        if (id == Integer.MAX_VALUE) NEXT_ID.set(1);
        return id;
    }

    public static void requestUpdate() {
        Minecraft c = Minecraft.getInstance();
        if (c == null || c.getConnection() == null) return;

        int id = nextRequestId();
        lastRequestId = id;

        c.getConnection().send(new ServerboundCommandSuggestionPacket(id, TARGET));
    }

    public static boolean isOurRequest(int id) {
        return id == lastRequestId;
    }

    public static void replaceAll(List<String> list) {
        FRIENDS.clear();
        for (String s : list) FRIENDS.add(normalizeName(s));
    }

    public static Set<String> get() {
        return FRIENDS;
    }

    public static boolean shouldHideEntity(Entity entity) {
        if (entity == null) return false;

        if (Spectator.isFollowing(entity)
                && Clubtimizer.client.options.getCameraType().isFirstPerson()) {
            return true;
        }

        if (entity instanceof Display.TextDisplay textDisplay
                && Clubtimizer.client.options.getCameraType().isFirstPerson()
                && Spectator.isFollowing(textDisplay.getVehicle())) {
            return true;
        }

        if (!ClubtimizerConfig.getLobby().hidePlayers) return false;
        if (!MCPVPStateChanger.inLobby()) return false;

        if (entity instanceof Player player) {
            return shouldHidePlayer(player);
        }

        if (entity instanceof Display.TextDisplay) {
            Entity vehicle = entity.getVehicle();
            if (vehicle instanceof Player player) {
                return shouldHidePlayer(player);
            }
        }

        return false;
    }

    public static boolean shouldHidePlayer(Player player) {
        if (player == null) return false;
        if (isLocal(player)) return false;

        String name = extractLastTokenName(player.getName().getString());
        if (isFriend(name)) return false;

        if (isNpc(player)) return false;
        if (Requeue.isInsideCylinder(player)) return false;
        return AutoGG.inSpawn();
    }

    public static boolean isNpc(Player p) {
        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        for (double[] c : NPC_COORDS) {
            if (x == c[0] && y == c[1] && z == c[2]) return true;
        }
        return false;
    }

    public static boolean isLocal(Player p) {
        return Clubtimizer.client.player != null && p.getUUID().equals(Clubtimizer.client.player.getUUID());
    }

    public static String extractLastTokenName(String raw) {
        if (raw == null) return "";
        String stripped = TextUtil.stripFormatting(raw).strip();
        if (stripped.isEmpty()) return stripped;
        String[] parts = stripped.split("\\s+");
        return parts[parts.length - 1];
    }

    public static String normalizeName(String raw) {
        if (raw == null) return "";
        raw = raw.strip();
        if (raw.isEmpty()) return raw;
        char c = raw.charAt(0);
        if (!Character.isLetterOrDigit(c)) raw = raw.substring(1).strip();
        return raw.toLowerCase();
    }

    public static boolean isSelfOrFriend(String raw) {
        String name = normalizeName(raw);
        if (name.equals(Clubtimizer.playerName.toLowerCase())) return true;
        return FRIENDS.contains(name);
    }

    public static boolean isFriend(String raw) {
        String name = normalizeName(raw);
        return FRIENDS.contains(name);
    }
}
