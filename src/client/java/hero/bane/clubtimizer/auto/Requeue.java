package hero.bane.clubtimizer.auto;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PingUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;

import java.util.ArrayDeque;
import java.util.Deque;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public final class Requeue {

    private static final int LEAVE_COOLDOWN_TICKS = 20;
    private static final String MENU_KEYWORD = "Queue Duels";

    public static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};
    public static final String[] GAMEMODES = {"Sword", "Axe", "Mace", "UHC", "Neth OP", "Pot", "SMP", "Vanilla"};

    private enum State { IDLE, WAITING_MENU, CLICKING }
    private static State state = State.IDLE;

    private static long lastLeaveTick = -LEAVE_COOLDOWN_TICKS;

    private static boolean awaitingServerUpdate = false;
    private static int lastStateId = -1;
    private static long clickReadyTick = 0;

    private static final Deque<Integer> targetSlots = new ArrayDeque<>();
    private static final Deque<String> targetNames = new ArrayDeque<>();

    private static boolean attackHeldGuard = false;

    private static String cachedOrder = "";
    private static int[] cachedTargets = SLOTS.clone();
    private static String[] cachedNames = GAMEMODES.clone();

    private static final MutableComponent qPrefix =
            Component.literal("Queued").withStyle(ChatFormatting.AQUA);

    private static String expectedMenuTitle = "";

    public static void handleTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) return;

        long tick = client.level.getGameTime();
        boolean attackPressed = client.options.keyAttack.isDown();

        if (!attackPressed) attackHeldGuard = false;

        switch (state) {
            case IDLE -> onIdle(client, tick, attackPressed);
            case WAITING_MENU -> onWaitingMenu(client, tick);
            case CLICKING -> onClicking(client, tick);
        }
    }

    private static void onIdle(Minecraft client, long tick, boolean attackPressed) {
        if (!attackPressed || attackHeldGuard) return;
        if (!isSwordTrigger(client)) return;

        attackHeldGuard = true;

        MCPVPState pvpState = MCPVPStateChanger.get();

        if (pvpState == MCPVPState.LOBBY) {
            say("Opening queue menu");
            rightClickSword(client);
            state = State.WAITING_MENU;
            return;
        }

        if (pvpState == MCPVPState.IN_QUEUE) {
            if (tick - lastLeaveTick >= LEAVE_COOLDOWN_TICKS) {
                say("Leaving queue");
                ChatUtil.chat("/leave");
                lastLeaveTick = tick;
            }
        }
    }

    private static void onWaitingMenu(Minecraft client, long tick) {
        AbstractContainerScreen<?> screen = getContainerScreen(client);
        if (screen == null) return;

        String title = screen.getTitle().getString();
        if (!title.contains(MENU_KEYWORD)) return;

        expectedMenuTitle = title;
        initClickSequence();

        lastStateId = screen.getMenu().getStateId();
        clickReadyTick = tick + Math.max(1, PingUtil.getDynamicDelay(client, 1));
        awaitingServerUpdate = false;

        state = State.CLICKING;
    }

    private static void onClicking(Minecraft client, long tick) {
        AbstractContainerScreen<?> screen = getContainerScreen(client);
        if (screen == null) {
            say("Stopping Clicking cause exited gui", 0xFFFFAA);
            reset();
            return;
        }

        if (!screen.getTitle().getString().equals(expectedMenuTitle)) {
            say("Stopping Clicking cause changed gui", 0xFFAAAA);
            reset();
            return;
        }

        int stateId = screen.getMenu().getStateId();

        if (awaitingServerUpdate) {
            if (stateId != lastStateId) {
                awaitingServerUpdate = false;
                clickReadyTick = tick + 1;
            }
            lastStateId = stateId;
            return;
        }

        lastStateId = stateId;

        if (tick < clickReadyTick) return;

        if (targetSlots.isEmpty()) {
            reset();
            return;
        }

        int slot = targetSlots.pollFirst();
        String name = targetNames.pollFirst();

        clickSlot(client, screen.getMenu().containerId, slot);
        say(qPrefix.copy().append(" ").append(TextUtil.rainbowGradient(name)));

        awaitingServerUpdate = true;
    }

    private static void reset() {
        awaitingServerUpdate = false;
        state = State.IDLE;
    }

    private static boolean isSwordTrigger(Minecraft client) {
        Player player = client.player;
        if (player == null) return false;

        if (isInsideCylinder(player)) return false;
        if (player.getInventory().getSelectedSlot() != 0) return false;

        ItemStack stack = player.getMainHandItem();
        return stack.is(Items.IRON_SWORD) && stack.has(DataComponents.UNBREAKABLE);
    }

    private static void rightClickSword(Minecraft client) {
        Player player = client.player;
        if (player != null && client.gameMode != null) {
            player.getInventory().setSelectedSlot(0);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private static AbstractContainerScreen<?> getContainerScreen(Minecraft client) {
        Screen s = client.screen;
        return (s instanceof AbstractContainerScreen<?> hs && !(hs instanceof InventoryScreen)) ? hs : null;
    }

    private static void clickSlot(Minecraft client, int containerId, int slot) {
        if (client.gameMode != null && client.player != null) {
            client.gameMode.handleInventoryMouseClick(
                    containerId,
                    slot,
                    0,
                    ClickType.QUICK_MOVE,
                    client.player
            );
        }
    }

    private static void initClickSequence() {
        parseOrder();
        targetSlots.clear();
        targetNames.clear();
        for (int i = 0; i < cachedTargets.length; i++) {
            targetSlots.addLast(cachedTargets[i]);
            targetNames.addLast(cachedNames[i]);
        }
    }

    private static void parseOrder() {
        String order = ClubtimizerConfig.getRequeueOrder();
        if (order == null) order = "";

        if (order.equals(cachedOrder)) return;

        if (order.isEmpty()) {
            cachedTargets = SLOTS.clone();
            cachedNames = GAMEMODES.clone();
            cachedOrder = "";
            return;
        }

        int len = order.length();
        int[] slots = new int[len];
        String[] names = new String[len];
        int out = 0;

        for (int i = 0; i < len; i++) {
            int idx = order.charAt(i) - '1';
            if (idx >= 0 && idx < SLOTS.length) {
                slots[out] = SLOTS[idx];
                names[out] = GAMEMODES[idx];
                out++;
            }
        }

        if (out == 0) {
            cachedTargets = SLOTS.clone();
            cachedNames = GAMEMODES.clone();
            cachedOrder = "";
            return;
        }

        cachedTargets = new int[out];
        cachedNames = new String[out];
        System.arraycopy(slots, 0, cachedTargets, 0, out);
        System.arraycopy(names, 0, cachedNames, 0, out);
        cachedOrder = order;
    }

    public static int handleRequeue(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "order");

        if (!isValidSequence(input)) {
            say("Invalid: use unique digits 1–8", 0xFF5555, true);
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            sb.append(GAMEMODES[input.charAt(i) - '1']).append("\n");
        }

        ClubtimizerConfig.setRequeueOrder(input);
        say("Requeue order set:");
        say(sb.toString().trim(), 0xFFAA55, false);
        return 1;
    }

    private static boolean isValidSequence(String input) {
        int len = input.length();
        if (len == 0 || len > GAMEMODES.length) return false;

        boolean[] seen = new boolean[GAMEMODES.length];
        for (int i = 0; i < len; i++) {
            int idx = input.charAt(i) - '1';
            if (idx < 0 || idx >= GAMEMODES.length || seen[idx]) return false;
            seen[idx] = true;
        }
        return true;
    }

    public static boolean isInsideCylinder(Player p) {
        double px = p.getX() - 0.5;
        double pz = p.getZ() - 63.5;
        double py = p.getY();
        return px * px + pz * pz <= 361 && py >= 99 && py <= 125;
    }
}
