package hero.bane.mixin;

import com.vladmarica.betterpingdisplay.hud.CustomPlayerListHud;
import hero.bane.Clubtimizer;
import hero.bane.util.PingUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomPlayerListHud.class, remap = false)
public abstract class BetterPingDisplayMixin {

    @Inject(method = "renderPingDisplay", at = @At("HEAD"), cancellable = true)
    private static void club$fixBetterPing(MinecraftClient client, PlayerListHud instance, DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (Clubtimizer.ip == null || !(Clubtimizer.ip.contains("mcpvp"))) {
            return;
        }
        int parsedPing = -1;
        if (entry.getDisplayName() != null) {
            parsedPing = PingUtil.parsePing(entry.getDisplayName().getString());
        }
        int latency = parsedPing >= 0 ? parsedPing : entry.getLatency();
        if ((latency < 0 || latency == 1 || latency >= 1000)) {
            ci.cancel();
        }
    }
}
