package hero.bane.clubtimizer.util;

import hero.bane.clubtimizer.Clubtimizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FriendUtil {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static int lastRequestId = -1;

    private static final Set<String> FRIENDS = new HashSet<>();
    private static final String TARGET = "/friend remove ";

    private static int nextRequestId() {
        int id = NEXT_ID.getAndIncrement();
        if (id == Integer.MAX_VALUE) NEXT_ID.set(1);
        return id;
    }

    public static void requestUpdate() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null || c.getNetworkHandler() == null) return;

        int id = nextRequestId();
        lastRequestId = id;

        c.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(id, TARGET));
    }

    public static boolean isOurRequest(int id) {
        return id == lastRequestId;
    }

    public static void replaceAll(List<String> list) {
        FRIENDS.clear();
        for (String s : list) FRIENDS.add(s.toLowerCase());
    }

    public static Set<String> get() {
        return FRIENDS;
    }

    private static String extractName(String raw) {
        if (raw == null) return "";
        raw = raw.strip();
        if (raw.isEmpty()) return raw;
        char c = raw.charAt(0);
        if (!Character.isLetterOrDigit(c)) raw = raw.substring(1).strip();
        return raw;
    }

    public static boolean isSelfOrFriend(String raw) {
        String name = extractName(raw);
        String low = name.toLowerCase();
        if (low.equals(Clubtimizer.playerName.toLowerCase())) return true;
        return FRIENDS.contains(low);
    }

    public static boolean isFriend(String raw) {
        String name = extractName(raw).toLowerCase();
        return FRIENDS.contains(name);
    }
}
