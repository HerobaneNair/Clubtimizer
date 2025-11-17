package hero.bane.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import hero.bane.Clubtimizer;
import hero.bane.auto.Tablist;
import hero.bane.util.PingUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "render", at = @At("HEAD"))
    private void club$processTablist(DrawContext ctx, int width, Scoreboard sb, @Nullable ScoreboardObjective obj, CallbackInfo ci) {
        if (!Tablist.goodState()) return;

        long tick = Clubtimizer.client.world != null ? Clubtimizer.client.world.getTime() : 0L;
        List<PlayerListEntry> list = this.collectPlayerEntries();
        Tablist.process(list, tick);
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void club$fixTabPings(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (Tablist.shouldntApply()) return;

        int latency = entry.getLatency();
        if (latency < 0 || latency == 1 || latency >= 1000) {
            ci.cancel();
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("betterpingdisplay")) {
            ci.cancel();

            String text = latency + "ms";
            int tw = Clubtimizer.client.textRenderer.getWidth(text);

            context.drawTextWithShadow(
                    Clubtimizer.client.textRenderer,
                    text,
                    x + width - tw - 1,
                    y,
                    PingUtil.getPingColor(latency)
            );
        }
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT", args = "intValue=13"))
    private int club$widenForPingText(int original) {
        if (Tablist.shouldntApply()) return original;
        if (FabricLoader.getInstance().isModLoaded("betterpingdisplay")) return original;
        return Clubtimizer.client.textRenderer.getWidth("xxxxms");
    }
}
