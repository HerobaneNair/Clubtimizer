package hero.bane.clubtimizer.util;

import hero.bane.clubtimizer.Clubtimizer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.concurrent.TimeUnit;

import static hero.bane.clubtimizer.Clubtimizer.client;

public class ChatUtil {

    public static final Component PREFIX =
            Component.literal("[")
                    .withStyle(s -> s.withColor(0xAAAAAA))
                    .append(Component.literal("Club")
                            .withStyle(s -> s.withColor(0xFFFFFF)))
                    .append(Component.literal("timizer")
                            .withStyle(s -> s.withColor(0xFF5555)))
                    .append(Component.literal("] ")
                            .withStyle(s -> s.withColor(0xAAAAAA)));

    private static boolean invalidClient() {
        return client == null || client.player == null;
    }

    public static void delayedSay(String message, int color, long delayMs) {
        delayedSay(message, color, true, delayMs);
    }

    public static void delayedSay(String message, int color) {
        delayedSay(message, color, true, 100);
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

        LocalPlayer player = client.player;
        Component msg = Component.literal(message).withStyle(s -> s.withColor(color));

        if (prepend) {
            player.displayClientMessage(PREFIX.copy().append(msg), false);
        } else {
            player.displayClientMessage(msg, false);
        }
    }

    public static void delayedSay(Component text, long delayMs) {
        Clubtimizer.executor.schedule(() -> say(text), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void delayedSay(Component text) {
        delayedSay(text, 100);
    }

    public static void delayedSay(Component text, boolean prepend, long delayMs) {
        Clubtimizer.executor.schedule(() -> say(text, prepend), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void delayedSay(Component text, boolean prepend) {
        delayedSay(text, prepend, 100);
    }

    public static void say(Component text) {
        say(text, true);
    }

    public static void say(Component text, boolean prepend) {
        if (invalidClient()) return;

        LocalPlayer player = client.player;
        if (prepend) {
            player.displayClientMessage(PREFIX.copy().append(text.copy()), false);
        } else {
            player.displayClientMessage(text.copy(), false);
        }
    }

    public static void chat(String message) {
        if (invalidClient()) return;

        if (message.charAt(0) == '/') {
            command(message);
            return;
        }

        client.player.connection.sendChat(message);
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

        Clubtimizer.player.connection.sendCommand(message);
    }
}
