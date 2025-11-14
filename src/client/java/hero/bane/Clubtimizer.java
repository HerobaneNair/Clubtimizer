package hero.bane;

import hero.bane.auto.Requeue;
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
    public static String ip = "_";
    public static int temp1 = 0;
    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static MinecraftClient client;
    public static ClientPlayerEntity player;
    public static String playerName = "";


    @Override
    public void onInitializeClient() {
        client = MinecraftClient.getInstance();
        if(client!=null) {
            player = client.player;
            if (player != null) playerName = client.player.getName().getString();
        }
        ClubtimizerCommand.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> updateIp(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ip = "_");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Requeue.handleTick(client);

            if (client.player != null && client.world != null) {
                if (MCPVPStateChanger.get()!= MCPVPState.NONE) {
                    if (client.world.getTime() % 5 == 0) MCPVPStateChanger.update(); //update every 5 ticks
                } else {
                    if (client.world.getTime() % 20 == 0) MCPVPStateChanger.update();
                }
            }
        });
        TotemResetter.initializeReflection();
    }

    private static void updateIp(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) {
            ip = client.getCurrentServerEntry().address.toLowerCase();
            player = client.player;
            if(player!=null) playerName = client.player.getName().getString();
            if(ip.contains("mcpvp") && ClubtimizerConfig.getLobby().hitboxes) {
                LOGGER.info("[Clubtimizer] Connected to: {}", ip);
                client.getEntityRenderDispatcher().setRenderHitboxes(true);
            }
        } else if (client.isIntegratedServerRunning()) {
            ip = "_sp";
        } else {
            ip = "_";
        }
    }
}
