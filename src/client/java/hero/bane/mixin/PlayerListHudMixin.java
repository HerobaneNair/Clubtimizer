package hero.bane.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import hero.bane.mixin.accessor.PlayerListEntryAccessor;
import hero.bane.state.MCPVPStateChanger;
import hero.bane.util.PingUtil;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Inject(method = "render", at = @At("HEAD"))
    private void club$fixTabNames(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() == null) return;
        String address = client.getCurrentServerEntry().address;
        if (address == null || !address.contains("mcpvp.club")) return;

        for (PlayerListEntry entry : this.collectPlayerEntries()) {
            Text display = entry.getDisplayName();
            if (display == null) continue;

            if (!MCPVPStateChanger.inGame()) continue;

            String raw = display.getString();
            if (raw.chars().filter(c -> c == ' ').count() != 2) continue;

            int parsed = PingUtil.parsePing(raw);
            if (parsed >= 0) {
                ((PlayerListEntryAccessor) entry).setLatency(parsed);

                MutableText rebuilt = Text.empty();
                for (Text part : display.getWithStyle(Style.EMPTY)) {
                    String replaced = part.getString().replaceAll("(\\d{1,4})\\s*ms", "").trim();
                    if (!replaced.isEmpty()) rebuilt.append(Text.literal(replaced).setStyle(part.getStyle()));
                }
                entry.setDisplayName(rebuilt);
            }
        }
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void club$fixTabPings(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() == null) return;
        String address = client.getCurrentServerEntry().address;
        if (address == null || !address.contains("mcpvp.club")) return;

        int latency = entry.getLatency();
        if (latency < 0 || latency == 1 || latency >= 1000) {
            ci.cancel();
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("betterpingdisplay")) {
            ci.cancel();
            String pingText = latency + "ms";
            int textWidth = client.textRenderer.getWidth(pingText);

            context.drawTextWithShadow(client.textRenderer, pingText, (x + width - textWidth - 1), y, PingUtil.getPingColor(latency));
        }
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "CONSTANT", args = "intValue=13")
    )
    private int club$widenForPingText(int original) {
        if (FabricLoader.getInstance().isModLoaded("betterpingdisplay")) return original;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() == null) return original;
        String address = client.getCurrentServerEntry().address;
        if (address == null || !address.contains("mcpvp.club")) return original;

        return client.textRenderer.getWidth("xxxxms"); //if they're on 10k+ ping oh well
    }
}
