package hero.bane;

import hero.bane.auto.Requeue;
import hero.bane.auto.Spectator;
import hero.bane.auto.TotemResetter;
import hero.bane.command.ClubtimizerCommand;
import hero.bane.config.ClubtimizerConfig;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Clubtimizer implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("clubtimizer");
    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static String ip = "_";
    public static int temp1 = 0;
    public static MinecraftClient client;
    public static ClientPlayerEntity player;
    public static String playerName = "";

    @Override
    public void onInitializeClient() {
        client = MinecraftClient.getInstance();
        player = client.player;
        if (player != null) playerName = player.getName().getString();
        ClubtimizerCommand.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, c) -> updateIp(c));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, c) -> {
            MCPVPStateChanger.update();
            ip = "_";
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Spectator.handleTick();
            Requeue.handleTick(client);
            if (client.player != null && client.world != null) {
                long t = client.world.getTime();
                if (MCPVPStateChanger.get() != MCPVPState.NONE) {
                    if (t % 5 == 0) MCPVPStateChanger.update();
                } else {
                    if (t % 20 == 0) MCPVPStateChanger.update();
                }
            }
        });
        TotemResetter.initReflection();
    }

    private static void updateIp(MinecraftClient c) {
        MCPVPStateChanger.update();
        var entry = c.getCurrentServerEntry();
        if (entry != null) {
            ip = entry.address.toLowerCase();
            player = c.player;
            if (player != null) playerName = player.getName().getString();
            if (ip.contains("mcpvp") && ClubtimizerConfig.getLobby().hitboxes) {
                LOGGER.info("[Clubtimizer] Connected to: {}", ip);
                c.getEntityRenderDispatcher().setRenderHitboxes(true);
            }
        } else if (c.isIntegratedServerRunning()) {
            ip = "_sp";
        } else {
            ip = "_";
        }
    }
}
