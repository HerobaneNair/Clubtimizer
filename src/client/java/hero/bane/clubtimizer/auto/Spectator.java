package hero.bane.clubtimizer.auto;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PingUtil;
import hero.bane.clubtimizer.util.TextUtil;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class Spectator {

    private static final List<String> aliveList = new ArrayList<>();
    private static long lastTick = -1L;

    private static Player followed = null;
    private static long lastTeleport = 0L;
    private static long followPauseUntil = 0L;
    private static boolean roundEnded = false;

    public static synchronized void updateAlivelist(List<String> newList, long t) {
        if (t == lastTick) return;
        lastTick = t;
        aliveList.clear();
        aliveList.addAll(newList);
    }

    public static synchronized boolean hasEntries() {
        return !aliveList.isEmpty();
    }

    public static synchronized List<String> getAll() {
        return new ArrayList<>(aliveList);
    }

    public static PlayerInfo findInfoByCleanName(String cleanName) {
        if (Clubtimizer.client == null || Clubtimizer.client.getConnection() == null)
            return null;

        for (PlayerInfo info : Clubtimizer.client.getConnection().getListedOnlinePlayers()) {
            ComponentSafe:
            {
                if (info.getTabListDisplayName() == null) break ComponentSafe;

                String stripped =
                        TextUtil.stripFormatting(
                                TextUtil.toLegacyString(info.getTabListDisplayName())
                        ).trim();

                String[] parts = stripped.split(" ");
                if (parts.length >= 2 && parts[1].equals(cleanName)) {
                    return info;
                }
            }
        }
        return null;
    }

    public static boolean isFollowing(Entity e) {
        return followed != null && followed == e;
    }

    public static void startFollowing(Entity target) {
        if (!(target instanceof Player p)) return;
        followed = p;
        Clubtimizer.client.setCameraEntity(p);
        lastTeleport = 0L;
        followPauseUntil = 0L;
    }

    public static void stopFollowing() {
        followed = null;
        if (Clubtimizer.client.player != null) {
            Clubtimizer.client.setCameraEntity(Clubtimizer.client.player);
        }
    }

    private static String cleanName(Player p) {
        if (Clubtimizer.client == null || Clubtimizer.client.getConnection() == null)
            return p.getName().getString();

        PlayerInfo info = Clubtimizer.client.getConnection().getPlayerInfo(p.getUUID());
        if (info == null || info.getTabListDisplayName() == null)
            return p.getName().getString();

        String stripped =
                TextUtil.stripFormatting(
                        TextUtil.toLegacyString(info.getTabListDisplayName())
                ).trim();

        String[] parts = stripped.split(" ");
        return parts.length >= 2 ? parts[1] : p.getName().getString();
    }

    public static void handleTick() {
        Minecraft client = Clubtimizer.client;

        if (followed == null || client.player == null) {
            stopFollowing();
            return;
        }

        if (!MCPVPStateChanger.inSpec() || client.options.keyShift.isDown()) {
            stopFollowing();
            return;
        }

        if (roundEnded) {
            stopFollowing();
            roundEnded = false;
            return;
        }

        String clean = cleanName(followed);
        if (!aliveList.contains(clean)) {
            stopFollowing();
            return;
        }

        if (client.level == null) {
            stopFollowing();
            return;
        }

        boolean entityLoaded = client.level.players().contains(followed);
        if (!entityLoaded) {
            ChatUtil.chat("/tp " + clean);
            return;
        }

        double distSq = client.player.distanceToSqr(followed);
        if (distSq > (256 * 256)) {
            ChatUtil.chat("/tp " + clean);
            return;
        }

        teleportToTarget(client);

        long now = System.currentTimeMillis();
        if (now >= followPauseUntil) {
            client.setCameraEntity(followed);
        }
    }

    private static void teleportToTarget(Minecraft client) {
        if (client.player == null || followed == null) return;

        long now = System.currentTimeMillis();
        if (now - lastTeleport < 1500) return;

        lastTeleport = now;

        client.player.setPos(
                followed.getX(),
                followed.getY(),
                followed.getZ()
        );

        soundFixer(client);

        double ping = PingUtil.parseScoreboardPing(client);
        followPauseUntil = now + (long)(ping * 2.8);
    }

    public static void handleMessage(String text) {
        if (!MCPVPStateChanger.inSpec()) return;
        if (followed == null) return;

        var player = Clubtimizer.player;
        if (player != null) {
            double x = player.getX();
            double z = player.getZ();
            if (x >= -300 && x <= 300 && z >= -300 && z <= 300) return;
        }

        if (TextUtil.roundEnd(text, true)) {
            roundEnded = true;
        }
    }

    private static void soundFixer(Minecraft client) {
        if (client.player == null || followed == null) return;

        Entity original = followed;
        client.setCameraEntity(client.player);
        client.setCameraEntity(original);
    }

    public static void onEntitiesDestroyed(IntList ids) {
        if (followed == null) return;

        int targetId = followed.getId();
        for (int id : ids) {
            if (id == targetId) {
                stopFollowing();
                return;
            }
        }
    }
}
