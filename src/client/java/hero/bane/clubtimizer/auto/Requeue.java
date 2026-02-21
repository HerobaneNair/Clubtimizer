package hero.bane.clubtimizer.auto;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.PlayerUtil;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;

import static hero.bane.clubtimizer.util.ChatUtil.say;

public final class Requeue {

    private static final int LEAVE_COOLDOWN_TICKS = 20;
    private static final String MENU_KEYWORD = "Queue Duels";
    private static final int MAX_DURATION_TICKS = 240;

    public static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};
    public static final String[] GAMEMODES = {"Sword", "Axe", "Mace", "UHC", "Neth OP", "Pot", "SMP", "Vanilla"};

    private enum State {IDLE, WAITING_MENU, ACTIVE}

    private static State state = State.IDLE;

    private static long lastLeaveTick = -LEAVE_COOLDOWN_TICKS;
    private static boolean attackHeldGuard = false;

    private static String cachedOrder = "";
    private static int[] cachedTargets = SLOTS.clone();
    private static String[] cachedNames = GAMEMODES.clone();

    private static final int[] attemptCounts = new int[SLOTS.length];

    private static final MutableComponent qPrefix =
            Component.literal("Queued").withStyle(ChatFormatting.AQUA);

    private static String expectedMenuTitle = "";
    private static int expectedContainerId = -1;

    private static long startTick = 0;

    private static boolean containerDirty = false;

    public static void handleTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) return;

        long tick = client.level.getGameTime();
        boolean attackPressed = client.options.keyAttack.isDown() &&
                (MCPVPStateChanger.get() == MCPVPState.IN_QUEUE || client.options.keyShift.isDown());

        if (!attackPressed) attackHeldGuard = false;

        switch (state) {
            case IDLE -> onIdle(client, tick, attackPressed);
            case WAITING_MENU -> onWaitingMenu(client, tick);
            case ACTIVE -> onActive(client, tick);
        }
    }

    private static void onIdle(Minecraft client, long tick, boolean attackPressed) {
        if (!attackPressed || attackHeldGuard) return;
        if (!isSwordTrigger(client)) return;

        attackHeldGuard = true;

        MCPVPState pvpState = MCPVPStateChanger.get();

        if (pvpState == MCPVPState.LOBBY) {
            rightClickSword(client);
            state = State.WAITING_MENU;
            return;
        }

        if (pvpState == MCPVPState.IN_QUEUE) {
            if (tick - lastLeaveTick >= LEAVE_COOLDOWN_TICKS) {
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
        expectedContainerId = screen.getMenu().containerId;

        parseOrder();

        Arrays.fill(attemptCounts, 0);

        startTick = tick;
        containerDirty = true;

        state = State.ACTIVE;
    }

    private static void onActive(Minecraft client, long tick) {
        AbstractContainerScreen<?> screen = getContainerScreen(client);
        if (isInvalid(client, screen)) return;

        if (tick - startTick > MAX_DURATION_TICKS) {
            say("Requeue timeout - resetting.", 0xFF5555);
            reset();
            return;
        }

        if (!containerDirty) return;
        containerDirty = false;

        boolean allQueued = true;

        for (int i = 0; i < cachedTargets.length; i++) {
            int slot = cachedTargets[i];
            String mode = cachedNames[i];

            if (!isQueued(screen, slot)) {
                allQueued = false;

                attemptCounts[i]++;

                say(
                        "Dirty: " + mode +
                                " Attempts: " + attemptCounts[i],
                        0xFFAA00
                );

                clickSlot(client, expectedContainerId, slot);
            } else {
                if (attemptCounts[i] != -1) {
                    say(
                            "Confirmed: " + mode +
                                    " Attempts: " + attemptCounts[i],
                            0x55FF55
                    );
                    attemptCounts[i] = -1;
                }
            }
        }

        if (allQueued) {
            say("All modes queued", 0x55FF55);
            reset();
        }
    }

    private static boolean isInvalid(Minecraft client, AbstractContainerScreen<?> screen) {
        Player player = client.player;
        if (player == null || client.level == null) {
            say("Nullscape");
            reset();
            return true;
        }

        if (player.hasEffect(MobEffects.BLINDNESS)) {
            say("Blind escape");
            reset();
            return true;
        }

        if (screen == null) {
            say("Screen nullscape");
            reset();
            return true;
        }

        if (expectedContainerId != screen.getMenu().containerId) {
            say("Container id invalid escape");
            reset();
            return true;
        }

        if (!screen.getTitle().getString().equals(expectedMenuTitle)) {
            say("Container menu title invalid escape");
            reset();
            return true;
        }

        return false;
    }

    private static boolean isQueued(AbstractContainerScreen<?> screen, int slot) {
        if (slot < 0 || slot >= screen.getMenu().slots.size()) return false;

        ItemStack stack = screen.getMenu().getSlot(slot).getItem();
        if (stack.isEmpty()) return false;

        Component name = stack.getHoverName();
        return name.getString().endsWith("(Queued)");
    }

    private static void reset() {
        state = State.IDLE;
        expectedMenuTitle = "";
        expectedContainerId = -1;
        containerDirty = false;
    }

    private static boolean isSwordTrigger(Minecraft client) {
        Player player = client.player;
        if (player == null) return false;

        if (PlayerUtil.inSumoArea(player)) return false;
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

    public static void containerChanged(int containerId) {
        if (state == State.ACTIVE && containerId == expectedContainerId) {
            containerDirty = true;
        }
    }

    public static int handleRequeue(CommandContext<FabricClientCommandSource> ctx) {
        String input = StringArgumentType.getString(ctx, "order");

        if (!isValidSequence(input)) {
            say("Invalid: use unique digits 1-8", 0xFF5555, true);
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
}