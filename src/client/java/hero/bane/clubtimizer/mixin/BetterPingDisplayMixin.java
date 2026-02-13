package hero.bane.clubtimizer.mixin;

import com.vladmarica.betterpingdisplay.hud.CustomPlayerListHud;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CustomPlayerListHud.class, remap = false)
public abstract class BetterPingDisplayMixin {

    @Inject(method = "renderPingDisplay", at = @At("HEAD"), cancellable = true)
    private static void club$fixBetterPing(
            Minecraft client,
            PlayerTabOverlay instance,
            GuiGraphics graphics,
            int width,
            int x,
            int y,
            PlayerInfo entry,
            CallbackInfo ci
    ) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;

        int parsedPing = -1;
        if (entry.getTabListDisplayName() != null) {
            parsedPing = PingUtil.parseTablistPing(entry.getTabListDisplayName().getString());
        }

        int latency = parsedPing >= 0 ? parsedPing : entry.getLatency();
        if (latency < 0 || latency == 1 || latency >= 1000) {
            ci.cancel();
        }
    }
}
