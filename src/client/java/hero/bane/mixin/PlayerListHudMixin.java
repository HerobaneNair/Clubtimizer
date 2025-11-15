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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Unique
    private static final ThreadLocal<StringBuilder> BUF = ThreadLocal.withInitial(() -> new StringBuilder(64));

    @Unique
    private static boolean club$shouldApply() {
        var e = MinecraftClient.getInstance().getCurrentServerEntry();
        if (e == null) return true;
        String addr = e.address;
        return addr == null || !TextUtil.fastContains(addr,"mcpvp.club");
    }

    @Unique
    private static boolean club$goodState() {
        MCPVPState s = MCPVPStateChanger.get();
        return MCPVPStateChanger.inGame() || s == MCPVPState.SPECTATING;
    }

    @Unique
    private static boolean club$isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    @Unique
    private static String club$removeMs(String s) {
        int idx = s.indexOf("ms");
        if (idx < 0) return s;

        int start = idx - 1;
        while (start >= 0 && club$isDigit(s.charAt(start))) start--;
        start++;

        int after = idx + 2;

        if (start == 0) {
            if (after >= s.length()) return "";
            return s.substring(after).trim();
        }

        if (after >= s.length()) return s.substring(0, start).trim();

        return (s.substring(0, start) + s.substring(after)).trim();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void club$fixTabNames(DrawContext ctx, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        if (club$shouldApply() || !club$goodState()) return;

        final List<PlayerListEntry> list = this.collectPlayerEntries();

        for (PlayerListEntry entry : list) {
            Text disp = entry.getDisplayName();
            if (disp == null) continue;

            final String raw = disp.getString();

            int spaces = 0;
            for (int i = 0, len = raw.length(); i < len; i++) {
                if (raw.charAt(i) == ' ' && ++spaces > 2) break;
            }
            if (spaces != 2) continue;

            int ping = PingUtil.parsePing(raw);
            if (ping < 0) continue;

            ((PlayerListEntryAccessor) entry).setLatency(ping);

            final MutableText rebuilt = Text.empty();
            final StringBuilder sb = BUF.get();
            sb.setLength(0);

            disp.visit((style, data) -> {
                if (data == null || data.isEmpty()) return Optional.empty();

                String trimmed = club$removeMs(data);
                if (!trimmed.isEmpty()) {
                    rebuilt.append(Text.literal(trimmed).setStyle(style));
                }

                return Optional.empty();
            }, Style.EMPTY);

            entry.setDisplayName(rebuilt);
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