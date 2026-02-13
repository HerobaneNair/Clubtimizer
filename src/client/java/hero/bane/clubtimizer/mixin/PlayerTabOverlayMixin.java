package hero.bane.clubtimizer.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.auto.Tablist;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PingUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Unique
    private static final int TAB_MAX_RENDERED = 80;

    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @Unique
    private long lastTick = -1L;

    @Unique
    private int gatedTabSize = 0;

    @Inject(method = "render", at = @At("HEAD"))
    private void club$processTablist(
            GuiGraphics graphics,
            int width,
            Scoreboard scoreboard,
            @Nullable Objective objective,
            CallbackInfo ci
    ) {
        if (!(MCPVPStateChanger.inGame()
                || MCPVPStateChanger.get() == MCPVPState.SPECTATING)) {
            gatedTabSize = 0;
            return;
        }

        assert Clubtimizer.client.level != null;
        long tick = Clubtimizer.client.level.getGameTime();
        if (tick == lastTick) return;
        lastTick = tick;

        List<PlayerInfo> list = this.getPlayerInfos();
        if (list.isEmpty()) {
            gatedTabSize = 0;
            return;
        }

        gatedTabSize = Math.min(list.size(), TAB_MAX_RENDERED);
        Tablist.process(list.subList(0, gatedTabSize), tick);
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void club$fixTabPings(
            GuiGraphics graphics,
            int width,
            int x,
            int y,
            PlayerInfo entry,
            CallbackInfo ci
    ) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;

        List<PlayerInfo> list = this.getPlayerInfos();
        int index = list.indexOf(entry);

        if (index < 0 || index >= gatedTabSize) {
            ci.cancel();
            return;
        }

        int latency = entry.getLatency();
        if (latency < 0 || latency == 1 || latency >= 1000) {
            ci.cancel();
            return;
        }

        if (Tablist.noBetterPing) {
            ci.cancel();

            String text = latency + "ms";
            int tw = Clubtimizer.client.font.width(text);

            graphics.drawString(
                    Clubtimizer.client.font,
                    text,
                    x + width - tw - 1,
                    y,
                    PingUtil.getPingColor(latency),
                    true
            );
        }
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "CONSTANT", args = "intValue=13")
    )
    private int club$widenForPingText(int original) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return original;
        if (FabricLoader.getInstance().isModLoaded("betterpingdisplay")) return original;
        return Clubtimizer.client.font.width("xxxxms");
    }
}
