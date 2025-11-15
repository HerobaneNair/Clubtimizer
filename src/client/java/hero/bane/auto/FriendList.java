package hero.bane.auto;

import hero.bane.mixin.accessor.ClientCommandSourceAccessor;
import net.minecraft.client.MinecraftClient;

import java.util.Collections;
import java.util.List;

public class FriendList {

    public static int pendingId = -1;
    public static List<String> cached = Collections.emptyList();

    public static void request() {
        var client = MinecraftClient.getInstance();
        if (client == null) return;

        var nh = client.getNetworkHandler();
        if (nh == null) return;

        var src = nh.getCommandSource();
        ((ClientCommandSourceAccessor) src).club$requestFriendList();
    }

    public static void complete(List<String> names) {
        cached = names;
        pendingId = -1;
    }
}
