package hero.bane.clubtimizer.mixin;

import com.mojang.authlib.GameProfile;
import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.gui.hud.spectator.SpectatorMenu;
import net.minecraft.client.gui.hud.spectator.TeleportToSpecificPlayerSpectatorCommand;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TeleportToSpecificPlayerSpectatorCommand.class)
public abstract class TeleportToSpecificPlayerSpectatorCommandMixin {

    @Shadow @Final private GameProfile gameProfile;

    @Unique
    private static long lastTeleportTime = 0L;

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void club$getFormattedSecondName(CallbackInfoReturnable<Text> cir) {
        if (!MCPVPStateChanger.inSpec()) return;
        if (Clubtimizer.client == null || Clubtimizer.client.getNetworkHandler() == null) return;

        PlayerListEntry entry =
                Clubtimizer.client.getNetworkHandler().getPlayerListEntry(gameProfile.getId());
        if (entry == null || entry.getDisplayName() == null) return;

        Text disp = entry.getDisplayName();

        String stripped = TextUtil.stripFormatting(TextUtil.toLegacyString(disp)).trim();

        String[] parts = stripped.split(" ");

        if (parts.length >= 2) {

            cir.setReturnValue(Text.literal(parts[1]));
            cir.cancel();
            return;
        }

        cir.setReturnValue(disp);
        cir.cancel();
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void club$overrideTeleport(SpectatorMenu menu, CallbackInfo ci) {
        if (Clubtimizer.player == null || !Clubtimizer.player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;

        long now = System.currentTimeMillis();
        if (now - lastTeleportTime < 1000L) {
            ChatUtil.say(TextUtil.rainbowGradient("Spectator Teleport on Cooldown"), false);
            ci.cancel();
            return;
        }
        lastTeleportTime = now;

        if (Clubtimizer.client == null || Clubtimizer.client.getNetworkHandler() == null) return;

        PlayerListEntry entry =
                Clubtimizer.client.getNetworkHandler().getPlayerListEntry(gameProfile.getId());
        if (entry == null || entry.getDisplayName() == null) return;

        String stripped = TextUtil.stripFormatting(TextUtil.toLegacyString(entry.getDisplayName())).trim();
        String[] parts = stripped.split(" ");

        if (parts.length < 2) return;
        String cleanUsername = parts[1];

        ChatUtil.chat("/tp " + cleanUsername);

        ci.cancel();
    }
}