package hero.bane.clubtimizer.util;

import hero.bane.clubtimizer.Clubtimizer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.TimeUnit;

import static hero.bane.clubtimizer.Clubtimizer.client;

public class ChatUtil {
    public static final Text PREFIX =
            Text.literal("[")
                    .styled(s -> s.withColor(0xAAAAAA)).append(
                            Text.literal("Club")
                                    .styled(s -> s.withColor(0xFFFFFF))).append(
                            Text.literal("timizer")
                                    .styled(s -> s.withColor(0xFF5555))).append(
                            Text.literal("] ")
                                    .styled(s -> s.withColor(0xAAAAAA)));

    private static boolean invalidClient() {
        return client == null || client.player == null || client.player.networkHandler == null;
    }

    public static void delayedSay(String message, int color, long delayMs) {
        delayedSay(message, color, true, delayMs);
    }

    public static void delayedSay(String message, int color) {
        delayedSay(message, color, true,100);
    }

    public static void delayedSay(String message, int color, boolean prepend, long delayMs) {
        Clubtimizer.executor.schedule(() -> say(message, color, prepend), delayMs, TimeUnit.MILLISECONDS);
    }


    public static void say(String message) {
        say(message, 0xFFFFFF, true);
    }

    public static void say(String message, int color) {
        say(message, color, true);
    }

    public static void say(String message, int color, boolean prepend) {
        if (invalidClient()) return;
        ClientPlayerEntity player = client.player;
        if (prepend) {
            player.sendMessage(PREFIX.copy().append(Text.literal(message).styled(s -> s.withColor(color))), false);
        } else {
            player.sendMessage(Text.literal(message).styled(s -> s.withColor(color)), false);
        }
    }

    public static void delayedSay(Text text, long delayMs) {
        Clubtimizer.executor.schedule(() -> say(text), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void delayedSay(Text text) {
        delayedSay(text, 100);
    }

    public static void delayedSay(Text text, boolean prepend, long delayMs) {
        Clubtimizer.executor.schedule(() -> say(text, prepend), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void delayedSay(Text text, boolean prepend) {
        delayedSay(text, prepend, 100);
    }

    public static void say(Text text) {
        say(text, true);
    }

    public static void say(Text text, boolean prepend) {
        if (invalidClient()) return;

        ClientPlayerEntity player = client.player;
        if (prepend) {
            player.sendMessage(PREFIX.copy().append(text.copy()), false);
        } else {
            player.sendMessage(text.copy(), false);
        }
    }

    public static void chat(String message) {
        if (invalidClient()) return;
        if (message.charAt(0) == '/') {
            command((message));
        }
        client.player.networkHandler.sendChatMessage(message);
    }

    public static void delayedChat(String message, long delayMs) {
        Clubtimizer.executor.schedule(() -> chat(message), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void delayedChat(String message) {
        delayedChat(message, 100);
    }

    private static void command(String message) {
        if (invalidClient()) return;
        if (message.charAt(0) == '/') {
            message = message.substring(1);
        }
        Clubtimizer.player.networkHandler.sendCommand(message);
    }
}