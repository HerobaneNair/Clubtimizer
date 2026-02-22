package hero.bane.clubtimizer;

import hero.bane.clubtimizer.auto.Rematch;
import hero.bane.clubtimizer.auto.Requeue;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.auto.Totem;
import hero.bane.clubtimizer.command.ClubtimizerCommand;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Clubtimizer implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("clubtimizer");
    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static String ip = "_";
    public static Minecraft client;
    public static LocalPlayer player;
    public static String playerName = "";
    public static boolean hasBlindness = false;

    @Override
    public void onInitializeClient() {
        client = Minecraft.getInstance();
        player = client.player;
        if (player != null) playerName = player.getName().getString();

        ClubtimizerCommand.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraft) -> updateIp(minecraft));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> {
            MCPVPStateChanger.update();
            ip = "_";
        });

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            Spectator.handleTick();
            Requeue.handleTick(minecraft);
//            Totem.handleTick(minecraft);

            if (minecraft.player != null && minecraft.level != null) {
                long t = minecraft.level.getGameTime();
                if (MCPVPStateChanger.get() != MCPVPState.NONE) {
                    if (t % 5 == 0) MCPVPStateChanger.update();
                    handleBlindness();
                } else {
                    if (t % 20 == 0) MCPVPStateChanger.update();
                    hasBlindness = false;
                }
            }
        });
        Totem.totemCounterLoaded = FabricLoader.getInstance().isModLoaded("totemcounter");
    }

    private static void updateIp(Minecraft minecraft) {
        MCPVPStateChanger.update();

        var entry = minecraft.getCurrentServer();
        if (entry != null) {
            ip = entry.ip.toLowerCase();
            player = minecraft.player;
            if (player != null) playerName = player.getName().getString();

            if (ip.contains("mcpvp") && ClubtimizerConfig.getLobby().hitboxes) {
                LOGGER.info("[Clubtimizer] Connected to: {}", ip);
                if (!minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
                    minecraft.debugEntries.toggleStatus(DebugScreenEntries.ENTITY_HITBOXES);
                }
            }
        } else if (minecraft.isLocalServer()) {
            ip = "_sp";
        } else {
            ip = "_";
        }
    }

    private static void handleBlindness() {
        hasBlindness = player.hasEffect(MobEffects.BLINDNESS);
        if (MCPVPStateChanger.inGame()) {
            Totem.resetPops();
            if (MCPVPStateChanger.get() == MCPVPState.BLUE || MCPVPStateChanger.get() == MCPVPState.RED) {
                Rematch.sendRematchMessage();
            }
        }
    }
}
