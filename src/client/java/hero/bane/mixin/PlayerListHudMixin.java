package hero.bane.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import hero.bane.mixin.accessor.PlayerListEntryAccessor;
import hero.bane.state.MCPVPState;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.PingUtil;
import hero.bane.util.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Unique
    private static boolean club$shouldApply() {
        var e = MinecraftClient.getInstance().getCurrentServerEntry();
        if (e == null) return true;
        String addr = e.address;
        return addr == null || !TextUtil.fastContains(addr, "mcpvp.club");
    }

    @Unique
    private static boolean club$goodState() {
        MCPVPState s = MCPVPStateChanger.get();
        return MCPVPStateChanger.inGame() || s == MCPVPState.SPECTATING;
    }

    @Unique
    private static String club$removeMs(String s) {
        if (!TextUtil.fastContains(s, "ms")) return s;
        int idx = s.indexOf("ms");
        if (idx < 0) return s;

        int start = idx - 1;
        while (start >= 0 && Character.isDigit(s.charAt(start))) start--;
        start++;

        int after = idx + 2;

        if (after >= s.length()) return s.substring(0, start).trim();
        if (start == 0) return s.substring(after).trim();

        return (s.substring(0, start) + s.substring(after)).trim();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void club$fixTabNames(DrawContext ctx, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        if (club$shouldApply() || !club$goodState()) return;

        List<PlayerListEntry> list = this.collectPlayerEntries();

        for (PlayerListEntry entry : list) {
            Text disp = entry.getDisplayName();
            if (disp == null) continue;

            String raw = disp.getString();
            if (!TextUtil.fastContains(raw, "ms")) continue;

            int ping = PingUtil.parsePing(raw);
            if (ping < 0) continue;

            ((PlayerListEntryAccessor) entry).setLatency(ping);

            String legacy = TextUtil.toLegacyString(disp);
            legacy = club$removeMs(legacy);
            entry.setDisplayName(TextUtil.fromLegacy(legacy));
        }
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void club$fixTabPings(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (club$shouldApply()) return;

        int latency = entry.getLatency();
        if (latency < 0 || latency == 1 || latency >= 1000) {
            ci.cancel();
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("betterpingdisplay")) {
            ci.cancel();
            MinecraftClient client = MinecraftClient.getInstance();
            String text = latency + "ms";
            int tw = client.textRenderer.getWidth(text);

            context.drawTextWithShadow(
                    client.textRenderer,
                    text,
                    x + width - tw - 1,
                    y,
                    PingUtil.getPingColor(latency)
            );
        }
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT", args = "intValue=13"))
    private int club$widenForPingText(int original) {
        if (club$shouldApply()) return original;
        if (FabricLoader.getInstance().isModLoaded("betterpingdisplay")) return original;
        return MinecraftClient.getInstance().textRenderer.getWidth("xxxxms");
    }
}
