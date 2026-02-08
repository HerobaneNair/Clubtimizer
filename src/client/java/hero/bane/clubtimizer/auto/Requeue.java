package hero.bane.clubtimizer.auto;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PingUtil;
import hero.bane.clubtimizer.util.TextUtil;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.Deque;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public final class Requeue {
    private static final int LEAVE_COOLDOWN_TICKS = 20;
    private static final String MENU_KEYWORD = "Queue Duels";

    public static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};
    public static final String[] GAMEMODES = {"Sword", "Axe", "Mace", "UHC", "Neth OP", "Pot", "SMP", "Vanilla"};

    private enum State {IDLE, WAITING, CLICKING}
    private static State state = State.IDLE;

    private static long lastLeaveTick = -LEAVE_COOLDOWN_TICKS;
    private static boolean awaitingNextScreen = false;
    private static int lastSyncId = -1;
    private static long clickReadyTick = 0;

    private static final Deque<Integer> targetSlots = new ArrayDeque<>();
    private static final Deque<String> targetNames = new ArrayDeque<>();

    private static boolean attackHeldGuard = false;

    private static String cachedOrder = "";
    private static int[] cachedTargets = SLOTS.clone();
    private static String[] cachedNames = GAMEMODES.clone();

    private static final MutableText qPrefix =
            Text.literal("Tried to queue in").styled(s -> s.withColor(Formatting.AQUA));

    private static String expectedMenuTitle = "";

    public static void handleTick(MinecraftClient client) {
        var player = client.player;
        var world = client.world;
        if (player == null || world == null) return;

        long tick = world.getTime();
        boolean attackPressed = client.options.attackKey.isPressed();

        if (!attackPressed) attackHeldGuard = false;

        switch (state) {
            case IDLE -> onIdle(client, tick, attackPressed);
            case WAITING -> onAwaitingMenu(client, tick);
            case CLICKING -> onClicking(client, tick);
        }
    }

    private static void onIdle(MinecraftClient client, long tick, boolean attackPressed) {
        if (!attackPressed || attackHeldGuard) return;
        if (!isSwordTrigger(client)) return;

        attackHeldGuard = true;

        MCPVPState pvpState = MCPVPStateChanger.get();

        switch (pvpState) {
            case LOBBY -> {
                rightClickSword(client);
                state = State.WAITING;
            }
            case IN_QUEUE -> {
                if (tick - lastLeaveTick >= LEAVE_COOLDOWN_TICKS) {
                    assert client.player != null;
                    var net = client.player.networkHandler;
                    if (net != null) {
                        net.sendChatCommand("leave");
                        lastLeaveTick = tick;
                    }
                }
            }
        }
    }

    private static void onAwaitingMenu(MinecraftClient client, long tick) {
        HandledScreen<?> hs = getHandledMenu(client);
        if (hs != null && hs.getTitle().getString().contains(MENU_KEYWORD)) {
            expectedMenuTitle = hs.getTitle().getString();
            initClickSequence();
            awaitingNextScreen = false;
            lastSyncId = hs.getScreenHandler().syncId;
            long delay = PingUtil.getDynamicDelay(client, 1);
            if (delay < 1) delay = 1;
            clickReadyTick = tick + delay;
            state = State.CLICKING;
        }
    }

    private static void onClicking(MinecraftClient client, long tick) {
        HandledScreen<?> hs = getHandledMenu(client);
        if (hs == null) {
            resetToIdle();
            return;
        }

        int syncId = hs.getScreenHandler().syncId;
        String title = hs.getTitle().getString();

        if (awaitingNextScreen) {
            if (syncId != lastSyncId) {
                awaitingNextScreen = false;
                clickReadyTick = tick + 1;
            }
            lastSyncId = syncId;
            return;
        }

        lastSyncId = syncId;

        if (tick < clickReadyTick) return;

        if (!title.equals(expectedMenuTitle)) {
            resetToIdle();
            return;
        }

        if (targetSlots.isEmpty()) {
            resetToIdle();
            return;
        }

        int slot = targetSlots.pollFirst();
        String name = targetNames.pollFirst();

        clickSlot(client, syncId, slot);
        say(qPrefix.copy().append(TextUtil.rainbowGradient(" " + name))); //makes sure no null problems

        awaitingNextScreen = true;
    }

    private static void resetToIdle() {
        awaitingNextScreen = false;
        state = State.IDLE;
    }

    private static boolean isSwordTrigger(MinecraftClient client) {
        var player = client.player;
        if (player == null) return false;

        if (isInsideCylinder(player)) return false;
        if (player.getInventory().getSelectedSlot() != 0) return false;

        ItemStack stack = player.getMainHandStack();
        return stack.getItem() == Items.IRON_SWORD &&
                stack.contains(DataComponentTypes.UNBREAKABLE);
    }

    private static void rightClickSword(MinecraftClient client) {
        var player = client.player;
        var im = client.interactionManager;
        if (player != null && im != null) {
            player.getInventory().setSelectedSlot(0);
            im.interactItem(player, Hand.MAIN_HAND);
        }
    }

    private static HandledScreen<?> getHandledMenu(MinecraftClient client) {
        Screen s = client.currentScreen;
        return (s instanceof HandledScreen<?> hs && !(hs instanceof InventoryScreen)) ? hs : null;
    }

    private static void clickSlot(MinecraftClient client, int syncId, int slot) {
        var im = client.interactionManager;
        var player = client.player;
        if (im != null && player != null) {
            im.clickSlot(syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
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
        int[] outSlots = new int[len];
        String[] outNames = new String[len];
        int outIndex = 0;

        for (int i = 0; i < len; i++) {
            int idx = order.charAt(i) - '1';
            if (idx >= 0 && idx < SLOTS.length) {
                outSlots[outIndex] = SLOTS[idx];
                outNames[outIndex] = GAMEMODES[idx];
                outIndex++;
            }
        }

        if (outIndex == 0) {
            cachedTargets = SLOTS.clone();
            cachedNames = GAMEMODES.clone();
            cachedOrder = "";
        } else {
            cachedTargets = new int[outIndex];
            System.arraycopy(outSlots, 0, cachedTargets, 0, outIndex);

            cachedNames = new String[outIndex];
            System.arraycopy(outNames, 0, cachedNames, 0, outIndex);

            cachedOrder = order;
        }
    }

    public static int handleRequeue(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "order");

        if (!isValidSequence(input)) {
            say("Invalid, has to be unique digits and 1-8", 0xFF5555, true);
            return 0;
        }

        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < input.length(); i++) {
            sb.append(GAMEMODES[input.charAt(i) - '1']).append("\n");
        }

        ClubtimizerConfig.setRequeueOrder(input);
        say("You selected");
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

    public static boolean isInsideCylinder(PlayerEntity p) {
        double px = p.getX() - 0.5;
        double pz = p.getZ() - 63.5;
        double py = p.getY();
        return px * px + pz * pz <= 361 && py >= 99 && py <= 125;
    }
}
