package hero.bane.auto;

import hero.bane.Clubtimizer;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.ChatUtil;
import hero.bane.util.PingUtil;
import hero.bane.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Spectator {

    private static final List<String> aliveList = new ArrayList<>();
    private static long lastTick = -1L;

    private static PlayerEntity followed = null;
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

    public static PlayerListEntry findEntryByCleanName(String cleanName) {
        if (Clubtimizer.client == null || Clubtimizer.client.getNetworkHandler() == null)
            return null;
        for (PlayerListEntry e : Clubtimizer.client.getNetworkHandler().getListedPlayerListEntries()) {
            if (e.getDisplayName() == null) continue;
            String stripped = TextUtil.stripFormatting(TextUtil.toLegacyString(e.getDisplayName())).trim();
            String[] parts = stripped.split(" ");
            if (parts.length >= 2 && parts[1].equals(cleanName)) return e;
        }
        return null;
    }

    public static boolean isFollowing(Entity e) {
        return followed != null && followed == e;
    }

    public static void startFollowing(Entity target) {
        if (!(target instanceof PlayerEntity p)) return;
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

    private static String resolveClean(PlayerEntity p) {
        if (Clubtimizer.client == null || Clubtimizer.client.getNetworkHandler() == null)
            return p.getName().getString();
        PlayerListEntry entry = Clubtimizer.client.getNetworkHandler().getPlayerListEntry(p.getUuid());
        if (entry == null || entry.getDisplayName() == null)
            return p.getName().getString();

        String stripped = TextUtil.stripFormatting(TextUtil.toLegacyString(entry.getDisplayName())).trim();
        String[] parts = stripped.split(" ");
        return parts.length >= 2 ? parts[1] : p.getName().getString();
    }

    private static int getPing() {
        return PingUtil.parseScoreboardPing(Clubtimizer.client);
    }

    public static void tick() {
        MinecraftClient client = Clubtimizer.client;
        if (followed == null) return;
        if (client.player == null) {
            stopFollowing();
            return;
        }

        if (!MCPVPStateChanger.inSpec()) {
            stopFollowing();
            return;
        }

        if (client.options.sneakKey.isPressed()) {
            stopFollowing();
            return;
        }

        if (roundEnded) {
            stopFollowing();
            roundEnded = false;
            return;
        }

        String clean = resolveClean(followed);
        if (!aliveList.contains(clean)) {
            stopFollowing();
            return;
        }

        BlockPos pos = followed.getBlockPos();
        assert client.world != null;
        boolean inRender = client.world
                .getChunkManager()
                .isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);

        long now = System.currentTimeMillis();

        if (!inRender) {
            if (now - lastTeleport >= 1500L) {
                lastTeleport = now;
                ChatUtil.chat("/tp " + clean);

                double ping = getPing();
                followPauseUntil = now + (long)(ping * 2.5);

                client.execute(() -> {
                    BlockPos np = followed.getBlockPos();
                    boolean stillMissing =
                            !client.world.getChunkManager()
                                    .isChunkLoaded(np.getX() >> 4, np.getZ() >> 4);

                    if (stillMissing) stopFollowing();
                });
            }
            return;
        }

        if (now < followPauseUntil) {
            client.setCameraEntity(followed);
            return;
        }

        client.setCameraEntity(followed);
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

        boolean messageWorks = text.contains("⚔ Match Complete")
                || (text.contains("won the round") && text.contains("\uD83D\uDDE1"));

        if (messageWorks) {
            roundEnded = true;
        }
    }
}
