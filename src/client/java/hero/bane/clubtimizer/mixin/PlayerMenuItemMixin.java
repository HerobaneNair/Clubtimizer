package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.ChatUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.gui.spectator.PlayerMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerMenuItem.class)
public abstract class PlayerMenuItemMixin {

    @Shadow @Final
    private PlayerInfo playerInfo;

    @Unique
    private static long club$lastTeleportTime = 0L;

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void club$overrideName(CallbackInfoReturnable<Component> cir) {
        if (!MCPVPStateChanger.inSpec()) return;

        Component display = playerInfo.getTabListDisplayName();
        if (display == null) return;

        String stripped = TextUtil
                .stripFormatting(TextUtil.toLegacyString(display))
                .trim();

        String[] parts = stripped.split(" ");
        if (parts.length >= 2) {
            cir.setReturnValue(Component.literal(parts[1]));
            cir.cancel();
        }
    }

    @Inject(method = "selectItem", at = @At("HEAD"), cancellable = true)
    private void club$overrideTeleport(SpectatorMenu menu, CallbackInfo ci) {
        if (Clubtimizer.player == null || !Clubtimizer.player.isSpectator()) return;
        if (!MCPVPStateChanger.inSpec()) return;

        long now = System.currentTimeMillis();
        if (now - club$lastTeleportTime < 1000L) {
            ChatUtil.say(
                    TextUtil.rainbowGradient("Spectator Teleport on Cooldown"),
                    false
            );
            ci.cancel();
            return;
        }
        club$lastTeleportTime = now;

        Component display = playerInfo.getTabListDisplayName();
        if (display == null) return;

        String stripped = TextUtil
                .stripFormatting(TextUtil.toLegacyString(display))
                .trim();

        String[] parts = stripped.split(" ");
        if (parts.length < 2) return;

        String cleanUsername = parts[1];
        ChatUtil.chat("/tp " + cleanUsername);

        ci.cancel(); // block ServerboundTeleportToEntityPacket
    }
}
