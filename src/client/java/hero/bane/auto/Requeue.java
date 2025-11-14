package hero.bane.auto;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.Clubtimizer;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.TextUtil;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static hero.bane.util.ChatUtil.say;

public final class Requeue {

    private static final int TICK_BUFFER = 2;
    private static final int LEAVE_COOLDOWN_TICKS = 20;
    private static final String MENU_KEYWORD = "Queue Duels";

    public static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};
    public static final String[] GAMEMODES = {"Sword", "Axe", "Mace", "UHC", "Neth OP", "Pot", "SMP", "Vanilla"};

    private enum State {
        IDLE, WAITING, CLICKING
    }

    private static State state = State.IDLE;
    private static long nextActionTick = 0;
    private static long lastLeaveTick = -LEAVE_COOLDOWN_TICKS;
    private static int delayTicks = 1;

    private static final Deque<Integer> targetSlots = new ArrayDeque<>();
    private static final Deque<String> targetNames = new ArrayDeque<>();
    private static String expectedMenuTitle = "";

    private static boolean attackHeldGuard = false;

    private static String cachedOrder = null;
    private static int[] cachedTargets = new int[0];
    private static String[] cachedNames = new String[0];

    private static final List<Integer> slotBuf = new ArrayList<>(SLOTS.length);
    private static final List<String> modeBuf = new ArrayList<>(SLOTS.length);

    private static final MutableText qPrefix = Text.literal("Queued into:").styled(s -> s.withColor(0x55FFFF));

    public static void handleTick(MinecraftClient client) {
        if (client == null) return;
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        final long tick = world.getTime();
        final boolean attackPressed = client.options.attackKey.isPressed();
        if (!attackPressed) attackHeldGuard = false;

        switch (state) {
            case IDLE -> onIdle(client, tick, attackPressed);
            case WAITING -> onAwaitingMenu(client, tick);
            case CLICKING -> onClicking(client, tick);
        }
    }

    private static void onIdle(MinecraftClient client, long tick, boolean attackPressed) {
        if (!attackPressed || attackHeldGuard) return;
        if (!isClickingUnbreakableIronSwordInSlot0(client)) return;

        attackHeldGuard = true;
        final MCPVPState pvpState = MCPVPStateChanger.get();

        if (pvpState == MCPVPState.LOBBY) {
            rightClickSword(client);
            delayTicks = Math.max(1, TextUtil.getDynamicDelay(client, TICK_BUFFER));
            nextActionTick = tick + (long) delayTicks * 2;
            state = State.WAITING;
        } else if (pvpState == MCPVPState.IN_QUEUE) {
            if (tick - lastLeaveTick >= LEAVE_COOLDOWN_TICKS) {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand("leave");
                    lastLeaveTick = tick;
                }
            }
        }
    }

    private static void onAwaitingMenu(MinecraftClient client, long tick) {
        final HandledScreen<?> hs = getHandledNonInventoryScreen(client);
        if (hs != null) {
            final String title = safeTitle(hs);
            if (title.contains(MENU_KEYWORD)) {
                expectedMenuTitle = title;
                initClickSequence();
                state = State.CLICKING;
                return;
            }
        }

        if (tick >= nextActionTick) {
            Clubtimizer.LOGGER.info("Timeout waiting for inventory ({} ticks).", delayTicks * 2);
            state = State.IDLE;
        }
    }

    private static void onClicking(MinecraftClient client, long tick) {
        if (tick < nextActionTick) return;

        final HandledScreen<?> hs = getHandledNonInventoryScreen(client);
        if (hs == null) {
            state = State.IDLE;
            return;
        }

        final String title = safeTitle(hs);
        if (!title.equals(expectedMenuTitle)) {
            Clubtimizer.LOGGER.warn("Stopped clicking - unexpected title '{}'.", title);
            state = State.IDLE;
            return;
        }

        if (targetSlots.isEmpty()) {
            state = State.IDLE;
            return;
        }

        final int syncId = hs.getScreenHandler().syncId;
        final Integer slot = targetSlots.pollFirst();
        final String name = (targetNames.isEmpty() ? ("Slot " + slot) : targetNames.pollFirst());

        if (slot != null) {
            clickSlot(client, syncId, slot);
            say(
                    qPrefix.copy().append(TextUtil.rainbowGradient(" " + name))
            );
        }

        nextActionTick = tick + (long) delayTicks;
    }

    private static boolean isClickingUnbreakableIronSwordInSlot0(MinecraftClient client) {
        if (client.player == null) return false;
        if (isInsideCylinder(client.player)) return false;
        if (!client.options.attackKey.isPressed()) return false;
        if (client.player.getInventory().getSelectedSlot() != 0) return false;

        final ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() != Items.IRON_SWORD) return false;
        return stack.contains(DataComponentTypes.UNBREAKABLE);
    }

    private static void rightClickSword(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        client.player.getInventory().setSelectedSlot(0);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }

    private static HandledScreen<?> getHandledNonInventoryScreen(MinecraftClient client) {
        Screen s = client.currentScreen;
        return (s instanceof HandledScreen<?> hs && !(s instanceof InventoryScreen)) ? hs : null;
    }

    private static String safeTitle(HandledScreen<?> hs) {
        final Text t = hs.getTitle();
        return (t == null) ? "" : t.getString();
    }

    private static void clickSlot(MinecraftClient client, int syncId, int slot) {
        if (client.interactionManager == null || client.player == null) return;
        client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
    }

    private static void initClickSequence() {
        parseOrderFromConfigIfNeeded();
        targetSlots.clear();
        targetNames.clear();
        for (int i = 0; i < cachedTargets.length; i++) {
            targetSlots.addLast(cachedTargets[i]);
            targetNames.addLast(cachedNames[i]);
        }
    }

    private static void parseOrderFromConfigIfNeeded() {
        final String order = ClubtimizerConfig.getRequeueOrder();
        if (order != null && order.equals(cachedOrder)) return;

        if (order == null || order.isEmpty()) {
            cachedTargets = SLOTS.clone();
            cachedNames = GAMEMODES.clone();
            cachedOrder = "";
            return;
        }

        slotBuf.clear();
        modeBuf.clear();

        for (int i = 0; i < order.length(); i++) {
            char c = order.charAt(i);
            int idx = c - '1';
            if (idx >= 0 && idx < SLOTS.length) {
                slotBuf.add(SLOTS[idx]);
                modeBuf.add(GAMEMODES[idx]);
            }
        }

        if (slotBuf.isEmpty()) {
            cachedTargets = SLOTS.clone();
            cachedNames = GAMEMODES.clone();
            cachedOrder = "";
        } else {
            cachedTargets = slotBuf.stream().mapToInt(i -> i).toArray();
            cachedNames = modeBuf.toArray(new String[0]);
            cachedOrder = order;
        }
    }

    public static int handleRequeue(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "order");
        if (!isValidSequence(input)) {
            say("Invalid, has to be unique digits and 1-8", 0xFF5555, true);
            return 0;
        }
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            int index = Character.getNumericValue(input.charAt(i)) - 1;
            summary.append(GAMEMODES[index]).append("\n");
        }
        ClubtimizerConfig.setRequeueOrder(input);
        say("You selected");
        say(summary.toString().trim(), 0xFFAA55, false);
        return 1;
    }

    private static boolean isValidSequence(String input) {
        int n = GAMEMODES.length;
        if (input.isEmpty() || input.length() > n) return false;
        boolean[] seen = new boolean[n];
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < '1' || c > (char) ('0' + n)) return false;
            int idx = c - '1';
            if (seen[idx]) return false;
            seen[idx] = true;
        }
        return true;
    }

    public static boolean isInsideCylinder(PlayerEntity p) {
        double px = p.getX() - 0.5;
        double pz = p.getZ() - 63.5;
        double py = p.getY();

        return (px * px + pz * pz <= 19 * 19) && (py >= 99 && py <= 125);
    }
}
