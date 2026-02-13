package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PingUtil;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    private Component tabListDisplayName;

    @Inject(method = "getLatency", at = @At("HEAD"), cancellable = true)
    private void club$getPingFromTab(CallbackInfoReturnable<Integer> cir) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;

        if (this.tabListDisplayName == null) return;

        int parsed = PingUtil.parseTablistPing(this.tabListDisplayName.getString());
        if (parsed >= 0) {
            cir.setReturnValue(parsed);
        }
    }
}
